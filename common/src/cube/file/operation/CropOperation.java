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

package cube.file.operation;

import cube.file.ImageOperation;
import cube.vision.Rectangle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 剪裁图像。
 */
public class CropOperation extends ImageOperation {

    public final static String Operation = "Crop";

    private List<Rectangle> cropRectList;

    public CropOperation() {
        super();
        this.cropRectList = new ArrayList<>();
    }

    public CropOperation(JSONObject json) {
        super();
        this.cropRectList = new ArrayList<>();
        JSONArray rects = json.getJSONArray("rects");
        for (int i = 0; i < rects.length(); ++i) {
            this.cropRectList.add(new Rectangle(rects.getJSONObject(i)));
        }
    }

    public void addCropRect(Rectangle rect) {
        this.cropRectList.add(rect);
    }

    public List<Rectangle> getCropRects() {
        return this.cropRectList;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONArray rects = new JSONArray();
        for (Rectangle rect : this.cropRectList) {
            rects.put(rect.toJSON());
        }
        json.put("rects", rects);

        return json;
    }
}
