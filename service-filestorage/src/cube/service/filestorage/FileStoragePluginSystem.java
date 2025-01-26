/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage;

import cube.file.hook.FileStorageHook;
import cube.plugin.PluginSystem;

/**
 * 文件存储服务插件系统。
 */
public class FileStoragePluginSystem extends PluginSystem<FileStorageHook> {

    public FileStoragePluginSystem() {
        super();
        this.build();
    }

    public FileStorageHook getSaveFileHook() {
        return this.getHook(FileStorageHook.SaveFile);
    }

    public FileStorageHook getDestroyFileHook() {
        return this.getHook(FileStorageHook.DestroyFile);
    }

    public FileStorageHook getNewFileHook() {
        return this.getHook(FileStorageHook.NewFile);
    }

    public FileStorageHook getDeleteFileHook() {
        return this.getHook(FileStorageHook.DeleteFile);
    }

    public FileStorageHook getDownloadFileHook() { return this.getHook(FileStorageHook.DownloadFile); }

    public FileStorageHook getCreateSharingTagHook() {
        return this.getHook(FileStorageHook.CreateSharingTag);
    }

    public FileStorageHook getTraceHook() {
        return this.getHook(FileStorageHook.Trace);
    }

    private void build() {
        FileStorageHook hook = new FileStorageHook(FileStorageHook.SaveFile);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.DestroyFile);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.NewFile);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.DeleteFile);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.DownloadFile);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.CreateSharingTag);
        this.addHook(hook);

        hook = new FileStorageHook(FileStorageHook.Trace);
        this.addHook(hook);
    }
}
