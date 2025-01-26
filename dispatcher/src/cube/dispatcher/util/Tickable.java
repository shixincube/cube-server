/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.util;

/**
 * 定时回调句柄。
 */
public interface Tickable {

    void onTick(long now);
}
