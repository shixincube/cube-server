/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.ferry;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.ferry.FerryAction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class FerryCellet extends AbstractCellet {

    private ExecutorService executor;

    private FerryService ferryService;

    public FerryCellet() {
        super(FerryService.NAME);
    }

    @Override
    public boolean install() {
        this.executor = Executors.newCachedThreadPool();

        this.ferryService = new FerryService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(FerryService.NAME, this.ferryService);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(FerryService.NAME);

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FerryAction.CheckIn.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.checkIn(dialect, talkContext);
                }
            });
        }
        else if (FerryAction.CheckOut.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ferryService.checkOut(dialect, talkContext);
                }
            });
        }
    }
}
