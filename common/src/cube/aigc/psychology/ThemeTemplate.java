/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc.psychology;

import cube.aigc.PromptChaining;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题模板。
 */
public class ThemeTemplate {

    public final Theme theme;

    public final String featurePromptFormat = "将%s这种特点结合心理学的压力特征，描述一下%s是如何影响压力的。";

    public final List<String> paragraphList = new ArrayList<>();

    public final List<String> paragraphPromptFormatList = new ArrayList<>();

    /**
     * 每个段落对应的提示词链。
     */
    public final List<PromptChaining> paragraphChainingList = new ArrayList<>();

    private ThemeTemplate(Theme theme) {
        this.theme = theme;
    }

    public static ThemeTemplate makeStressThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Stress);
        template.paragraphList.add("压力的主要表现");
        template.paragraphList.add("压力的调整");
        template.paragraphList.add("总结");

        template.paragraphPromptFormatList.add("已知这些关键词描述工作、生活压力：%s。作为心理学咨询专家，通过对这些词的描述，结合心理学压力特点给出对压力的表现描述。");
        template.paragraphPromptFormatList.add("最近我的压力主要表现为：%s。这些压力表现应该如何调整，给我一些建议。");
        template.paragraphPromptFormatList.add("基于最近我的压力表现：%s，综合性地给我总结一些结论，能让我知道后续我应该怎么解决这些压力。");

        return template;
    }

    public static ThemeTemplate makeFamilyRelationshipsThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.FamilyRelationships);
        return template;
    }

    public static ThemeTemplate makeIntimacyThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Intimacy);
        return template;
    }

    public static ThemeTemplate makeCognitionThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Cognition);
        return template;
    }
}
