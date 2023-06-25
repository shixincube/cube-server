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

    public enum Type {

        Simplex("simplex"),

        Complex("complex");

        public final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type parse(String value) {
            if (value.equalsIgnoreCase(Simplex.value)) {
                return Simplex;
            }
            else {
                return Complex;
            }
        }
    }

    public final Type type;

    private List<ComplexResource> resources;

    public ComplexContext(Type type) {
        super(Utils.generateSerialNumber());
        this.type = type;
        this.resources = new ArrayList<>();
    }

    public ComplexContext(JSONObject json) {
        super(json);
        this.type = Type.parse(json.getString("type"));
        this.resources = new ArrayList<>();

        JSONArray array = json.getJSONArray("resources");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            String subject = data.getString("subject");
            if (subject.equals(ComplexResource.Subject.Hyperlink.name())) {
                this.resources.add(new HyperlinkResource(data));
            }
            else if (subject.equals(ComplexResource.Subject.Chart.name())) {
                this.resources.add(new ChartResource(data));
            }
        }
    }

    public boolean isSimplex() {
        return this.type == Type.Simplex;
    }

    public int numResources() {
        return this.resources.size();
    }

    public boolean hasResource(ComplexResource.Subject subject) {
        for (ComplexResource res : this.resources) {
            if (res.subject == subject) {
                return true;
            }
        }

        return false;
    }

    public ComplexResource getResource() {
        return this.resources.get(0);
    }

    public List<ComplexResource> getResources() {
        return this.resources;
    }

    public void addResource(ComplexResource resource) {
        this.resources.add(resource);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("type", this.type.value);

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
