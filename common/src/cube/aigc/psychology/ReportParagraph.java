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
public class ReportParagraph implements JSONable {

    public String title;

    public int score;

    private String overview = "";

    private List<String> descriptions = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    public ReportParagraph(String title) {
        this.title = title;
    }

    public ReportParagraph(JSONObject json) {
        this.title = json.getString("title");
        this.score = json.getInt("score");
        this.overview = json.getString("overview");
        JSONArray array = json.getJSONArray("descriptions");
        for (int i = 0; i < array.length(); ++i) {
            this.descriptions.add(array.getString(i));
        }
        array = json.getJSONArray("suggestions");
        for (int i = 0; i < array.length(); ++i) {
            this.suggestions.add(array.getString(i));
        }
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public void addDescriptions(List<String> list) {
        this.descriptions.addAll(list);
    }

    public List<String> getDescriptions() {
        return this.descriptions;
    }

    public void addSuggestions(List<String> list) {
        this.suggestions.addAll(list);
    }

    public String markdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("## ").append(this.title).append("\n\n");
        buf.append("### ").append("概述\n\n");
        buf.append(this.overview).append("\n\n");
        buf.append("### ").append("描述\n\n");
        for (String description : this.descriptions) {
            buf.append(description).append("\n");
        }
        buf.append("\n");
        buf.append("### ").append("建议\n\n");
        for (String suggestion : this.suggestions) {
            buf.append(suggestion).append("\n");
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("score", this.score);
        json.put("overview", this.overview);

        JSONArray array = new JSONArray();
        for (String text : this.descriptions) {
            array.put(text);
        }
        json.put("descriptions", array);

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
