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
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.ferry.FerryAdapter;
import cube.ferry.Ticket;
import cube.plugin.PluginSystem;
import cube.service.ferry.plugin.WriteMessagePlugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 摆渡数据服务。
 */
public class FerryService extends AbstractModule {

    public final static String NAME = "Ferry";

    private Timer timer;

    private FerryAdapter adapter;

    private List<Ticket> tickets;

    public FerryService() {
        super();
        this.tickets = new LinkedList<>();
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

    public void checkIn(String domainName, TalkContext talkContext) {

    }

    public void checkOut() {

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
