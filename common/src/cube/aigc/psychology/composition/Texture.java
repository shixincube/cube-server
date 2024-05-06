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

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

public class Texture implements JSONable {

    public double max;

    public double avg;

    public double squareDeviation;

    public double standardDeviation;

    public double hierarchy;

    public double density;

    public Texture() {
        this.max = 0;
        this.avg = 0;
        this.squareDeviation = 0;
        this.standardDeviation = 0;
        this.hierarchy = 0;
        this.density = 1;
    }

    public Texture(JSONObject json) {
        this.max = Double.parseDouble(json.getString("max"));
        this.avg = Double.parseDouble(json.getString("avg"));
        this.squareDeviation = Double.parseDouble(json.getString("squareDeviation"));
        this.standardDeviation = Double.parseDouble(json.getString("standardDeviation"));
        this.hierarchy = Double.parseDouble(json.getString("hierarchy"));
        this.density = Double.parseDouble(json.getString("density"));
    }

    public boolean isValid() {
        return this.max > 0 && this.squareDeviation > 0 && this.standardDeviation > 0 && this.hierarchy > 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("max", Double.toString(this.max));
        json.put("avg", Double.toString(this.avg));
        json.put("squareDeviation", Double.toString(this.squareDeviation));
        json.put("standardDeviation", Double.toString(this.standardDeviation));
        json.put("hierarchy", Double.toString(this.hierarchy));
        json.put("density", Double.toString(this.density));
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
