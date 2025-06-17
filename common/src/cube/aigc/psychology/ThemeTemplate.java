/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题模板。
 *
 * @deprecated
 */
public class ThemeTemplate {

    public final Theme theme;

    private List<SymptomContent> symptomPrompts;

    public ThemeTemplate(String name, JSONObject json) {
        this.theme = Theme.parse(name);
        this.symptomPrompts = new ArrayList<>();

        JSONObject prompt = json.getJSONObject("prompt");
        if (prompt.has("symptoms")) {
            try {
                JSONArray array = prompt.getJSONArray("symptoms");
                for (int i = 0; i < array.length(); ++i) {
                    this.symptomPrompts.add(new SymptomContent(array.getJSONObject(i)));
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "", e);
            }
        }
    }

    public SymptomContent findSymptomContent(String text) {
        for (SymptomContent sc : this.symptomPrompts) {
            if (text.contains(sc.word)) {
                return sc;
            }
        }

        return null;
    }

    public class SymptomContent {

        public final String word;

        public final String content;

        public SymptomContent(JSONObject json) {
            this.word = json.getString("word");
            this.content = json.getString("content");
        }
    }
}
