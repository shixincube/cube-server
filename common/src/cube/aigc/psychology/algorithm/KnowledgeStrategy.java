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

import cube.aigc.psychology.Term;
import cube.aigc.psychology.Theme;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识策略。
 */
public class KnowledgeStrategy implements JSONable {

    private Term term;

    private String interpretation;

    private String advise;

    private String remark;

    private String explain;

    public KnowledgeStrategy(JSONObject json) {
        this.term = json.has("term") ?
                Term.parse(json.getString("term")) : Term.parse(json.getString("comment"));

        if (json.has("interpretation")) {
            this.interpretation = json.getString("interpretation");
        }
        if (json.has("advise")) {
            this.advise = json.getString("advise");
        }
        if (json.has("remark")) {
            this.remark = json.getString("remark");
        }
        if (json.has("explain")) {
            this.explain = json.getString("explain");
        }
    }

    public Term getTerm() {
        return this.term;
    }

    public String getInterpretation() {
        return this.interpretation;
    }

    public String getAdvise() {
        return this.advise;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getExplain() {
        return this.explain;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KnowledgeStrategy) {
            KnowledgeStrategy other = (KnowledgeStrategy) obj;
            if (other.term == this.term) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.term.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        if (null != this.interpretation) {
            json.put("interpretation", this.interpretation);
        }
        if (null != this.advise) {
            json.put("advise", this.advise);
        }
        if (null != this.remark) {
            json.put("remark", this.remark);
        }
        if (null != this.explain) {
            json.put("explain", this.explain);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("term", this.term.word);
        return json;
    }
}
