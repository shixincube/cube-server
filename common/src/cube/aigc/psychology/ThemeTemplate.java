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

package cube.aigc.psychology;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题模板。
 */
public class ThemeTemplate {

    public final Theme theme;

    private List<ParagraphPromptFormat> paragraphPromptFormatList;

    private RepresentationPromptFormat representationPromptFormat;

    public ThemeTemplate(String name, JSONObject json) {
        this.theme = Theme.parse(name);
        JSONObject prompt = json.getJSONObject("prompt");

        if (prompt.has("representation")) {
            this.representationPromptFormat = new RepresentationPromptFormat(prompt.getJSONObject("representation"));
        }

        this.paragraphPromptFormatList = new ArrayList<>();
        JSONArray array = prompt.getJSONArray("paragraphs");
        for (int i = 0; i < array.length(); ++i) {
            this.paragraphPromptFormatList.add(new ParagraphPromptFormat(array.getJSONObject(i)));
        }
    }

    public List<String> getTitles() {
        List<String> list = new ArrayList<>();
        for (ParagraphPromptFormat ppf : this.paragraphPromptFormatList) {
            list.add(ppf.title);
        }
        return list;
    }

    public String getExplain(int index) {
        if (index >= this.paragraphPromptFormatList.size()) {
            return null;
        }
        return this.paragraphPromptFormatList.get(index).explain;
    }

    public String formatFeaturePrompt(int index, String representationText) {
        if (index >= this.paragraphPromptFormatList.size()) {
            return null;
        }

        ParagraphPromptFormat format = this.paragraphPromptFormatList.get(index);
        String prompt = String.format(format.featureFormat, representationText);
        return prompt;
    }

    public String formatDescriptionPrompt(int index, String descriptionText) {
        if (index >= this.paragraphPromptFormatList.size()) {
            return null;
        }
        ParagraphPromptFormat format = this.paragraphPromptFormatList.get(index);
        String prompt = String.format(format.descriptionFormat, descriptionText);
        return prompt;
    }

    public String formatSuggestionPrompt(int index, String representationText) {
        if (index >= this.paragraphPromptFormatList.size()) {
            return null;
        }

        ParagraphPromptFormat format = this.paragraphPromptFormatList.get(index);
        String prompt = String.format(format.suggestionFormat, representationText);
        return prompt;
    }

    public String formatOpinionPrompt(int index, String suggestionText) {
        if (index >= this.paragraphPromptFormatList.size()) {
            return null;
        }
        ParagraphPromptFormat format = this.paragraphPromptFormatList.get(index);
        String prompt = String.format(format.opinionFormat, suggestionText);
        return prompt;
    }

    public String formatRepresentationDescriptionPrompt(String representation,
                                                        int age, String gender, String markedRepresentation) {
        if (null == this.representationPromptFormat) {
            return null;
        }

        return String.format(this.representationPromptFormat.description,
                age, filterGender(gender), markedRepresentation);
    }

    public String formatSuggestionPrompt(String representation) {
        if (null == this.representationPromptFormat) {
            return null;
        }

        return String.format(this.representationPromptFormat.suggestion,
                representation);
    }

    private String filterGender(String gender) {
        if (gender.equalsIgnoreCase("male")) {
            return "男";
        }
        else if (gender.equalsIgnoreCase("female")) {
            return "女";
        }
        else if (gender.contains("男")) {
            return "男";
        }
        else {
            return "女";
        }
    }

    /*
    public static ThemeTemplate makeStressThemeTemplate() {
        ThemeTemplate template = null;
        return template;
    }

    public static ThemeTemplate makeFamilyRelationshipsThemeTemplate() {
        return null;
    }

    public static ThemeTemplate makeIntimacyThemeTemplate() {
        return null;
    }

    public static ThemeTemplate makeCognitionThemeTemplate() {;
        return null;
    }
    */

    public class ParagraphPromptFormat {

        public final String title;

        public String explain;

        public String featureFormat;

        public String descriptionFormat;

        public String suggestionFormat;

        public String opinionFormat;

        public ParagraphPromptFormat(JSONObject json) {
            this.title = json.getString("title");
            this.explain = json.getString("explain");
            this.featureFormat = json.getString("feature");
            this.descriptionFormat = json.getString("description");
            this.suggestionFormat = json.getString("suggestion");
            this.opinionFormat = json.getString("opinion");
        }
    }

    public class RepresentationPromptFormat {

        public final String description;

        public final String suggestion;

        public RepresentationPromptFormat(JSONObject json) {
            this.description = json.getString("description");
            this.suggestion = json.getString("suggestion");
        }
    }
}
