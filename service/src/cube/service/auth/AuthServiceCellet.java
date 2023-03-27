/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.auth;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.AuthAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.auth.task.ApplyTokenTask;
import cube.service.auth.task.GetDomainTask;
import cube.service.auth.task.GetTokenTask;
import cube.service.auth.task.LatencyTask;

import java.util.concurrent.ExecutorService;

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
