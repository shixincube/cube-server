/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.auth.AuthToken;

import java.util.List;

/**
 * 业务模组。
 * @deprecated
 */
public interface Module {

    String getName();

    List<String> getMatchingWords();

    void start();

    void stop();

    Flowable match(AuthToken token, String query);
}
