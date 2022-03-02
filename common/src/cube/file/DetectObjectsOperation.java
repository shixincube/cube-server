/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.file;

import cube.common.entity.DetectedObject;
import org.json.JSONObject;

import java.util.List;

/**
 * 检测图像里的对象操作。
 */
public class DetectObjectsOperation extends ImageOperation {

    public final static String Operation = "detectObject";

    private List<DetectedObject> detectedObjectList;

    public DetectObjectsOperation() {
        super();
    }

    public DetectObjectsOperation(JSONObject json) {

    }

    public void setDetectedObjects(List<DetectedObject> detectedObjects) {
        this.detectedObjectList = detectedObjects;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        return json;
    }
}
