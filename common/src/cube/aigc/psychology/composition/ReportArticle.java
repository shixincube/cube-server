/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Theme;
import cube.common.entity.Article;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

public class ReportArticle extends Article {

    public final Theme theme;

    public final String templateName;

    public final long timestamp;

    public long reportSn;

    public AIGCStateCode state;

    public ReportArticle(String title, Theme theme, String templateName, long timestamp) {
        super(title);
        this.theme = theme;
        this.templateName = templateName;
        this.timestamp = timestamp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("theme", this.theme.code);
        json.put("templateName", this.templateName);
        json.put("timestamp", this.timestamp);
        json.put("reportSn", reportSn);
        json.put("state", this.state.code);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return super.toCompactJSON();
    }
}
