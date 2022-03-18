/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.hub;

import cell.core.talk.TalkContext;
import cube.hub.signal.AckSignal;
import cube.hub.signal.PassBySignal;
import cube.hub.signal.ReadySignal;
import cube.hub.signal.Signal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信令控制中心。
 */
public class SignalController {

    private final static SignalController instance = new SignalController();

    private ConcurrentHashMap<Long, TalkContext> pretenderIdMap;

    private HubCellet cellet;

    private SignalController() {
        this.pretenderIdMap = new ConcurrentHashMap<>();
    }

    public final static SignalController getInstance() {
        return SignalController.instance;
    }

    public void setCellet(HubCellet cellet) {
        this.cellet = cellet;
    }

    public boolean send(Signal signal) {
        return true;
    }

    public Signal receive(Signal signal) {
        if (PassBySignal.NAME.equals(signal.getName())) {
            this.passBy((PassBySignal) signal);
        }
        else if (ReadySignal.NAME.equals(signal.getName())) {
            ReadySignal readySignal = (ReadySignal) signal;
            this.pretenderIdMap.put(readySignal.getDescription().getPretender().getId(),
                    readySignal.talkContext);
        }

        return new AckSignal();
    }

    private void passBy(PassBySignal passBySignal) {
        if (passBySignal.isBroadcast()) {
            Long skipId = passBySignal.getDescription().getPretender().getId();

            Iterator<Map.Entry<Long, TalkContext>> iter = this.pretenderIdMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Long, TalkContext> entry = iter.next();
                Long id = entry.getKey();
                if (skipId.equals(id)) {
                    continue;
                }

                List<Signal> signalList = passBySignal.getSignals();
            }
        }
    }

    private void transmit(Long pretenderId) {

    }
}
