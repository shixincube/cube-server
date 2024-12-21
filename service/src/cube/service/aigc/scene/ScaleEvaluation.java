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

        return Resource.getInstance().loadScaleByFilename(con.name);
    }
}
