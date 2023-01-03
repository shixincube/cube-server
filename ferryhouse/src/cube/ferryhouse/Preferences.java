/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
