/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.conference;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.ConferenceAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.conference.task.AcceptInvitationTask;
import cube.service.conference.task.CreateConferenceTask;
import cube.service.conference.task.DeclineInvitationTask;
import cube.service.conference.task.ListConferencesTask;

import java.util.concurrent.ExecutorService;

/**
 * 会议服务的 Cellet 。
 */
public class ConferenceServiceCellet extends AbstractCellet {

    private ExecutorService executor;

    public ConferenceServiceCellet() {
        super(ConferenceService.NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(ConferenceService.NAME, new ConferenceService());

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(ConferenceService.NAME);

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (ConferenceAction.ListConferences.name.equals(action)) {
            this.executor.execute(new ListConferencesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ConferenceAction.CreateConference.name.equals(action)) {
            this.executor.execute(new CreateConferenceTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ConferenceAction.AcceptInvitation.name.equals(action)) {
            this.executor.execute(new AcceptInvitationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ConferenceAction.DeclineInvitation.name.equals(action)) {
            this.executor.execute(new DeclineInvitationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }
}
