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

import java.util.Iterator;
import java.util.Map;

/**
 * 管理守护任务。
 */
public class DaemonTask implements Runnable {

    private ContactManager manager;

    /**
     * 间隔 10 分钟
     */
    private int contactTick = 10;
    private int contactTickCount = 0;

    /**
     * 间隔 5 分钟
     */
    private int groupTick = 5;
    private int groupTickCount = 0;

    public DaemonTask(ContactManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        ++this.contactTickCount;
        if (this.contactTickCount >= this.contactTick) {
            this.contactTickCount = 0;
            this.processContacts();
        }

        ++this.groupTickCount;
        if (this.groupTickCount >= this.groupTick) {
            this.groupTickCount = 0;
            this.processGroups();
        }
    }

    private void processContacts() {
    }

    private void processGroups() {
        Iterator<Map.Entry<String, GroupTable>> gtiter = this.manager.activeGroupTables.entrySet().iterator();
        while (gtiter.hasNext()) {
            Map.Entry<String, GroupTable> entry = gtiter.next();
            GroupTable gt = entry.getValue();
            gt.submitActiveTime();
        }
    }
}
