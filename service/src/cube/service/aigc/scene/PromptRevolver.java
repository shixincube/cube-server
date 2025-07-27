/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.common.entity.GeneratingRecord;

public class PromptRevolver {

    public final String content;

    public String prefix;

    public String postfix;

    public GeneratingRecord result;

    public PromptRevolver(String content) {
        this.content = content;
    }
}
