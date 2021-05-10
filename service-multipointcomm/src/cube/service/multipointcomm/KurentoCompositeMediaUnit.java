/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.multipointcomm;

import cell.util.log.Logger;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;
import org.kurento.client.Composite;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * Kurento 单元。
 */
public class KurentoCompositeMediaUnit extends AbstractCompositeMediaUnit {

    private final MultipointCommService service;

    private final Portal portal;

    private final String url;

    private final ExecutorService executor;

    private KurentoClient kurentoClient;

    /**
     * Comm Field 对应的 Media Pipeline 。
     */
    private final ConcurrentMap<Long, KurentoMediaPipelineWrapper> pipelineMap;

    public KurentoCompositeMediaUnit(MultipointCommService service, Portal portal, String url, ExecutorService executor) {
        this.service = service;
        this.portal = portal;
        this.url = url;
        this.executor = executor;

        try {
            this.kurentoClient = KurentoClient.create(url);
        } catch (Throwable e) {
            Logger.e(this.getClass(), "", e);
        }

        this.pipelineMap = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void preparePipeline(CommField commField, CommFieldEndpoint endpoint) {
        if (null == this.kurentoClient) {
            return;
        }

        Logger.i(this.getClass(), "Prepare media pipeline: \"" + commField.getName() + "\" - " + commField.getId());

        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            wrapper = new KurentoMediaPipelineWrapper(commField.getId(), this.kurentoClient.createMediaPipeline());

            // 创建混合集线器
            wrapper.composite = new Composite.Builder(wrapper.pipeline).build();

            this.pipelineMap.put(commField.getId(), wrapper);
        }

        KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            session = new KurentoSession(this.portal, commField.getId(), endpoint, wrapper.pipeline);
            wrapper.addSession(endpoint.getId(), session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode subscribe(CommField commField, CommFieldEndpoint endpoint,
                                             CommFieldEndpoint target, String offerSDP,
                                             MediaUnitCallback callback) {
        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        final KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        final KurentoSession sender = wrapper.getSession(target.getId());
        if (null == sender) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        // 接收数据
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                session.receiveFrom(sender, offerSDP);
                callback.on(commField, endpoint);
            }
        });

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode unsubscribe(CommField commField, CommFieldEndpoint endpoint, CommFieldEndpoint target,
                                               MediaUnitCallback callback) {
        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        final KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        final KurentoSession sender = wrapper.getSession(target.getId());
        if (null == sender) {
            // 没有找到目标会话
            return MultipointCommStateCode.Ok;
        }

        // 取消接收数据
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                session.cancelFrom(sender);
                callback.on(commField, endpoint);
            }
        });

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode addCandidate(CommField commField, CommFieldEndpoint endpoint,
                                                CommFieldEndpoint target, JSONObject candidateJson) {
        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        if (null == target) {
            return MultipointCommStateCode.DataStructureError;
        }

        KurentoSession targetSession = wrapper.getSession(target.getId());
        if (null == targetSession) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        // 添加 Candidate
        IceCandidate candidate = new IceCandidate(candidateJson.getString("candidate"),
                candidateJson.getString("sdpMid"), candidateJson.getInt("sdpMLineIndex"));
        session.addCandidate(candidate, targetSession);

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint) {
        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        // 删除会话
        KurentoSession session = wrapper.removeSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        for (CommFieldEndpoint participant : commField.getEndpoints()) {
            if (participant.equals(endpoint)) {
                continue;
            }

            // 让其他参与者取消接收流
            KurentoSession participantSession = wrapper.getSession(participant.getId());
            participantSession.cancelFrom(session);
        }

        try {
            session.close();
        } catch (IOException e) {
            Logger.e(this.getClass(), "", e);
        }

        // 删除终端
        commField.removeEndpoint(endpoint);

        if (commField.numEndpoints() == 0) {
            Logger.d(this.getClass(), "Comm field \"" + commField.getName() + "\" empty.");
            // 已经没有终端在场域里，删除场域
            this.pipelineMap.remove(commField.getId());
            wrapper.closePipeline();
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode release(CommField commField) {
        KurentoMediaPipelineWrapper wrapper = this.pipelineMap.remove(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        // 关闭所有会话
        for (KurentoSession session : wrapper.getSessions()) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 关闭通道
        wrapper.closePipeline();

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        for (KurentoMediaPipelineWrapper wrapper : this.pipelineMap.values()) {
            wrapper.closePipeline();
        }
        this.pipelineMap.clear();

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (null != this.kurentoClient) {
            this.kurentoClient.destroy();
            this.kurentoClient = null;
        }
    }

    @Override
    public void onTick(long now) {
        if (null == this.kurentoClient) {
            try {
                this.kurentoClient = KurentoClient.create(this.url);
            } catch (Throwable e) {
                Logger.w(this.getClass(), "", e);
            }
        }

        // 删除超时的管道
        Iterator<KurentoMediaPipelineWrapper> iter = this.pipelineMap.values().iterator();
        while (iter.hasNext()) {
            KurentoMediaPipelineWrapper wrapper = iter.next();
            CommField commField = this.service.getCommField(wrapper.commFieldId);
            if (null != commField) {
                if (commField.numEndpoints() == 0) {
//                    Logger.d(this.getClass(), "CommField \"" + commField.getName() + "\" : no endpoint");
                    Logger.i(this.getClass(), "Media pipeline closed : " + commField.getName());
                    wrapper.closePipeline();
                    iter.remove();
                }
                else {
                    Logger.d(this.getClass(), "CommField \"" + commField.getName() + "\" : " + commField.numEndpoints() + " endpoints");
                }
            }
            else {
//                Logger.e(this.getClass(), "CommField : " + wrapper.commFieldId + " removed");
                Logger.w(this.getClass(), "Media pipeline closed : " + wrapper.commFieldId);
                wrapper.closePipeline();
                iter.remove();
            }
        }
    }
}
