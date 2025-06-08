/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.common.entity.*;

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
    private long contactIdle = 10 * 60 * 1000L;

    /**
     * 附录空闲时长。
     */
    private long appendixIdle = 1 * 60 * 1000L;

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

        this.processSearchResult();

        this.processVerificationCode();
    }

    private void processContacts() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<String, ContactTable>> ctiter = this.manager.onlineTables.entrySet().iterator();
        while (ctiter.hasNext()) {
            Map.Entry<String, ContactTable> entry = ctiter.next();
            ContactTable table = entry.getValue();

            // 遍历所有在线联系人
            for (Contact contact : table.getOnlineContacts()) {

                List<Device> list = contact.getDeviceList();
                if (list.isEmpty()) {
                    if (now - contact.getTimestamp() > this.contactIdle) {
                        // 联系人已经没有设备，移除联系人
                        table.remove(contact);
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
                        table.remove(contact);
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
    }

    private void processSearchResult() {
        long now = System.currentTimeMillis();

        Iterator<ContactSearchResult> iter = this.manager.searchMap.values().iterator();
        while (iter.hasNext()) {
            ContactSearchResult result = iter.next();
            if (now - result.getTimestamp() > 5 * 60 * 1000) {
                iter.remove();
            }
        }
    }

    private void processVerificationCode() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, VerificationCode>> iter = this.manager.verificationCodes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, VerificationCode> entry = iter.next();
            if (now - entry.getValue().timestamp > 60 * 60 * 1000) {
                iter.remove();
            }
        }
    }
}
