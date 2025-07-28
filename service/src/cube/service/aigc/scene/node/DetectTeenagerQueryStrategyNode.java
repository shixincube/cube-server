/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.node;

import cube.aigc.ModelConfig;
import cube.aigc.StrategyNode;
import cube.common.entity.GeneratingRecord;

public class DetectTeenagerQueryStrategyNode extends StrategyNode {

    private String query;

    public DetectTeenagerQueryStrategyNode(String query) {
        super(ModelConfig.BAIZE_UNIT);
        this.query = query;
    }

    @Override
    public String perform(GeneratingRecord input) {
        StringBuilder buf = new StringBuilder();
        buf.append("已知提问内容：").append(this.query).append("。\n\n");
        buf.append("请问以上提问内容是否与询问孩子、学生或者青少年的相关情况，这些情况可以是心理状态、学业情况、日常表现或者人格特点。如果是请回答：是，如果不是请回答：不是。");
        return buf.toString();
    }
}
