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

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Indicator;
import cube.common.JSONable;
import cube.util.TextUtils;
import org.json.JSONObject;

public class ReportSuggestion implements JSONable {

    public Indicator indicator;

    public String title;

    public String report;

    public String suggestion;

    public ReportSuggestion(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.report = TextUtils.fixChineseBlank(json.getString("report").trim());
        this.suggestion = TextUtils.fixChineseBlank(json.getString("suggestion").trim());
        this.title = json.has("title") ? TextUtils.fixChineseBlank(json.getString("title").trim())
                : this.indicator.name;
    }

    public ReportSuggestion(Indicator indicator, String title, String report, String suggestion) {
        this.indicator = indicator;
        this.title = TextUtils.fixChineseBlank(title.trim());
        this.report = TextUtils.fixChineseBlank(report.trim());
        this.suggestion = TextUtils.fixChineseBlank(suggestion.trim());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("report", this.report);
        json.put("suggestion", this.suggestion);
        json.put("title", this.title);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
