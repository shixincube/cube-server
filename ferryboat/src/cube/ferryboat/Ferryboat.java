/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryboat;

import cell.api.Nucleus;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.ferry.FerryPacket;
import cube.ferry.Ticket;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 管理器描述。
 */
public class Ferryboat {

    public final static String NAME = "Ferry";

    private final static Ferryboat instance = new Ferryboat();

    private Nucleus nucleus;

    private String address;

    private int port;

    private ExecutorService executor;

    private FerryboatCellet cellet;

    private FerryReceiver receiver;

    private Map<String, Ticket> ticketMap;

    private Ferryboat() {
        this.ticketMap = new ConcurrentHashMap<>();
    }

    public static Ferryboat getInstance() {
        return Ferryboat.instance;
    }

    public void start(Nucleus nucleus) {
        this.nucleus = nucleus;

        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);

        this.receiver = new FerryReceiver(this.executor, this.ticketMap);

        nucleus.getTalkService().addListener(this.receiver);

        // 连接服务器
        this.address = "127.0.0.1";
        this.port = 6000;
        nucleus.getTalkService().call(this.address, this.port);
    }

    public void stop() {
        this.nucleus.getTalkService().removeListener(this.receiver);

        if (null != this.executor) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    public void setCellet(FerryboatCellet cellet) {
        this.cellet = cellet;
    }

    protected FerryboatCellet getCellet() {
        return this.cellet;
    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");

        Logger.i(this.getClass(), "#checkIn - " + domain + " - " + talkContext.getSessionHost());

        Ticket ticket = new Ticket(domain, dialect.getParamAsJson("licence"), talkContext);
        this.ticketMap.put(domain, ticket);

        // 写入地址
        dialect.addParam("address", talkContext.getSessionHost());
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
