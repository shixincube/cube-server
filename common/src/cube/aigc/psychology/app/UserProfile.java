/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.common.JSONable;
import cube.common.entity.Membership;
import cube.common.entity.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserProfile implements JSONable {

    public final long timestamp;

    public Membership membership;

    public int totalPoints = 0;

    public List<Point> pointList = new ArrayList<>();

    public int permissibleReports = 0;

    public int usagePerMonth = 0;

    public BigFivePersonality personality;

    public UserProfile() {
        this.timestamp = System.currentTimeMillis();
    }

    public UserProfile(JSONObject json) {
        this.timestamp = json.getLong("timestamp");
        if (json.has("membership")) {
            this.membership = new Membership(json.getJSONObject("membership"));
        }
        this.totalPoints = json.getInt("totalPoints");
        JSONArray array = json.getJSONArray("pointList");
        for (int i = 0; i < array.length(); ++i) {
            this.pointList.add(new Point(array.getJSONObject(i)));
        }
        this.permissibleReports = json.getInt("permissibleReports");
        this.usagePerMonth = json.getInt("usagePerMonth");

        if (json.has("personality")) {
            this.personality = new BigFivePersonality(json.getJSONObject("personality"));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("timestamp", this.timestamp);
        if (null != this.membership) {
            json.put("membership", this.membership.toJSON());
        }
        json.put("totalPoints", this.totalPoints);
        JSONArray array = new JSONArray();
        for (Point point : this.pointList) {
            array.put(point.toJSON());
        }
        json.put("pointList", array);
        json.put("permissibleReports", this.permissibleReports);
        json.put("usagePerMonth", this.usagePerMonth);

        if (null != this.personality) {
            json.put("personality", this.personality.toCompactJSON());
        }

        // 旧版本兼容 1.0.4 及之前的版本
        json.put("numReports", this.permissibleReports);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
