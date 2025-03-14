/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.ferry.FerryAction;
import cube.service.ferry.task.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class FerryCellet extends AbstractCellet {

    private FerryService ferryService;

    public FerryCellet() {
        super(FerryService.NAME);
    }

    @Override
    public boolean install() {
        this.ferryService = new FerryService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(FerryService.NAME, this.ferryService);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(FerryService.NAME);
    }

    public FerryService getFerryService() {
        return this.ferryService;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FerryAction.Ping.name.equals(action)) {
            this.execute(new PingTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.PingAck.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.notifyAckBundles(dialect);
                }
            });
        }
        else if (FerryAction.Report.name.equals(action)) {
            this.execute(new ReportTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.ReportAck.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.notifyAckBundles(dialect);
                }
            });
        }
        else if (FerryAction.TakeOutTenet.name.equals(action)) {
            this.execute(new TakeOutTenetTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.QueryDomain.name.equals(action)) {
            this.execute(new QueryDomainTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.JoinDomain.name.equals(action)) {
            this.execute(new JoinDomainTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.QuitDomain.name.equals(action)) {
            this.execute(new QuitDomainTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FerryAction.CheckIn.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.checkIn(dialect, talkContext);
                }
            });
        }
        else if (FerryAction.CheckOut.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.checkOut(dialect, talkContext);
                }
            });
        }
        else if (FerryAction.Tenet.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.triggerTenet(dialect);
                }
            });
        }
        else if (FerryAction.Synchronize.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.processSynchronize(dialect, talkContext);
                }
            });
        }
        else {
            Logger.w(this.getClass(), "Unknown action: " + action);
        }
    }
}
