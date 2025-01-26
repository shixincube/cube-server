/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.notice;

/**
 * 通知类型。
 */
public class NoticeType {

    /**
     * 仅显示一次的通知。
     */
    public final static int ONLY_ONCE = 1;

    /**
     * 每日仅显示一次的通知。
     */
    public final static int ONLY_ONCE_A_DAY = 2;

    /**
     * 每次启动都显示的通知。
     */
    public final static int EVERY_TIME_START = 4;

    private NoticeType() {
    }
}
