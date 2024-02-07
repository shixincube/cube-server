/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
