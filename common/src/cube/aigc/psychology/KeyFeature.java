/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.algorithm.Score;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KeyFeature implements JSONable {

    private String name;

    private String description;

    private List<Score> indicators;

    public KeyFeature(String name, String description) {
        this.name = name;
        this.description = description;
        this.indicators = new ArrayList<>();
    }

    public KeyFeature(JSONObject json) {
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.indicators = new ArrayList<>();
        JSONArray array = json.getJSONArray("indicators");
        for (int i = 0; i < array.length(); ++i) {
            Score score = new Score(array.getJSONObject(i));
            this.indicators.add(score);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addIndicatorScore(Score score) {
        this.indicators.add(score);
    }

    public String makePrompt(Theme theme, Attribute attribute) {
        StringBuilder buf = new StringBuilder();
        if (attribute.language.isChinese()) {
            switch (theme) {
                case Generic:
                case HouseTreePerson:
                    buf.append("在房树人心理绘画中，");
                    buf.append(this.description);
                    buf.append("绘画者为").append(attribute.getGenderText()).append("性");
                    buf.append("，年龄是").append(attribute.getAgeText()).append("。");
                    buf.append("请根据房树人理论说明一下以上表述。");
                    break;
                default:
                    break;
            }
        }
        else {
            // TODO XJW
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("description", this.description );
        JSONArray array = new JSONArray();
        for (Score score : this.indicators) {
            array.put(score.toJSON());
        }
        json.put("indicators", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
