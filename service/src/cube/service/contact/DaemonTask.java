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

package cube.service.contact;

import cube.common.entity.Contact;
import cube.common.entity.ContactAppendix;
import cube.common.entity.Device;
import cube.common.entity.GroupAppendix;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 管理守护任务。
 */
public class DaemonTask implements Runnable {

    private ContactManager manager;

    /**
     * 允许联系人数据实体空闲的最大时间。
     */
    private long contactIdle = 10L * 60L * 1000L;

    /**
     * 附录空闲时长。
     */
    private long appendixIdle = 30L * 60L * 1000L;

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

            this.processAppendix();
        }

        ++this.groupTickCount;
        if (this.groupTickCount >= this.groupTick) {
            this.groupTickCount = 0;
            this.processGroups();
        }
    }

    private void processContacts() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<String, ContactTable>> ctiter = this.manager.onlineTables.entrySet().iterator();
        while (ctiter.hasNext()) {
            Map.Entry<String, ContactTable> entry = ctiter.next();
            ContactTable table = entry.getValue();

            Iterator<Contact> citer = table.onlineContacts.values().iterator();
            while (citer.hasNext()) {
                Contact contact = citer.next();

                List<Device> list = contact.getDeviceList();
                if (list.isEmpty()) {
                    if (now - contact.getTimestamp() > this.contactIdle) {
                        // 联系人已经没有设备，移除联系人
                        citer.remove();
                    }

                    continue;
                }

                for (Device device : list) {
                    if (null != device.getTalkContext()) {
                        if (!device.getTalkContext().isValid()) {
                            // 该设备已失效
                            contact.removeDevice(device);
                        }
                    }
                }

                if (0 == contact.numDevices()) {
                    if (now - contact.getTimestamp() > this.contactIdle) {
                        citer.remove();
                    }
                }
            }
        }
    }

    private void processGroups() {
        Iterator<Map.Entry<String, GroupTable>> gtiter = this.manager.activeGroupTables.entrySet().iterator();
        while (gtiter.hasNext()) {
            Map.Entry<String, GroupTable> entry = gtiter.next();
            GroupTable gt = entry.getValue();
            gt.submitActiveTime();
        }
    }

    private void processAppendix() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<String, ContactAppendix>> caiter = this.manager.contactAppendixMap.entrySet().iterator();
        while (caiter.hasNext()) {
            Map.Entry<String, ContactAppendix> entry = caiter.next();
            if (now - entry.getValue().getTimestamp() > this.appendixIdle) {
                caiter.remove();
            }
        }

        Iterator<Map.Entry<String, GroupAppendix>> gaiter = this.manager.groupAppendixMap.entrySet().iterator();
        while (gaiter.hasNext()) {
            Map.Entry<String, GroupAppendix> entry = gaiter.next();
            if (now - entry.getValue().getTimestamp() > this.appendixIdle) {
                gaiter.remove();
            }
        }
    }
}
