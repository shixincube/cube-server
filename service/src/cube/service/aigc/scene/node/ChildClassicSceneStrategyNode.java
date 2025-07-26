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
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.composition.BigFiveFactor;
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
        if (input.answer.contains("不是")) {
            Logger.d(this.getClass(), "#perform - No child query: " + this.query);
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append("此评测数据由AiXinLi模型生成，采用的评测方法是“房树人”绘画投射测试。");
        result.append("评测数据的受测人是匿名的，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
        result.append("评测日期是：").append(this.revolver.formatReportDate(report)).append("。\n\n");

        if (!this.report.getPermission().isPermissioned()) {
            // 无权限
            result.append("具体的评测数据因为权限不足无法获得详细数据，仅限于上述信息回答问题“");
            result.append(query);
            result.append("”。\n");
            result.append("要求如下：\n\n- 如果无法从中得到答案，请说“受限于未能获得评测报告全部数据无法为您提供更多信息。”");
            result.append("\n- 不允许在答案中添加编造成分。");
            result.append("\n");
            return result.toString();
        }

        result.append("受测人的心理特征摘要如下：\n");
        result.append(report.getSummary());
        result.append("\n\n");

        // 人格特质描述
        BigFivePersonality personality = this.report.getEvaluationReport().getPersonalityAccelerator()
                .getBigFivePersonality();
        result.append("受测人的大五人格描述如下：\n");
        result.append(this.revolver.filterPersonalityDescription(personality.getDescription()));
        result.append("\n\n");

        // 宜人性
        String term = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Obligingness.name);
        if (null != term) {
            result.append(term).append("\n\n");
        }
        if (personality.getObligingness() > BigFivePersonality.HighScore) {
            result.append("受测人人格特质里的宜人性高。");
            result.append("宜人性高的优势有：").append(personality.obligingnessAnnotation.getHighAdvantages());
            result.append("宜人性高的风险有：").append(personality.obligingnessAnnotation.getHighDisadvantages());
        }
        else if (personality.getObligingness() < BigFivePersonality.LowScore) {
            result.append("受测人人格特质里的宜人性低。");
            result.append("宜人性低的优势有：").append(personality.obligingnessAnnotation.getLowAdvantages());
            result.append("宜人性低的风险有：").append(personality.obligingnessAnnotation.getLowDisadvantages());
        }
        else {
            result.append("受测人人格特质里的宜人性适中。");
        }
        result.append("\n\n");

        // 尽责性
        term = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Conscientiousness.name);
        if (personality.getConscientiousness() > BigFivePersonality.HighScore) {
            if (null != term) {
                result.append(term).append("\n\n");
            }
        }

        // 外向性

        // 进取性

        // 情绪性

        Resource.getInstance().getCorpus("report", "SYMPTOM_STRATEGY_WITH_PERSONALITY");

        return result.toString();
    }
}
