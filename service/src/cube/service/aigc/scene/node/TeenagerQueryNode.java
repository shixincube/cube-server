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

public class TeenagerQueryNode extends StrategyNode {

    private String query;

    private QueryRevolver revolver;

    private PaintingReport report;

    public TeenagerQueryNode(String query, QueryRevolver revolver, PaintingReport report) {
        super(ModelConfig.BAIZE_NEXT_UNIT);
        this.query = query;
        this.revolver = revolver;
        this.report = report;
    }

    @Override
    public String perform(GeneratingRecord input) {
        if (input.answer.length() < 10) {
            Logger.d(this.getClass(), "#perform - No query: " + this.query);
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append("此评测数据由AiXinLi模型生成，采用的评测方法是“房树人”绘画评测。");
        result.append("评测数据的受测人是匿名的，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
        result.append("评测日期是：").append(this.revolver.formatReportDate(report)).append("。\n\n");

        if (!this.report.getPermission().isPermissioned()) {
            // 无权限
            result.append("具体的评测数据因为权限不足无法获得详细数据，仅限于上述信息回答问题“");
            result.append(this.query);
            result.append("”。\n");
            result.append("要求如下：\n\n- 如果无法从中得到答案，请说“受限于未能获得评测报告全部数据无法为您提供更多信息。”");
            result.append("\n- 不允许在答案中添加编造成分。");
            result.append("\n");
            return result.toString();
        }

        result.append("受测人有以下的行为表现：\n");
        result.append(this.filterFirstLine(input.answer));
        if (!result.toString().endsWith("\n")) {
            result.append("\n\n");
        }

        result.append("受测人的心理特征如下：\n");
        result.append(this.report.getSummary());
        if (!result.toString().endsWith("\n")) {
            result.append("\n\n");
        }

        String prompt = String.format(Resource.getInstance().getCorpus("report", "SYMPTOM_STRATEGY_WITH_QUERY"),
                result.toString(), this.query);
        return prompt;
    }
}
