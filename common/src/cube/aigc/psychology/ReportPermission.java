/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.common.JSONable;
import org.json.JSONObject;

public final class ReportPermission implements JSONable {

    public final long contactId;

    public final long reportSn;

    /**
     * 关联文件信息权限。
     */
    public boolean file = true;

    /**
     * 指标摘要权限。
     */
    public boolean indicatorSummary = false;

    /**
     * 指标详情权限。
     */
    public boolean indicatorDetails = false;

    /**
     * 人格画像权限。
     */
    public boolean personalityPortrait = false;

    /**
     * 人格特质详情权限。
     */
    public boolean personalityDetails = false;

    /**
     * 六维分析权限。
     */
    public boolean dimensionScore = false;

    /**
     * 关注等级权限。
     */
    public boolean attention = false;

    /**
     * 常模判断权限。
     */
    public boolean reference = false;

    /**
     * 推荐测试权限。
     */
    public boolean recommend = false;

    /**
     * 处置建议权限。
     */
    public boolean suggestion = false;

    /**
     * 症状因子数据权限。
     */
    public boolean symptomFactor = false;

    /**
     * 情绪因子数据权限。
     */
    public boolean affectFactor = false;

    /**
     * 人格因子数据权限。
     */
    public boolean personalityFactor = false;

    public ReportPermission(long contactId, long reportSn) {
        this.contactId = contactId;
        this.reportSn = reportSn;
    }

    public ReportPermission(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.reportSn = json.getLong("reportSn");
        this.file = json.getBoolean("file");
        this.indicatorSummary = json.getBoolean("indicatorSummary");
        this.indicatorDetails = json.getBoolean("indicatorDetails");
        this.personalityPortrait = json.getBoolean("personalityPortrait");
        this.personalityDetails = json.getBoolean("personalityDetails");
        this.dimensionScore = json.getBoolean("dimensionScore");
        this.attention = json.getBoolean("attention");
        this.reference = json.getBoolean("reference");
        this.recommend = json.getBoolean("recommend");
        this.suggestion = json.getBoolean("suggestion");
        this.symptomFactor = json.getBoolean("symptomFactor");
        this.affectFactor = json.getBoolean("affectFactor");
        this.personalityFactor = json.getBoolean("personalityFactor");
    }

    public boolean isPermissionAllowed() {
        return (this.indicatorSummary && this.indicatorDetails && this.personalityDetails);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("reportSn", this.reportSn);
        json.put("file", this.file);
        json.put("indicatorSummary", this.indicatorSummary);
        json.put("indicatorDetails", this.indicatorDetails);
        json.put("personalityPortrait", this.personalityPortrait);
        json.put("personalityDetails", this.personalityDetails);
        json.put("dimensionScore", this.dimensionScore);
        json.put("attention", this.attention);
        json.put("reference", this.reference);
        json.put("recommend", this.recommend);
        json.put("suggestion", this.suggestion);
        json.put("symptomFactor", this.symptomFactor);
        json.put("affectFactor", this.affectFactor);
        json.put("personalityFactor", this.personalityFactor);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public static ReportPermission createNormalPermissions(long contactId, long reportSn) {
        ReportPermission permission = new ReportPermission(contactId, reportSn);
        permission.file = true;
        permission.indicatorSummary = true;
        permission.indicatorDetails = true;
        permission.personalityPortrait = true;
        permission.personalityDetails = true;
        permission.dimensionScore = false;
        permission.attention = true;
        permission.reference = false;
        permission.recommend = true;
        permission.suggestion = true;
        permission.symptomFactor = true;
        permission.affectFactor = false;
        permission.personalityFactor = false;
        return permission;
    }

    public static ReportPermission createAllPermissions(long contactId, long reportSn) {
        ReportPermission permission = new ReportPermission(contactId, reportSn);
        permission.file = true;
        permission.indicatorSummary = true;
        permission.indicatorDetails = true;
        permission.personalityPortrait = true;
        permission.personalityDetails = true;
        permission.dimensionScore = true;
        permission.attention = true;
        permission.reference = true;
        permission.recommend = true;
        permission.suggestion = true;
        permission.symptomFactor = true;
        permission.affectFactor = true;
        permission.personalityFactor = true;
        return permission;
    }
}
