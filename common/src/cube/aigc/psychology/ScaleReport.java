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

import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleFactor;
import cube.aigc.psychology.composition.ScalePrompt;
import cube.aigc.psychology.composition.ScaleResult;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 心理学量表报告。
 */
public class ScaleReport extends Report {

    private Scale scale;

    private List<ScaleFactor> factors;

    private boolean finished = false;

    private long finishedTimestamp;

    private AIGCStateCode state;

    public ScaleReport(long contactId, Scale scale) {
        super(scale.getSN(), contactId, System.currentTimeMillis(), scale.getAttribute());
        this.scale = scale;
        this.factors = new ArrayList<>();

        ScaleResult result = this.scale.getResult();
        if (null != result && null != result.prompt) {
            for (ScalePrompt.Factor prompt : result.prompt.getFactors()) {
                String displayName = result.matchFactorName(prompt.factor);
                if (null == displayName) {
                    continue;
                }
                this.factors.add(new ScaleFactor(prompt.factor, displayName, prompt.score));
            }
        }

        this.state = AIGCStateCode.Processing;
    }

    public ScaleReport(long sn, long contactId, long timestamp, Attribute attribute, JSONArray factorArray) {
        super(sn, contactId, timestamp, attribute);
        this.factors = new ArrayList<>();
        this.setFactors(factorArray);
        this.state = AIGCStateCode.Ok;
        this.finished = true;
        this.finishedTimestamp = timestamp;
    }

    public ScaleReport(JSONObject json) {
        super(json);
        this.finished = json.getBoolean("finished");
        this.finishedTimestamp = json.getLong("finishedTimestamp");
        this.state = AIGCStateCode.parse(json.getInt("state"));
        this.factors = new ArrayList<>();
        JSONArray array = json.getJSONArray("factors");
        for (int i = 0; i < array.length(); ++i) {
            this.factors.add(new ScaleFactor(array.getJSONObject(i)));
        }
    }

    public List<ScaleFactor> getFactors() {
        return this.factors;
    }

    public JSONArray getFactorsAsJSONArray() {
        JSONArray array = new JSONArray();
        for (ScaleFactor factor : this.factors) {
            array.put(factor.toJSON());
        }
        return array;
    }

    public void setFactors(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            this.factors.add(new ScaleFactor(array.getJSONObject(i)));
        }
    }

    public void setFinished(boolean value) {
        this.finished = value;
        this.finishedTimestamp = System.currentTimeMillis();
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setState(AIGCStateCode state) {
        this.state = state;
    }

    public AIGCStateCode getState() {
        return this.state;
    }

    public Scale getScale() {
        return this.scale;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("finished", this.finished);
        json.put("finishedTimestamp", this.finishedTimestamp);
        json.put("state", this.state.code);

        json.put("factors", this.getFactorsAsJSONArray());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}