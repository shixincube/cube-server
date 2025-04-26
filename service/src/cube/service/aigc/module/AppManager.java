/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cube.aigc.Flowable;
import cube.aigc.Module;

import java.util.List;

public class AppManager implements Module {

    public AppManager() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public List<String> getMatchingWords() {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Flowable match(String content) {
        return null;
    }
}
