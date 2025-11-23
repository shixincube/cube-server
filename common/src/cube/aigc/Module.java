/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

/**
 * 业务模组。
 */
public interface Module {

    String getName();

    void start();

    void stop();
}
