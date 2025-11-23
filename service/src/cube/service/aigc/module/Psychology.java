/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cube.aigc.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated
 */
public class Psychology implements Module {

    private final String name = "Psychology";

    private List<String> matchingWords;

    public Psychology() {
        this.matchingWords = new ArrayList<>();
        this.matchingWords.add("心理学");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
