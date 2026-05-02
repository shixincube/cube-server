/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.Report;
import cube.common.JSONable;
import cube.common.entity.Article;
import cube.common.entity.User;
import org.json.JSONObject;

/**
 * 报告数据关系描述。
 */
public class ConversationRelation implements JSONable {

    public final String name;

    private long reportSn = 0;

    private Report report;

    private long articleSn = 0;

    private ReportArticle article;

    private long comprehensiveReportSn = 0;

    private ComprehensiveReport comprehensiveReport;

    private long uid = 0;

    private User user;

    public ConversationRelation() {
        this.name = "Anonymous";
    }

    public ConversationRelation(JSONObject json) {
        this.name = json.getString("name");
        if (json.has("reportSn")) {
            this.reportSn = json.getLong("reportSn");
        }
        if (json.has("articleSn")) {
            this.articleSn = json.getLong("articleSn");
        }
        if (json.has("comprehensiveReportSn")) {
            this.comprehensiveReportSn = json.getLong("comprehensiveReportSn");
        }
        if (json.has("uid")) {
            this.uid = json.getLong("uid");
        }
    }

    public boolean hasReport() {
        return this.reportSn > 0;
    }

    public long getReportSn() {
        return this.reportSn;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public Report getReport() {
        return this.report;
    }

    public boolean hasArticle() {
        return this.articleSn > 0;
    }

    public long getArticleSn() {
        return this.articleSn;
    }

    public void setArticle(ReportArticle article) {
        this.article = article;
    }

    public Article getArticle() {
        return this.article;
    }

    public boolean hasComprehensiveReport() {
        return this.comprehensiveReportSn > 0;
    }

    public long getComprehensiveReportSn() {
        return this.comprehensiveReportSn;
    }

    public void setComprehensiveReport(ComprehensiveReport comprehensiveReport) {
        this.comprehensiveReport = comprehensiveReport;
    }

    public ComprehensiveReport getComprehensiveReport() {
        return this.comprehensiveReport;
    }

    public boolean hasUser() {
        return this.uid > 0;
    }

    public long getUid() {
        return this.uid;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return this.user;
    }

    public boolean isValidUser() {
        if (null != this.user) {
            return this.user.isRegistered();
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (this.reportSn > 0) {
            json.put("reportSn", this.reportSn);
        }
        if (this.articleSn > 0) {
            json.put("articleSn", this.articleSn);
        }
        if (this.comprehensiveReportSn > 0) {
            json.put("comprehensiveReportSn", this.comprehensiveReportSn);
        }
        if (this.uid > 0) {
            json.put("uid", this.uid);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
