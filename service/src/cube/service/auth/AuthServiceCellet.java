/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.common.action.AuthAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.auth.task.ApplyTokenTask;
import cube.service.auth.task.GetDomainTask;
import cube.service.auth.task.GetTokenTask;
import cube.service.auth.task.LatencyTask;

/**
 * 授权服务 Cellet 单元。
 */
public class AuthServiceCellet extends AbstractCellet {

    public AuthServiceCellet() {
        super(AuthService.NAME);
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(AuthService.NAME, new AuthService());

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(AuthService.NAME);
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (AuthAction.ApplyToken.name.equals(action)) {
            this.execute(new ApplyTokenTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AuthAction.GetToken.name.equals(action)) {
            this.execute(new GetTokenTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AuthAction.GetDomain.name.equals(action)) {
            this.execute(new GetDomainTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AuthAction.Latency.name.equals(action)) {
            this.execute(new LatencyTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }

}
