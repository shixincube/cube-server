/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.hub.HubAction;
import cube.hub.event.Event;
import cube.hub.signal.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信令控制中心。
 */
public class SignalController {

    private ConcurrentHashMap<Long, TalkContext> pretenderIdMap;

    private HubCellet cellet;

    private ConcurrentHashMap<Long, Signal> blockingSignalMap;

    public SignalController(HubCellet cellet) {
        this.cellet = cellet;
        this.pretenderIdMap = new ConcurrentHashMap<>();
        this.blockingSignalMap = new ConcurrentHashMap<>();
    }

    /**
     * 关闭。
     */
    public void dispose() {
        for (Signal signal : this.blockingSignalMap.values()) {
            synchronized (signal) {
                signal.notify();
            }
        }
        this.blockingSignalMap.clear();
    }

    /**
     * 删除客户端。
     * @param talkContext
     */
    public void removeClient(TalkContext talkContext) {
        Iterator<Map.Entry<Long, TalkContext>> iter = this.pretenderIdMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, TalkContext> entry = iter.next();
            if (entry.getValue() == talkContext) {
                iter.remove();
                break;
            }
        }
    }

    public boolean transmit(Long pretenderId, Signal signal) {
        TalkContext talkContext = this.pretenderIdMap.get(pretenderId);
        if (null == talkContext) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(HubAction.TransmitSignal.name);
        actionDialect.addParam("signal", signal.toJSON());

        this.cellet.speak(talkContext, actionDialect);
        return true;
    }

    public Event transmitSyncEvent(Long pretenderId, Signal signal) {
        TalkContext talkContext = this.pretenderIdMap.get(pretenderId);
        if (null == talkContext) {
            return null;
        }

        this.blockingSignalMap.put(signal.getSerialNumber(), signal);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#transmitSyncEvent - Signal " + signal.getName()
                    + " (" + signal.getSerialNumber() + ")");
        }

        ActionDialect actionDialect = new ActionDialect(HubAction.TransmitSignal.name);
        actionDialect.addParam("signal", signal.toJSON());

        this.cellet.speak(talkContext, actionDialect);

        synchronized (signal) {
            try {
                signal.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.blockingSignalMap.remove(signal.getSerialNumber());

        return signal.event;
    }

    public boolean capture(Event event) {
        Signal signal = this.blockingSignalMap.remove(event.getSerialNumber());
        if (null == signal) {
            return false;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#capture - Event " + event.getName()
                    + " (" + event.getSerialNumber() + ")");
        }

        signal.event = event;

        synchronized (signal) {
            signal.notify();
        }

        return true;
    }

    /**
     * 接收来自客户端的信令。
     *
     * @param signal
     * @return
     */
    public Signal receive(Signal signal) {
        if (PassBySignal.NAME.equals(signal.getName())) {
            this.passBy((PassBySignal) signal);
        }
        else if (ReadySignal.NAME.equals(signal.getName())) {
            ReadySignal readySignal = (ReadySignal) signal;
            this.pretenderIdMap.put(readySignal.getDescription().getPretender().getId(),
                    readySignal.talkContext);

            (new Thread() {
                @Override
                public void run() {
                    // 通知客户端提交状态报告
                    transmit(readySignal.getDescription().getPretender().getId(), new ReportSignal());
                }
            }).start();
        }
        else if (QueryChannelCodeSignal.NAME.equals(signal.getName())) {
            QueryChannelCodeSignal querySignal = (QueryChannelCodeSignal) signal;
            String code = this.cellet.getService().getChannelManager()
                                    .getChannelCodeWithAccountId(querySignal.getAccountId());
            if (null != code) {
                QueryChannelCodeSignal ack = new QueryChannelCodeSignal(querySignal.getAccountId());
                ack.setCode(code);
                return ack;
            }
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

                TalkContext talkContext = entry.getValue();
                if (!talkContext.isValid()) {
                    iter.remove();
                    continue;
                }

                List<Signal> signalList = passBySignal.getSignals();
                for (Signal signal : signalList) {
                    ActionDialect actionDialect = new ActionDialect(HubAction.TransmitSignal.name);
                    actionDialect.addParam("signal", signal.toJSON());

                    this.cellet.speak(talkContext, actionDialect);
                }
            }
        }
        else {
            List<Long> destinations = passBySignal.getDestinations();
            if (null == destinations) {
                return;
            }

            for (Long id : destinations) {
                List<Signal> signalList = passBySignal.getSignals();
                for (Signal signal : signalList) {
                    boolean result = this.transmit(id, signal);
                    if (!result) {
                        Logger.i(this.getClass(), "Pass-by to " + id + " failed");
                        break;
                    }
                }
            }
        }
    }
}
