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

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题模板。
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
