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
import cube.ferry.Ticket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理器描述。
 */
public class Ferryboat {

    private final static Ferryboat instance = new Ferryboat();

    private Nucleus nucleus;

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
        String address = "127.0.0.1";
        int port = 6000;
        nucleus.getTalkService().call(address, port);
    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");
        Ticket ticket = new Ticket(domain, talkContext);
        this.ticketMap.put(domain, ticket);
    }

    public void checkOut(ActionDialect dialect, TalkContext talkContext) {

    }

    public void passBy(ActionDialect actionDialect) {

    }
}
