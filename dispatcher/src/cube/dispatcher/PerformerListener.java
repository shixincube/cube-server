/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher;

import cell.core.talk.Primitive;

/**
 * 执行器监听器。
 */
public interface PerformerListener {

    void onReceived(String cellet, Primitive primitive);
}
