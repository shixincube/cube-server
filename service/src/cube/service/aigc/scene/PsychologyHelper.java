/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.Dataset;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.HexagonDimension;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class PsychologyHelper {

    private PsychologyHelper() {
    }

    public static void fillDimensionScoreDescription(Tokenizer tokenizer, HexagonDimensionScore sds) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        for (HexagonDimension dim : HexagonDimension.values()) {
            int score = sds.getDimensionScore(dim);
            String query = null;
            int rate = 0;
            if (score <= 60) {
                query = "六维分析中" + dim.displayName + "维度得分低的表现";
                rate = 1;
            } else if (score >= 90) {
                query = "六维分析中" + dim.displayName + "维度得分高的表现";
                rate = 3;
            } else {
                query = "六维分析中" + dim.displayName + "维度得分中等的表现";
                rate = 2;
            }

            List<String> keywordList = analyzer.analyzeOnlyWords(query, 7);

            Dataset dataset = Resource.getInstance().loadDataset();
            String answer = dataset.matchContent(keywordList.toArray(new String[0]), 7);
            if (null != answer) {
                sds.record(dim, rate, answer);
            }
            else {
                Logger.e(PsychologyHelper.class, "#fillDimensionScoreDescription - Answer is null: " + query);
            }
        }
    }
}
