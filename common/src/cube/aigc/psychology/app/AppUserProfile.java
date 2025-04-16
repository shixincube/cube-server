/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.common.JSONable;
import cube.common.entity.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppUserProfile implements JSONable {

    public final long timestamp;

    public int totalPoints = 0;

    public List<Point> pointList = new ArrayList<>();

    public int numReports = 0;

    public BigFivePersonality personality;

    public AppUserProfile() {
        this.timestamp = System.currentTimeMillis();
    }

    public AppUserProfile(JSONObject json) {
        this.timestamp = json.getLong("timestamp");
        this.totalPoints = json.getInt("totalPoints");
        JSONArray array = json.getJSONArray("pointList");
        for (int i = 0; i < array.length(); ++i) {
            this.pointList.add(new Point(array.getJSONObject(i)));
        }
        this.numReports = json.getInt("numReports");
        if (json.has("personality")) {
            this.personality = new BigFivePersonality(json.getJSONObject("personality"));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("timestamp", this.timestamp);
        json.put("totalPoints", this.totalPoints);
        JSONArray array = new JSONArray();
        for (Point point : this.pointList) {
            array.put(point.toJSON());
        }
        json.put("pointList", array);
        json.put("numReports", this.numReports);
        if (null != this.personality) {
            json.put("personality", this.personality.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
