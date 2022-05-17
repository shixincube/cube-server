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

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
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
