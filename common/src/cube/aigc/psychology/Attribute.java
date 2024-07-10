/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.aigc.psychology;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 报告属性。
 */
public class Attribute implements JSONable {

    public final static int MAX_AGE = 65;

    public final static int MIN_AGE = 12;

    public final String gender;

    public final int age;

    public final boolean strict;

    public Attribute(String gender, int age, boolean strict) {
        this.gender = gender;
        this.age = age;
        this.strict = strict;
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
