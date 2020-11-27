/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.api.Speakable;
import cube.common.Packet;
import cube.service.multipointcomm.signaling.Signaling;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体单元描述。
 */
public class MediaUnit {

    protected final static String CELLET_NAME = "MediaUnit";

    protected String address;

    protected int port;

    protected Speakable speaker;

    private ConcurrentHashMap<Long, SignalingBundle> sentSignalings;

    public MediaUnit(String address, int port) {
        this.address = address;
        this.port = port;
        this.sentSignalings = new ConcurrentHashMap<>();
    }

    public boolean transmit(Packet packet, Signaling signaling, SignalingCallback signalingCallback) {
        if (this.speaker.speak(CELLET_NAME, packet.toDialect())) {
            SignalingBundle bundle = new SignalingBundle(signaling, signalingCallback);
            this.sentSignalings.put(packet.sn, bundle);
            return true;
        }

        return false;
    }

    public void receive(Packet packet) {
        SignalingBundle bundle = this.sentSignalings.remove(packet.sn);
        if (null != bundle) {
            // 处理信令
//            bundle.callback.on();
        }
        else {
            // 接收
        }
    }

    /**
     * 信令束。
     */
    protected class SignalingBundle {

        protected Signaling signaling;

        protected SignalingCallback signalingCallback;

        protected SignalingBundle(Signaling signaling, SignalingCallback signalingCallback) {
            this.signaling = signaling;
            this.signalingCallback = signalingCallback;
        }
    }
}
