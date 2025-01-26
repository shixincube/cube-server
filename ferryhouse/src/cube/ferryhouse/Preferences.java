/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse;

/**
 * 偏好设置。
 */
public class Preferences {

    /**
     * 是否从云端同步数据到本地，仅在故障数据恢复时有效。
     */
    public final static String ITEM_SYNCH_DATA = "SynchronizeData";

    /**
     * 是否在重启后清空所有数据，配置数据除外。
     */
    public final static String ITEM_CLEANUP_WHEN_REBOOT = "CleanupWhenReboot";

    /**
     * 最大存储空间大小。
     */
    public final static String ITEM_MAX_STORAGE_SPACE_SIZE = "MaxStorageSpaceSize";

    public boolean cleanupWhenReboot = false;

    public boolean synchronizeData = true;

    public long maxStorageSpaceSize = 256L * 1024 * 1024 * 1024;

    public Preferences() {
    }
}
