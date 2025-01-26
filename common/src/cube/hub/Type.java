/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub;

/**
 * 消息类型描述。
 */
public final class Type {

    /**
     * 事件。
     * 用于进行数据传输。
     */
    public final static String Event = "HubEvent";

    /**
     * 信号。
     * 用于进行操作控制。
     */
    public final static String Signal = "HubSignal";

    private Type() {
    }
}
