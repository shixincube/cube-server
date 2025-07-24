/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.node;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.StrategyNode;
import cube.aigc.psychology.PaintingReport;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.scene.QueryRevolver;

public class ChildClassicSceneStrategyNode extends StrategyNode {

    private String query;

    private QueryRevolver revolver;

    private PaintingReport report;

    public ChildClassicSceneStrategyNode(String query, QueryRevolver revolver, PaintingReport report) {
        super(ModelConfig.BAIZE_NEXT_UNIT);
        this.query = query;
        this.revolver = revolver;
        this.report = report;
    }

    @Override
    public String perform(GeneratingRecord input) {
        if (input.answer.equalsIgnoreCase("不是")) {
            Logger.d(this.getClass(), "#perform - No child query: " + this.query);
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append("此评测数据由AiXinLi模型生成，采用的评测方法是“房树人”绘画投射测试。");
        result.append("评测数据的受测人是匿名的，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
        result.append("评测日期是：").append(this.revolver.formatReportDate(report)).append("。\n\n");

        return result.toString();
    }
}
