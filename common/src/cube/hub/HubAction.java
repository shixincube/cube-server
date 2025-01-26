/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub;

/**
 * Hub 动作。
 */
public enum HubAction {

    /**
     * 触发事件。
     */
    TriggerEvent("triggerEvent"),

    /**
     * 传输信号。
     */
    TransmitSignal("transmitSignal"),

    /**
     * 管道操作。
     */
    Channel("channel"),

    /**
     * 放置文件。
     */
    PutFile("putFile")

    ;

    public final String name;

    HubAction(String name) {
        this.name = name;
    }
}
