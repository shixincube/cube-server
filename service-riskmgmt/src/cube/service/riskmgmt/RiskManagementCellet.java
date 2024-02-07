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
