/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import java.util.List;

/**
 * 业务模组。
 */
public interface Module {

    String getName();

    List<String> getMatchingWords();

    void start();

    void stop();
}
