/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    public final static String CONTACT = "contact";

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

    public FileStoragePluginContext(FileLabel fileLabel, Contact contact, Device device) {
        this.fileLabel = fileLabel;
        this.contact = contact;
        this.device = device;
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
        else if (CONTACT.equals(name)) {
            return this.contact;
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
        else if (CONTACT.equals(name) && value instanceof Contact) {
            this.contact = (Contact) value;
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
