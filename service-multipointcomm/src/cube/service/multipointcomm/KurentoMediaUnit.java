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
import cube.service.multipointcomm.signaling.Signaling;
import org.kurento.client.Continuation;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurento 单元。
 */
public final class KurentoMediaUnit extends AbstractMediaUnit {

    private final long timeout = 60L * 1000L;

    private final Portal portal;

    private final String url;

    private KurentoClient kurentoClient;

    /**
     * Comm Field 对应的 Media Pipeline 。
     */
    private final ConcurrentHashMap<Long, MediaPipelineWrapper> pipelineMap;

    public KurentoMediaUnit(Portal portal, String url) {
        this.portal = portal;
        this.url = url;

        try {
            this.kurentoClient = KurentoClient.create(url);
        } catch (Throwable e) {
            Logger.e(this.getClass(), "", e);
        }

        this.pipelineMap = new ConcurrentHashMap<>();
    }

    @Override
    public void preparePipeline(CommField commField, CommFieldEndpoint endpoint) {
        if (null == this.kurentoClient) {
            return;
        }

        Logger.i(this.getClass(), "Prepare media pipeline: \"" + commField.getName() + "\" - " + commField.getId());

        MediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            wrapper = new MediaPipelineWrapper(commField.getId(), this.kurentoClient.createMediaPipeline());
            this.pipelineMap.put(commField.getId(), wrapper);
        }

        KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            session = new KurentoSession(this.portal, commField, endpoint, wrapper.pipeline);
            wrapper.addSession(endpoint.getId(), session);
        }
    }

    @Override
    public MultipointCommStateCode receiveFrom(CommField commField, CommFieldEndpoint endpoint, Signaling signaling) {
        MediaPipelineWrapper wrapper = this.pipelineMap.get(commField.getId());
        if (null == wrapper) {
            return MultipointCommStateCode.NoPipeline;
        }

        KurentoSession session = wrapper.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        CommFieldEndpoint target = signaling.getTarget();
        KurentoSession sender = wrapper.getSession(target.getId());

        // 接收数据
        session.receiveFrom(sender);

        return MultipointCommStateCode.Ok;

//        WebRtcEndpoint rtcEndpoint = this.commEndpointMap.get(endpoint.getId());
//        if (null == rtcEndpoint) {
//            rtcEndpoint = new WebRtcEndpoint.Builder(wrapper.pipeline).build();
//            rtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
//                @Override
//                public void onEvent(IceCandidateFoundEvent event) {
//
//                }
//            });
//
//            this.commEndpointMap.put(endpoint.getId(), rtcEndpoint);
//        }
    }

    @Override
    public void destroy() {
        for (MediaPipelineWrapper wrapper : this.pipelineMap.values()) {
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
        Iterator<MediaPipelineWrapper> iter = this.pipelineMap.values().iterator();
        while (iter.hasNext()) {
            MediaPipelineWrapper wrapper = iter.next();
            if (now - wrapper.timestamp > this.timeout) {
                Logger.w(this.getClass(), "Media pipeline timeout: " + wrapper.id);
                wrapper.closePipeline();
                iter.remove();
            }
        }
    }

    /**
     * 可以视为 ROOM 结构。
     */
    protected class MediaPipelineWrapper {

        protected final Long id;

        protected final MediaPipeline pipeline;

        /**
         * Comm Field Endpoint 对应的 Session
         */
        private final ConcurrentHashMap<Long, KurentoSession> endpointSessionMap;

        protected long timestamp;

        protected MediaPipelineWrapper(Long id, MediaPipeline pipeline) {
            this.timestamp = System.currentTimeMillis();
            this.id = id;
            this.pipeline = pipeline;
            this.endpointSessionMap = new ConcurrentHashMap<>();
        }

        public KurentoSession getSession(Long id) {
            return this.endpointSessionMap.get(id);
        }

        public void addSession(Long id, KurentoSession session) {
            this.endpointSessionMap.put(id, session);
        }

        protected void closePipeline() {
            pipeline.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    Logger.d(KurentoMediaUnit.class, "Released Pipeline : " + id);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    Logger.d(KurentoMediaUnit.class, "Could not release Pipeline : " + id);
                }
            });
        }
    }
}