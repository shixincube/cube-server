/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.RiskManagementAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.riskmgmt.task.GetContactRiskTask;
import cube.service.riskmgmt.task.ModifyContactRiskTask;

/**
 * 风控管理 Cellet 单元。
 */
public class RiskManagementCellet extends AbstractCellet {

    private RiskManagement riskManagement;

    public RiskManagementCellet() {
        super(RiskManagement.NAME);
    }

    public RiskManagement getService() {
        return this.riskManagement;
    }

    @Override
    public boolean install() {
        this.riskManagement = new RiskManagement();

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(RiskManagement.NAME, this.riskManagement);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(RiskManagement.NAME);

        this.riskManagement = null;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

        if (RiskManagementAction.GetContactRisk.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetContactRiskTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (RiskManagementAction.ModifyContactRisk.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ModifyContactRiskTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (RiskManagementAction.ListContactBehaviors.name.equals(action)) {
        }
    }
}
