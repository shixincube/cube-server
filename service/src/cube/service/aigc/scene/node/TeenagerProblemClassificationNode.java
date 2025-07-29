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
import cube.aigc.psychology.Resource;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.scene.QueryRevolver;

public class TeenagerProblemClassificationNode extends StrategyNode  {

    private QueryRevolver revolver;

    private PaintingReport report;

    public TeenagerProblemClassificationNode(QueryRevolver revolver, PaintingReport report) {
        super(ModelConfig.BAIZE_NEXT_UNIT);
        this.revolver = revolver;
        this.report = report;
    }

    @Override
    public String perform(GeneratingRecord input) {
        if (input.answer.contains("不是")) {
            Logger.d(this.getClass(), "#perform - No teenager query");
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append("此评测数据由AiXinLi模型生成，采用的评测方法是“房树人”绘画投射测试。");
        result.append("评测数据的受测人是匿名的，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
//        result.append("评测日期是：").append(this.revolver.formatReportDate(report)).append("。\n\n");

        if (!this.report.getPermission().isPermissioned()) {
            Logger.d(this.getClass(), "#perform - No Permission: " + this.report.sn);
            return null;
        }

        result.append("受测人的心理特征如下：\n");
        result.append(this.report.getSummary());
        if (!result.toString().endsWith("\n")) {
            result.append("\n\n");
        }

        String table = this.report.getAttribute().age <= 12 ?
                Resource.getInstance().getChildStrategyContent() : Resource.getInstance().getTeenagerStrategyContent();

        String prompt = String.format(
                Resource.getInstance().getCorpus("report", "SYMPTOM_STRATEGY_WITH_TEENAGER_TABLE"),
                result.toString(), table);
        return prompt;
    }
}
