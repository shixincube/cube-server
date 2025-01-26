/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client;

/**
 * 超时回调。
 */
public interface TimeoutCallback {

    public void on(ServerClient client);
}
