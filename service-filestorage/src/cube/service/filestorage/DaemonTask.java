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

package cube.service.filestorage;

import cube.service.filestorage.system.FileDescriptor;

import java.util.Iterator;

/**
 * 守护任务。
 */
public class DaemonTask implements Runnable {

    /**
     * 文件描述符的超时时长。
     */
    private final long fileDescriptorTimeout = 60L * 60L * 1000L;

    private FileStorageService service;

    public DaemonTask(FileStorageService service) {
        this.service = service;
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

        // TODO 清理 File Hierarchy
    }
}
