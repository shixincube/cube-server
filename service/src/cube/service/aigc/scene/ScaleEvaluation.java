/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScalesConfiguration;

/**
 * 量表评估器。
 */
public class ScaleEvaluation {

    public ScaleEvaluation() {
    }

    public Scale recommendScale(EvaluationReport evaluationReport) {
        Attention attention = evaluationReport.getAttention();
        if (Attention.NoAttention == attention) {
            Logger.d(this.getClass(), "#recommendScale - No attention");
            return null;
        }

        ScalesConfiguration configuration = new ScalesConfiguration();
        ScalesConfiguration.Category category = configuration.getCategory(ScalesConfiguration.CATEGORY_MENTAL_HEALTH);
        ScalesConfiguration.Configuration con = category.find("精神症状");
        if (null == con) {
            Logger.d(this.getClass(), "#recommendScale - Can NOT find scale configuration");
            return null;
        }

        return Resource.getInstance().loadScaleByFilename(con.name, evaluationReport.getContactId());
    }
}
