/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.ferry.FerryAction;
import cube.ferry.Ticket;

import javax.websocket.OnOpen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 连接后端服务的监听器。
 */
public class FerryReceiver implements TalkListener {

    private ExecutorService executor;

    private Map<String, Ticket> ticketMap;

    /**
     * 文件标签对应的域映射。
     * Key: 文件标签的文件码，即数据流名称。
     * Value: 对应的域名称。
     */
    private Map<String, String> fileCodeMarkMap;

    public FerryReceiver(ExecutorService executor, Map<String, Ticket> ticketMap) {
        this.executor = executor;
        this.ticketMap = ticketMap;
        this.fileCodeMarkMap = new ConcurrentHashMap<>();
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

    private void processInputStream(PrimitiveInputStream primitiveInputStream) {
        this.executor.execute(() -> {
            String domain = fileCodeMarkMap.remove(primitiveInputStream.getName());
            if (null == domain) {
                Logger.w(FerryReceiver.class, "#processInputStream - Can NOT find domain: " + primitiveInputStream.getName());
                try {
                    primitiveInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            Ticket ticket = ticketMap.get(domain);
            if (null == ticket) {
                Logger.w(this.getClass(), "#processInputStream - Can NOT find domain talk context: " + domain);
                try {
                    primitiveInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            byte[] bytes = new byte[256];
            int length = 0;
            // 发送流
            PrimitiveOutputStream pos = Ferryboat.getInstance().getCellet().speakStream(ticket.talkContext,
                    primitiveInputStream.getName());
            try {
                while ((length = primitiveInputStream.read(bytes)) > 0) {
                    pos.write(bytes, 0, length);
                }
                pos.flush();
            } catch (IOException e) {
                Logger.w(FerryReceiver.this.getClass(), "#processInputStream", e);
            } finally {
                if (null != pos) {
                    try {
                        pos.close();
                    } catch (IOException e) {
                    }
                }

                try {
                    primitiveInputStream.close();
                } catch (IOException e) {
                }
            }
        });
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
        else if (FerryAction.MarkFile.name.equals(action)) {
            this.fileCodeMarkMap.put(actionDialect.getParamAsString("code"),
                    actionDialect.getParamAsString("domain"));
        }
        else if (FerryAction.Report.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ferry(actionDialect);
                }
            });
        }
        else if (FerryAction.Synchronize.name.equals(action)) {
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
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#onListened(PrimitiveInputStream)");
        }

        this.processInputStream(primitiveInputStream);
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
                // 注册已经 Check-in 的 House
                for (Ticket ticket : ticketMap.values()) {
                    ActionDialect dialect = new ActionDialect(FerryAction.CheckIn.name);
                    dialect.addParam("domain", ticket.domain.getName());
                    dialect.addParam("licence", ticket.licence);
                    dialect.addParam("address", ticket.talkContext.getSessionHost());
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
