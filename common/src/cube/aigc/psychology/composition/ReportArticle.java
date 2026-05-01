/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cell.util.Utils;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.common.Language;
import cube.common.entity.Article;
import cube.common.state.AIGCStateCode;
import cube.util.TimeUtils;
import org.json.JSONObject;

public class ReportArticle extends Article {

    public final Theme theme;

    public final String templateName;

    public final long timestamp;

    public IndicatorRate rate = IndicatorRate.Lowest;

    public AIGCStateCode state = AIGCStateCode.Ok;

    public ReportArticle(String title, Theme theme, String templateName, long timestamp) {
        super(title);
        this.theme = theme;
        this.templateName = templateName;
        this.timestamp = timestamp;
    }

    @Override
    public String toMarkdown() {
        StringBuilder buf = new StringBuilder();
        if (null != this.title) {
            buf.append("# ").append(this.title).append("\n\n");
        }

        buf.append("> ").append("报告日期：");
        buf.append(TimeUtils.formatDateString(this.timestamp, Language.Chinese));
        buf.append("\n\n");

        if (null != this.content) {
            buf.append(this.content).append("\n\n");
        }
        if (!this.paragraphs.isEmpty()) {
            for (Paragraph paragraph : this.paragraphs) {
                buf.append(paragraph.toMarkdown());
            }
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("theme", this.theme.code);
        json.put("templateName", this.templateName);
        json.put("timestamp", this.timestamp);
        json.put("rate", this.rate.value);
        json.put("state", this.state.code);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return super.toCompactJSON();
    }
}
