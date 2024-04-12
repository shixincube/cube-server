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

package cube.aigc.psychology.algorithm;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 表征含义。
 */
public class Representation implements JSONable {

    public KnowledgeStrategy knowledgeStrategy;

    public int positiveCorrelation = 0;

    public int negativeCorrelation = 0;

    public String description = "";

    public Representation(KnowledgeStrategy knowledgeStrategy) {
        this.knowledgeStrategy = knowledgeStrategy;
    }

    public Representation(JSONObject json) {
        this.knowledgeStrategy = new KnowledgeStrategy(json.getJSONObject("knowledgeStrategy"));
        this.positiveCorrelation = json.getInt("positiveCorrelation");
        this.negativeCorrelation = json.getInt("negativeCorrelation");
        this.description = json.getString("description");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Representation) {
            Representation other = (Representation) obj;
            if (other.knowledgeStrategy.getComment() == this.knowledgeStrategy.getComment()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.knowledgeStrategy.getComment().hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("knowledgeStrategy", this.knowledgeStrategy.toJSON());
        json.put("positiveCorrelation", this.positiveCorrelation);
        json.put("negativeCorrelation", this.negativeCorrelation);
        json.put("description", this.description);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("knowledgeStrategy", this.knowledgeStrategy.toCompactJSON());
        json.put("positiveCorrelation", this.positiveCorrelation);
        json.put("negativeCorrelation", this.negativeCorrelation);
        json.put("description", this.description);
        return json;
    }
}
