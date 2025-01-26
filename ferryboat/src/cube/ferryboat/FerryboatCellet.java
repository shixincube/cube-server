/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryboat;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.ferry.FerryAction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据摆渡单元。
 */
public class FerryboatCellet extends AbstractCellet {

    private ExecutorService executor;

    public FerryboatCellet() {
        super(Ferryboat.NAME);
    }

    @Override
    public boolean install() {
        this.executor = Executors.newCachedThreadPool();

        Ferryboat.getInstance().setCellet(this);

        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FerryAction.CheckIn.name.equals(action)) {
            Ferryboat.getInstance().checkIn(dialect, talkContext);
        }
        else if (FerryAction.CheckOut.name.equals(action)) {
            Ferryboat.getInstance().checkOut(dialect, talkContext);
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                Ferryboat.getInstance().passBy(dialect);

                if (action.equals(FerryAction.PingAck.name)) {
                    Logger.d(FerryboatCellet.class, "Pass-by ping-ack: " + dialect.getParamAsInt("sn"));
                }
            }
        });
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable server) {
        super.onQuitted(talkContext, server);

        String domain = Ferryboat.getInstance().checkOut(talkContext);
        if (null != domain) {
            ActionDialect dialect = new ActionDialect(FerryAction.CheckOut.name);
            dialect.addParam("domain", domain);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    Ferryboat.getInstance().passBy(dialect);
                }
            });
        }
    }
}
