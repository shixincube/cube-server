/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 复合会话内容。
 */
public class ComplexContext extends Entity {

    public enum RawType {

        Simplex("simplex"),

        Complex("complex");

        public final String value;

        RawType(String value) {
            this.value = value;
        }

        public static RawType parse(String value) {
            if (value.equalsIgnoreCase(Simplex.value)) {
                return Simplex;
            }
            else {
                return Complex;
            }
        }
    }

    public final RawType rawType;

    private List<ComplexResource> resources;

    public ComplexContext(RawType rawType) {
        super(Utils.generateSerialNumber());
        this.rawType = rawType;
        this.resources = new ArrayList<>();
    }

    public ComplexContext(JSONObject json) {
        super(json);
        this.rawType = RawType.parse(json.getString("raw"));
        this.resources = new ArrayList<>();

        JSONArray array = json.getJSONArray("resources");
        for (int i = 0; i < array.length(); ++i) {
            this.resources.add(new ComplexResource(array.getJSONObject(i)));
        }
    }

    public boolean isSimplex() {
        return this.rawType == RawType.Simplex;
    }

    public int numResources() {
        return this.resources.size();
    }

    public ComplexResource getResource() {
        return this.resources.get(0);
    }

    public void addResource(ComplexResource resource) {
        this.resources.add(resource);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("raw", this.rawType.value);

        JSONArray array = new JSONArray();
        for (ComplexResource resource : this.resources) {
            array.put(resource.toCompactJSON());
        }
        json.put("resources", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        return json;
    }
}
