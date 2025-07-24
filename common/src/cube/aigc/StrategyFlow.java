/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;

public class StrategyFlow implements Flowable {

    private StrategyNode entry;

    public StrategyFlow(StrategyNode entry) {
        this.entry = entry;
    }

    @Override
    public GeneratingRecord generate(Generatable generator) {
        StrategyNode node = this.entry;
        GeneratingRecord result = null;
        while (null != node) {
            // 执行生成提示词
            String prompt = node.perform(result);
            if (null == prompt) {
                result = null;
                break;
            }

            // 生成数据
            result = generator.generateText(node.getUnitName(), prompt, new GeneratingOption(), null);
            if (null == result) {
                break;
            }

            node = node.next();
        }

        return result;
    }
}
