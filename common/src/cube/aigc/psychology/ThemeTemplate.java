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

    public final List<String> paragraphList = new ArrayList<>();

    public final List<String> paragraphPromptFormatList = new ArrayList<>();

    /**
     * 每个段落对应的提示词链。
     */
    public final List<PromptChaining> paragraphChainingList = new ArrayList<>();

    private String featurePromptFormat;

    private ThemeTemplate(Theme theme, String featurePromptFormat) {
        this.theme = theme;
        this.featurePromptFormat = featurePromptFormat;
    }

    public String formatFeaturePrompt(String word) {
        return String.format(this.featurePromptFormat, word, word);
    }

    public static ThemeTemplate makeStressThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Stress,
                "将%s这种特点结合心理学的压力特征，描述一下%s是如何影响压力的。");

        template.paragraphList.add("压力的主要表现");
        template.paragraphList.add("压力的调整");
        template.paragraphList.add("总结");

        template.paragraphPromptFormatList.add("已知信息：%s。作为心理学咨询专家，结合心理学里心理压力特点给出对压力的表现描述。");
        template.paragraphPromptFormatList.add("已知信息：%s。这些压力表现应该如何调整，给我一些建议。");
        template.paragraphPromptFormatList.add("基于最近我的压力表现：%s，综合性地给我总结一些结论，能让我知道后续我应该怎么解决这些压力。");

        return template;
    }

    public static ThemeTemplate makeFamilyRelationshipsThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.FamilyRelationships,
                "将%s这种特点结合心理学里的家庭关系特征，描述一下%s是如何影响家庭关系的。");
        return template;
    }

    public static ThemeTemplate makeIntimacyThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Intimacy,
                "将%s这种特点结合心理学的亲密关系特征，描述一下%s是如何影响亲密关系的。");
        return template;
    }

    public static ThemeTemplate makeCognitionThemeTemplate() {
        ThemeTemplate template = new ThemeTemplate(Theme.Cognition,
                "将%s这种特点结合心理学的认知特征，描述一下%s是如何影响人的认知的。");
        return template;
    }
}