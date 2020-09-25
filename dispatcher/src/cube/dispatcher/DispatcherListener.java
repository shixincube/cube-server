/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.dispatcher;

import cell.api.Nucleus;
import cell.carpet.CellListener;

import java.util.Timer;

public class DispatcherListener implements CellListener {

    private Timer timer;

    public DispatcherListener() {
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        Performer performer = new Performer(nucleus);

        // 设定范围
        Scope scope = new Scope();
        scope.cellets.add("Auth");
        scope.cellets.add("Contact");
        scope.cellets.add("Messaging");
        performer.addDirector("127.0.0.1", 6000, scope);

        nucleus.setParameter("performer", performer);
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.start();

        this.timer = new Timer();
        this.timer.schedule(new Daemon(performer), 10L * 1000L, 30L * 1000L);
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.stop();
    }
}
