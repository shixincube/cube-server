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
import org.kurento.client.Composite;
import org.kurento.client.Continuation;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 被管理的每一个媒体管道上的数据封装。
 */
public class KurentoMediaPipelineWrapper {

    protected final Long commFieldId;

    protected final MediaPipeline pipeline;

    /**
     * Comm Field Endpoint 对应的 Session
     */
    private final ConcurrentMap<Long, KurentoSession> endpointSessionMap;

    protected Composite composite;

    protected HubPort compositeOutputHubPort;

    protected long timestamp;

    protected KurentoMediaPipelineWrapper(Long commFieldId, MediaPipeline pipeline) {
        this.timestamp = System.currentTimeMillis();
        this.commFieldId = commFieldId;
        this.pipeline = pipeline;
        this.endpointSessionMap = new ConcurrentHashMap<>();
    }

    public KurentoSession getSession(Long id) {
        this.timestamp = System.currentTimeMillis();
        return this.endpointSessionMap.get(id);
    }

    public void addSession(Long id, KurentoSession session) {
        this.endpointSessionMap.put(id, session);
        this.timestamp = System.currentTimeMillis();
    }

    public KurentoSession removeSession(Long id) {
        this.timestamp = System.currentTimeMillis();
        return this.endpointSessionMap.remove(id);
    }

    public Collection<KurentoSession> getSessions() {
        return this.endpointSessionMap.values();
    }

    protected void closePipeline() {
        this.pipeline.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                Logger.d(KurentoForwardingMediaUnit.class, "Released Pipeline : " + commFieldId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                Logger.d(KurentoForwardingMediaUnit.class, "Could not release Pipeline : " + commFieldId);
            }
        });
    }
}
