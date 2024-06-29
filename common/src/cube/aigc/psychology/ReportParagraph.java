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

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 报告段落。
 */
@Deprecated
public class ReportParagraph implements JSONable {

    public String title;

    public int score;

    public String description = "";

    public String opinion = "";

    private List<String> features = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    public ReportParagraph(String title) {
        this.title = title;
    }

    public ReportParagraph(String title, int score, String description, String opinion) {
        this.title = title;
        this.score = score;
        this.description = description;
        this.opinion = opinion;
    }

    public ReportParagraph(JSONObject json) {
        this.title = json.getString("title");
        this.score = json.getInt("score");
        this.description = json.getString("description");
        this.opinion = json.getString("opinion");
        JSONArray array = json.getJSONArray("features");
        for (int i = 0; i < array.length(); ++i) {
            this.features.add(array.getString(i));
        }
        array = json.getJSONArray("suggestions");
        for (int i = 0; i < array.length(); ++i) {
            this.suggestions.add(array.getString(i));
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public void addFeatures(List<String> list) {
        this.features.addAll(list);
    }

    public void addFeature(String feature) {
        this.features.add(feature);
    }

    public List<String> getFeatures() {
        return this.features;
    }

    public void addSuggestions(List<String> list) {
        this.suggestions.addAll(list);
    }

    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }

    public List<String> getSuggestions() {
        return this.suggestions;
    }

    public String markdown(boolean details) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n\n");
        buf.append("## ").append(this.title);

        buf.append("\n\n");
        buf.append("### ").append("描述\n\n");
        buf.append(this.description);

        buf.append("\n\n");
        buf.append("### ").append("建议\n\n");
        buf.append(this.opinion);

        if (details) {
            buf.append("\n\n");
            buf.append("### ").append("特征\n");
            for (String feature : this.features) {
                buf.append("\n").append(feature);
            }

            buf.append("\n\n");
            buf.append("### ").append("指导\n");
            for (String suggestion : this.suggestions) {
                buf.append("\n").append(suggestion);
            }
        }

        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("score", this.score);
        json.put("description", this.description);
        json.put("opinion", this.opinion);

        JSONArray array = new JSONArray();
        for (String text : this.features) {
            array.put(text);
        }
        json.put("features", array);

        array = new JSONArray();
        for (String text : this.suggestions) {
            array.put(text);
        }
        json.put("suggestions", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
