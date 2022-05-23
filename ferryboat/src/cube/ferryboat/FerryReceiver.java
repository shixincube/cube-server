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

package cube.ferryboat;

import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.ferry.FerryAction;
import cube.ferry.Ticket;

import javax.websocket.OnOpen;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 连接后端服务的监听器。
 */
public class FerryReceiver implements TalkListener {

    private ExecutorService executor;

    private Map<String, Ticket> ticketMap;

    public FerryReceiver(ExecutorService executor, Map<String, Ticket> ticketMap) {
        this.executor = executor;
        this.ticketMap = ticketMap;
    }

    private void ferry(ActionDialect actionDialect) {
        String domain = actionDialect.getParamAsString("domain");
        Ticket ticket = this.ticketMap.get(domain);
        if (null == ticket) {
            Logger.w(this.getClass(), "#ferry - Can NOT find domain talk context: " + domain);
            return;
        }

        Ferryboat.getInstance().getCellet().speak(ticket.talkContext, actionDialect);
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        final ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        final String action = actionDialect.getName();

        if (FerryAction.Ferry.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ferry(actionDialect);
                }
            });
        }
        else if (FerryAction.Ping.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ferry(actionDialect);
                }
            });
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {
        Logger.d(this.getClass(), "#onListened(PrimitiveInputStream)");
    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.d(this.getClass(), "#onContacted");

        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 注册已经 Check-in 的 House
                for (Ticket ticket : ticketMap.values()) {
                    ActionDialect dialect = new ActionDialect(FerryAction.CheckIn.name);
                    dialect.addParam("domain", ticket.domain.getName());
                    Ferryboat.getInstance().passBy(dialect);
                }
            }
        }).start();
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.d(this.getClass(), "#onQuitted");
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        Logger.d(this.getClass(), "#onFailed");
    }
}
