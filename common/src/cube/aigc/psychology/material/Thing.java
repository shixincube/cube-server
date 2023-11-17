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

package cube.aigc.psychology.material;

import cube.common.JSONable;
import cube.vision.BoundingBox;
import org.json.JSONObject;

import java.util.List;

/**
 * 素材。
 */
public abstract class Thing implements JSONable {

    protected Label label;

    protected BoundingBox boundingBox;

    public Thing(Label label) {
        this.label = label;
    }

    public Thing(JSONObject json) {
        this.label = Label.parse(json.getString("label"));
        this.boundingBox = new BoundingBox(json.getJSONObject("bbox"));
    }

    public Label getLabel() {
        return this.label;
    }

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public void setBoundingBox(BoundingBox bbox) {
        this.boundingBox = bbox;
    }

    public double getWidth() {
        return this.boundingBox.width;
    }

    public double getHeight() {
        return this.boundingBox.height;
    }

    protected <T> T getMaxAreaThing(List<T> list) {
        int max = 0;
        T result = null;
        for (T t : list) {
            if (t instanceof Thing) {
                Thing thing = (Thing) t;
                int area = thing.getBoundingBox().calculateArea();
                if (area > max) {
                    max = area;
                    result = t;
                }
            }
        }
        return result;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label.name);
        json.put("bbox", this.boundingBox.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label.name);
        json.put("bbox", this.boundingBox.toJSON());
        return json;
    }
}
