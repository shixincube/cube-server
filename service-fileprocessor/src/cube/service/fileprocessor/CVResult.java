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

package cube.service.fileprocessor;

import cube.common.JSONable;
import cube.common.entity.BoundingBox;
import cube.common.entity.DetectedObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * CV 处理结果。
 */
public class CVResult implements JSONable {

    private String fileName;

    private List<DetectedObject> detectedObjects;

    public CVResult() {
    }

    public CVResult(String fileName) {
        this.fileName = fileName;
    }

    public void set(CVResult result) {
        this.fileName = result.fileName;
        this.detectedObjects = result.detectedObjects;
    }

    public List<DetectedObject> getDetectedObjects() {
        return this.detectedObjects;
    }

    public void setDetectedObjects(JSONArray array) {
        this.detectedObjects = new ArrayList<>();

        for (int i = 0; i < array.length(); ++i) {
            JSONObject json = array.getJSONObject(i);
            DetectedObject obj = new DetectedObject(json.getString("class"),
                    json.getDouble("probability"),
                    new BoundingBox(json.getJSONObject("bound")));
            this.detectedObjects.add(obj);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileName", this.fileName);

        JSONArray objects = new JSONArray();
        for (DetectedObject object : this.detectedObjects) {
            objects.put(object.toJSON());
        }
        json.put("detectedObjects", objects);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
