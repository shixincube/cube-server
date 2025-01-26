/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.command;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 命令。
 */
public abstract class Command implements JSONable, Runnable {

    public final String name;

    public Command(String name) {
        this.name = name;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
