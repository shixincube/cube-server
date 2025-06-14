/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 报告属性。
 */
public class Attribute implements JSONable {

    public final static int MAX_AGE = 65;

    public final static int MIN_AGE = 11;

    public final String gender;

    public final int age;

    public final boolean strict;

    public Attribute(String gender, int age, boolean strict) {
        this.gender = gender.equals("男") ? "male" : (gender.equals("女") ? "female" : gender.toLowerCase());
        this.age = age;
        this.strict = strict;
    }

    public Attribute(String gender, int age) {
        this.gender = gender;
        this.age = age;
        this.strict = false;
    }

    public Attribute(JSONObject json) {
        this.gender = json.getString("gender");
        this.age = json.getInt("age");
        this.strict = json.has("strict") && json.getBoolean("strict");
    }

    public boolean isMale() {
        return this.gender.equalsIgnoreCase("male")
                || this.gender.contains("男");
    }

    public boolean isFemale() {
        return this.gender.equalsIgnoreCase("female")
                || this.gender.contains("女");
    }

    public String getGenderText() {
        return this.isMale() ? "男" : "女";
    }

    public String getAgeText() {
        return this.age + "岁";
    }

    public double[] calcFactor() {
//        double[] data = FloatUtils.softmax(new double[] {
//                this.isMale() ? 0.9 : 0.1,
//                ((double) this.age) * 0.01
//        });
        double[] data = new double[] {
                this.isMale() ? 0.9 : 0.1,
                ((double) this.age) * 0.01
        };
        return data;
    }

    public boolean isValid() {
        return this.age > 0 && this.gender.length() > 0;
    }

    public JSONArray calcFactorToArray() {
        double[] data = this.calcFactor();
        JSONArray array = new JSONArray();
        array.put(data[0]);
        array.put(data[1]);
        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("gender", this.gender);
        json.put("age", this.age);
        json.put("strict", this.strict);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
