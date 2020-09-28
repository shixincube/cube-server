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

package cube.service.contact;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理器守护线程。
 */
public class ManagementDaemon extends Thread {

    private ContactManager manager;

    private boolean spinning = true;

    private long spinningSleep = 10L * 1000L;

    private int onlineContactTick = 60;
    private int onlineContactTickCount = 0;

    public ManagementDaemon(ContactManager manager) {
        this.manager = manager;
        setName("ManagementDaemon");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (this.spinning) {
            try {
                Thread.sleep(this.spinningSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ++this.onlineContactTickCount;
            if (this.onlineContactTickCount >= this.onlineContactTick) {
                this.onlineContactTickCount = 0;
                this.processOnlineContacts();
            }
        }
    }

    public final void terminate() {
        this.spinning = false;
    }

    private void processOnlineContacts() {
//        ConcurrentHashMap
    }
}
