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

package cube.service.ferry;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.ferry.FerryAdapter;
import cube.ferry.FerryPacket;
import cube.ferry.Ticket;
import cube.plugin.PluginSystem;
import cube.service.ferry.plugin.WriteMessagePlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 摆渡数据服务。
 */
public class FerryService extends AbstractModule {

    public final static String NAME = "Ferry";

    private final FerryCellet cellet;

    private Timer timer;

    private FerryAdapter adapter;

    private Map<String, Ticket> tickets;

    private Queue<FerryPacket> pushQueue;

    private Object pushMutex = new Object();
    private boolean pushing = false;

    public FerryService(FerryCellet cellet) {
        super();
        this.cellet = cellet;
        this.tickets = new ConcurrentHashMap<>();
        this.pushQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void start() {
        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // 启动适配器


                    // 配置
                    setup();

                    (new Thread() {
                        @Override
                        public void run() {
                            timer.cancel();
                            timer = null;
                        }
                    }).start();
                }
            }, 10 * 1000);
        }
    }

    @Override
    public void stop() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        this.teardown();

        if (null != this.adapter) {
            this.adapter.stop();
            this.adapter = null;
        }

        this.pushQueue.clear();
        this.tickets.clear();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public FerryAdapter getAdapter() {
        return this.adapter;
    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");
        Ticket ticket = new Ticket(domain, talkContext);
        this.tickets.put(domain, ticket);
    }

    public void checkOut(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");
        this.tickets.remove(domain);
    }

    public boolean hasDomain(String domain) {
        return this.tickets.containsKey(domain);
    }

    public void pushToBoat(String domain, FerryPacket packet) {
        if (!this.tickets.containsKey(domain)) {
            Logger.w(this.getClass(), "#pushToBoat - Can NOT find domain talk context: " + domain);
            return;
        }

        // 设置 Domain
        packet.setDomain(domain);

        this.pushQueue.offer(packet);

        synchronized (this.pushMutex) {
            if (this.pushing) {
                return;
            }

            this.pushing = true;
        }

        (new Thread() {
            @Override
            public void run() {
                FerryPacket ferryPacket = pushQueue.poll();
                while (null != ferryPacket) {
                    Ticket ticket = tickets.get(ferryPacket.getDomain());
                    if (null != ticket) {
                        // 向 Ferry 推送数据
                        cellet.speak(ticket.talkContext, ferryPacket.toDialect());
                    }

                    ferryPacket = pushQueue.poll();
                }

                synchronized (pushMutex) {
                    pushing = false;
                }
            }
        }).start();
    }

    private void setup() {
        AbstractModule messagingModule = this.getKernel().getModule("Messaging");
        if (null != messagingModule) {
            messagingModule.getPluginSystem().register("WriteMessage", new WriteMessagePlugin(this));
        }
    }

    private void teardown() {
    }
}
