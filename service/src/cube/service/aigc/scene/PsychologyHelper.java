/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            if (score <= 40) {
                query = "六维分析中" + dim.displayName + "维度得分低的表现";
                rate = 1;
            } else if (score >= 80) {
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
