/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.hook;

import cube.plugin.Hook;

/**
 * 文件存储服务插件钩子。
 */
public class FileStorageHook extends Hook {

    /**
     * 文件保存到系统。
     */
    public final static String SaveFile = "SaveFile";

    /**
     * 从系统里销毁文件。
     */
    public final static String DestroyFile = "DestroyFile";

    /**
     * 目录插入新文件。
     */
    public final static String NewFile = "NewFile";

    /**
     * 删除目录里文件。
     */
    public final static String DeleteFile = "DeleteFile";

    public final static String DownloadFile = "DownloadFile";

    public final static String CreateSharingTag = "CreateSharingTag";

    public final static String Trace = "Trace";

    /**
     * 构造函数。
     *
     * @param key 钩子关键字。
     */
    public FileStorageHook(String key) {
        super(key);
    }
}
