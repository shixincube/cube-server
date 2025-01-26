/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.data.wechat;

/**
 * 消息时间精度。
 */
public final class DatePrecision {

    /**
     * 精确到小时分钟。
     */
    public final static int Minute = 0;

    /**
     * 精确到天。
     */
    public final static int Day = 1;

    /**
     * 未知精度。
     */
    public final static int Unknown = 9;

    private DatePrecision() {
    }
}
