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

package cube.service.filestorage;

import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.service.auth.AuthService;
import cube.service.filestorage.system.FileDescriptor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 守护任务。
 */
public class DaemonTask implements Runnable {

    /**
     * 文件描述符的超时时长。
     */
    private final long fileDescriptorTimeout = 60 * 60 * 1000L;

    private FileStorageService service;

    private long lastCheckFileLabelTimestamp;

    private Map<Long, Contact> managedContacts;

    public DaemonTask(FileStorageService service) {
        this.service = service;
        this.lastCheckFileLabelTimestamp = System.currentTimeMillis();
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
    }
}
