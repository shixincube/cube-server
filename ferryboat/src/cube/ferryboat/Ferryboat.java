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

import cell.api.Nucleus;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.ferry.FerryPacket;
import cube.ferry.Ticket;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理器描述。
 */
public class Ferryboat {

    public final static String NAME = "Ferry";

    private final static Ferryboat instance = new Ferryboat();

    private Nucleus nucleus;

    private String address;

    private int port;

    private FerryReceiver receiver;

    private Map<String, Ticket> ticketMap;

    private Ferryboat() {
        this.ticketMap = new ConcurrentHashMap<>();
        this.receiver = new FerryReceiver(this.ticketMap);
    }

    public static Ferryboat getInstance() {
        return Ferryboat.instance;
    }

    public void config(Nucleus nucleus) {
        this.nucleus = nucleus;
        nucleus.getTalkService().addListener(this.receiver);

        // 连接服务器
        this.address = "127.0.0.1";
        this.port = 6000;
        nucleus.getTalkService().call(this.address, this.port);
    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        Logger.i(this.getClass(), "#checkIn - " + talkContext.getSessionHost());

        String domain = dialect.getParamAsString("domain");
        Ticket ticket = new Ticket(domain, talkContext);
        this.ticketMap.put(domain, ticket);
    }

    public void checkOut(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");
        this.ticketMap.remove(domain);
    }

    public String checkOut(TalkContext talkContext) {
        String domain = null;

        Iterator<Map.Entry<String, Ticket>> iterator = this.ticketMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Ticket> e = iterator.next();
            Ticket ticket = e.getValue();
            if (ticket.sessionId == talkContext.getSessionId().longValue()) {
                domain = e.getKey();
                iterator.remove();
                break;
            }
        }

        return domain;
    }

    public void passBy(ActionDialect actionDialect) {
        if (this.nucleus.getTalkService().isCalled(this.address, this.port)) {
            FerryPacket packet = new FerryPacket(actionDialect);
            this.nucleus.getTalkService().speak(NAME, packet.toDialect());
        }
        else {
            Logger.w(this.getClass(), "#passBy - connection is error: "
                    + this.address + ":" + this.port);
        }
    }
}
