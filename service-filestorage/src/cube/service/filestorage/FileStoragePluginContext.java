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

import cube.common.entity.*;
import cube.plugin.PluginContext;
import cube.service.filestorage.hierarchy.Directory;

/**
 * 文件存储插件上下文。
 */
public class FileStoragePluginContext extends PluginContext {

    public final static String FILE_LABEL = "fileLabel";

    public final static String DIRECTORY = "directory";

    public final static String SHARING_TAG = "sharingTag";

    public final static String VISIT_TRACE = "visitTrace";

    private FileLabel fileLabel;

    private Directory directory;

    private Contact contact;

    private Device device;

    private SharingTag sharingTag;

    private VisitTrace visitTrace;

    public FileStoragePluginContext(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    public FileStoragePluginContext(Directory directory, FileLabel fileLabel, Contact contact, Device device) {
        this.directory = directory;
        this.fileLabel = fileLabel;
        this.contact = contact;
        this.device = device;
    }

    public FileStoragePluginContext(SharingTag sharingTag) {
        this.sharingTag = sharingTag;
    }

    public FileStoragePluginContext(VisitTrace visitTrace) {
        this.visitTrace = visitTrace;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public Directory getDirectory() {
        return this.directory;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    public SharingTag getSharingTag() {
        return this.sharingTag;
    }

    public VisitTrace getVisitTrace() {
        return this.visitTrace;
    }

    @Override
    public Object get(String name) {
        if (FILE_LABEL.equals(name)) {
            return this.fileLabel;
        }
        else if (DIRECTORY.equals(name)) {
            return this.directory;
        }
        else if (SHARING_TAG.equals(name)) {
            return this.sharingTag;
        }
        else if (VISIT_TRACE.equals(name)) {
            return this.visitTrace;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
        if (FILE_LABEL.equals(name) && value instanceof FileLabel) {
            this.fileLabel = (FileLabel) value;
        }
        else if (DIRECTORY.equals(name) && value instanceof Directory) {
            this.directory = (Directory) value;
        }
        else if (SHARING_TAG.equals(name) && value instanceof SharingTag) {
            this.sharingTag = (SharingTag) value;
        }
        else if (VISIT_TRACE.equals(name) && value instanceof VisitTrace) {
            this.visitTrace = (VisitTrace) value;
        }
    }
}
