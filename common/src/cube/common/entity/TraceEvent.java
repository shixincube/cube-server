/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 节点事件。
 */
public final class TraceEvent {

    /**
     * 发送。
     */
    public final static String Transmit = "Transmit";

    /**
     * 打开。
     */
    public final static String Open = "Open";

    /**
     * 转发。
     */
    public final static String Forward = "Forward";

    /**
     * 归档。
     */
    public final static String Archive = "Archive";

    /**
     * 删除。
     */
    public final static String Delete = "Delete";

    /**
     * 重命名。
     */
    public final static String Rename = "Rename";

    /**
     * 复制。
     */
    public final static String Copy = "Copy";

    /**
     * 分享。
     */
    public final static String Share = "Share";

    /**
     * 浏览。
     */
    public final static String View = "View";

    /**
     * 浏览已丢失分享文件。
     */
    public final static String ViewLoss = "ViewLoss";

    /**
     * 浏览已过期分享文件。
     */
    public final static String ViewExpired = "ViewExpired";

    /**
     * 提取。
     */
    public final static String Extract = "Extract";

    private TraceEvent() {
    }
}
