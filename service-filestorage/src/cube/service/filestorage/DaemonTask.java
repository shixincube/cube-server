/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage;

import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.FileLabel;
import cube.service.auth.AuthService;
import cube.service.filestorage.system.FileDescriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 守护任务。
 */
public class DaemonTask implements Runnable {

    /**
     * 文件描述符的超时时长。
     */
    private final long fileDescriptorTimeout = 24 * 60 * 60 * 1000L;

    private FileStorageService service;

    private long lastCheckFileLabelTimestamp;

    private List<ManagedContact> managedContacts;

    public DaemonTask(FileStorageService service) {
        this.service = service;
        this.lastCheckFileLabelTimestamp = System.currentTimeMillis();
        this.managedContacts = new ArrayList<>();
    }

    public void addManagedContact(Contact contact, Device device) {
        this.removeManagedContact(contact, device);

        ManagedContact managedContact = new ManagedContact(contact, device);
        long size = this.service.getFileHierarchyManager().countFileTotalSize(contact.getDomain().getName(),
                contact.getId());
        managedContact.spaceSize = size;

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (service.notifyPerformance(managedContact.contact, managedContact.device, size)) {
                    managedContact.notified = true;
                }
            }
        });

        synchronized (this.managedContacts) {
            this.managedContacts.add(managedContact);
        }
    }

    public void removeManagedContact(Contact contact, Device device) {
        synchronized (this.managedContacts) {
            for (ManagedContact mc : this.managedContacts) {
                if (mc.contact.getId().equals(contact.getId()) && mc.device.equals(device)) {
                    this.managedContacts.remove(mc);
                    break;
                }
            }
        }
    }

    protected ManagedContact getManagedContact(Contact contact) {
        synchronized (this.managedContacts) {
            for (ManagedContact mc : this.managedContacts) {
                if (mc.contact.getId().equals(contact.getId())) {
                    return mc;
                }
            }
        }

        return null;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        Iterator<FileDescriptor> fditer = this.service.fileDescriptors.values().iterator();
        while (fditer.hasNext()) {
            FileDescriptor descriptor = fditer.next();
            if (now - descriptor.getTimestamp() > this.fileDescriptorTimeout) {
                fditer.remove();
            }
        }

        if (now - this.lastCheckFileLabelTimestamp > 12L * 60 * 60 * 1000) {
            // 更新时间戳
            this.lastCheckFileLabelTimestamp = now;

            (new Thread() {
                @Override
                public void run() {
                    AuthService authService = (AuthService) service.getKernel().getModule(AuthService.NAME);
                    for (String domainName : authService.getDomainList()) {
                        // 获取超期的文件标签
                        List<FileLabel> fileLabelList = service.serviceStorage.listFileLabel(domainName, now);
                        for (FileLabel fileLabel : fileLabelList) {
                            // 删除本地文件
                            service.deleteFile(domainName, fileLabel);
                        }
                    }
                }
            }).start();
        }

        synchronized (this.managedContacts) {
            for (ManagedContact mc : this.managedContacts) {
                if (!mc.notified) {
                    mc.notified = true;
                    boolean result = this.service.notifyPerformance(mc.contact, mc.device, mc.spaceSize);
                    if (!result) {
                        mc.notified = false;
                    }
                }
            }
        }
    }


    /**
     * 被管理的联系人。
     */
    protected class ManagedContact {

        protected long timestamp = System.currentTimeMillis();

        protected boolean notified = false;

        protected Contact contact;

        protected Device device;

        protected long spaceSize;

        protected ManagedContact(Contact contact, Device device) {
            this.contact = contact;
            this.device = device;
        }
    }
}
