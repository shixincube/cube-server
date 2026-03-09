/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.common.JSONable;
import cube.util.ConfigUtils;
import cube.util.Gender;
import org.json.JSONObject;

/**
 * 客户。
 */
public class Customer implements JSONable {

    /**
     * 正常状态。
     */
    public final static int STATE_NORMAL = 0;

    /**
     * 已删除。
     */
    public final static int STATE_DELETE = 1;

    public long id;

    public String name;

    public Gender gender;

    public int age;

    public String mobile;

    public String comment;

    public long timestamp;

    public int state = STATE_NORMAL;

    public HexagonDimensionScore hexagonScore;

    public BigFivePersonality personality;

    public Customer(String name, Gender gender, int age, String mobile, String comment, long timestamp) {
        this.id = ConfigUtils.generateSerialNumber();
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.mobile = mobile;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public Customer(long id, String name, Gender gender, int age, String mobile, String comment, long timestamp,
                    int state) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.mobile = mobile;
        this.comment = comment;
        this.timestamp = timestamp;
        this.state = state;
    }

    public Customer(JSONObject json) {
        this.id = json.getLong("id");
        this.name = json.getString("name");
        this.gender = Gender.parse(json.getString("gender"));
        this.age = json.getInt("age");
        this.mobile = json.getString("mobile");
        this.comment = json.has("comment") ? json.getString("comment") : null;
        this.timestamp = json.has("timestamp") ? json.getLong("timestamp") : System.currentTimeMillis();
        this.state = json.has("state") ? json.getInt("state") : STATE_NORMAL;
        this.hexagonScore = json.has("hexagonScore") ?
                new HexagonDimensionScore(json.getJSONObject("hexagonScore")) : null;
        this.personality = json.has("personality") ?
                new BigFivePersonality(json.getJSONObject("personality")) : null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("name", this.name);
        json.put("gender", this.gender.name);
        json.put("age", this.age);
        json.put("mobile", this.mobile);
        json.put("timestamp", this.timestamp);
        if (null != this.comment) {
            json.put("comment", this.comment);
        }
        if (null != this.hexagonScore) {
            json.put("hexagonScore", this.hexagonScore.toJSON());
        }
        if (null != this.personality) {
            json.put("personality", this.personality.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
