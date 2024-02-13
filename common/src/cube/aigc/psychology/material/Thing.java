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

package cube.aigc.psychology.material;

import cube.common.entity.Material;
import org.json.JSONObject;

import java.util.List;

/**
 * 素材。
 */
public abstract class Thing extends Material {

    protected Label paintingLabel;

    public Thing(Label paintingLabel) {
        this.paintingLabel = paintingLabel;
    }

    public Thing(JSONObject json) {
        super(json);
        this.paintingLabel = Label.parse(this.label);
    }

    public Label getLabel() {
        return this.paintingLabel;
    }

    public int getWidth() {
        return this.bbox.width;
    }

    public int getHeight() {
        return this.bbox.height;
    }

    protected <T> T getMaxAreaThing(List<T> list) {
        int max = 0;
        T result = null;
        for (T t : list) {
            if (t instanceof Thing) {
                Thing thing = (Thing) t;
                int area = thing.bbox.calculateArea();
                if (area > max) {
                    max = area;
                    result = t;
                }
            }
        }
        return result;
    }
}
