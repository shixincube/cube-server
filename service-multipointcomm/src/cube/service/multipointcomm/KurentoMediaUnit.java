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
import org.kurento.client.Continuation;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurento 单元。
 */
public final class KurentoMediaUnit extends AbstractMediaUnit {

    private long timeout = 60L * 1000L;

    private String url;

    private KurentoClient kurentoClient;

    private ConcurrentHashMap<Long, MediaPipelineWrapper> pipelineMap;

    public KurentoMediaUnit(String url) {
        this.url = url;

        try {
            this.kurentoClient = KurentoClient.create(url);
        } catch (Throwable e) {
            Logger.e(this.getClass(), "", e);
        }

        this.pipelineMap = new ConcurrentHashMap<>();
    }

    @Override
    public void preparePipeline(CommField commField) {
        this.pipelineMap.put(commField.getId(),
                new MediaPipelineWrapper(commField.getId(), this.kurentoClient.createMediaPipeline()));
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
        }
    }

    @Override
    public void onTick(long now) {
        // 删除超时的管道
        Iterator<MediaPipelineWrapper> iter = this.pipelineMap.values().iterator();
        while (iter.hasNext()) {
            MediaPipelineWrapper wrapper = iter.next();
            if (now - wrapper.timestamp > this.timeout) {
                wrapper.closePipeline();
                iter.remove();
            }
        }
    }

    protected class MediaPipelineWrapper {

        protected Long id;

        protected MediaPipeline pipeline;

        protected long timestamp;

        protected MediaPipelineWrapper(Long id, MediaPipeline pipeline) {
            this.id = id;
            this.pipeline = pipeline;
            this.timestamp = System.currentTimeMillis();
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
