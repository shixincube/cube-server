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

        if (!this.report.getPermission().isPermissioned()) {
            Logger.d(this.getClass(), "#perform - No Permission: " + this.report.sn);
            return null;
        }

        StringBuilder data = new StringBuilder();
        data.append(this.report.getSummary());

        String table = this.report.getAttribute().age <= 12 ?
                Resource.getInstance().getChildStrategyContent() : Resource.getInstance().getTeenagerStrategyContent();

        String prompt = String.format(
                Resource.getInstance().getCorpus("report", "SYMPTOM_STRATEGY_WITH_TEENAGER_TABLE"),
                data.toString(), table);
        return prompt;
    }
}
