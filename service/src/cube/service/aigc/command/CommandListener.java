/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.command;

/**
 * 命令执行监听器。
 * @deprecated
 */
public interface CommandListener {

    void onCompleted(Command command);

    void onFailed(int code);
}
