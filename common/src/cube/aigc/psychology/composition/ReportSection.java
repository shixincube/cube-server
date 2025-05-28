/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.common.JSONable;
import cube.util.TextUtils;
import org.json.JSONObject;

public class ReportSection implements JSONable {

    public Indicator indicator;

    public String title;

    public String report;

    public String suggestion;

    public IndicatorRate rate;

    public ReportSection(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.report = TextUtils.fixChineseBlank(json.getString("report").trim());
        this.suggestion = TextUtils.fixChineseBlank(json.getString("suggestion").trim());
        this.title = json.has("title") ? TextUtils.fixChineseBlank(json.getString("title").trim())
                : this.indicator.name;
        this.rate = json.has("rate") ? IndicatorRate.parse(json.getJSONObject("rate")) : IndicatorRate.None;
    }

    public ReportSection(Indicator indicator, String title, String report, String suggestion, IndicatorRate rate) {
        this.indicator = indicator;
        this.title = TextUtils.fixChineseBlank(title.trim());
        this.report = TextUtils.fixChineseBlank(report.trim());
        this.suggestion = TextUtils.fixChineseBlank(suggestion.trim());
        this.rate = rate;
    }

    public JSONObject toPermissionJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("title", this.title);
        json.put("report", TextUtils.clip(this.report));
        json.put("suggestion", TextUtils.clip(this.suggestion));
        json.put("rate", this.rate.toJSON());
        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("title", this.title);
        json.put("report", this.report);
        json.put("suggestion", this.suggestion);
        json.put("rate", this.rate.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
