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
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识库进度。
 */
public class KnowledgeProgress implements JSONable {

    private final long sn;

    private long startTime;

    private long endTime;

    private int totalDocs;

    private int processedDocs;

    private KnowledgeDoc activatingDoc;

    private int stateCode;

    public KnowledgeProgress(int stateCode) {
        this.sn = Utils.generateSerialNumber();
        this.startTime = System.currentTimeMillis();
        this.totalDocs = 0;
        this.processedDocs = 0;
        this.stateCode = stateCode;
    }

    public KnowledgeProgress(JSONObject json) {
        this.sn = json.getLong("sn");
        this.startTime = json.getLong("startTime");
        this.endTime = json.getLong("endTime");
        this.totalDocs = json.getInt("totalDocs");
        this.processedDocs = json.getInt("processedDocs");
        if (json.has("activatingDoc")) {
            this.activatingDoc = new KnowledgeDoc(json.getJSONObject("activatingDoc"));
        }
        this.stateCode = json.getInt("stateCode");
    }

    public long getSN() {
        return this.sn;
    }

    public void setTotalDocs(int value) {
        this.totalDocs = value;
    }

    public int getProcessedDocs() {
        return this.processedDocs;
    }

    public void setProcessedDocs(int value) {
        this.processedDocs = value;
    }

    public void setActivatingDoc(KnowledgeDoc doc) {
        this.activatingDoc = doc;
    }

    public void setStateCode(int code) {
        this.stateCode = code;
    }

    public void setEndTime(long time) {
        this.endTime = time;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("startTime", this.startTime);
        json.put("endTime", this.endTime);
        json.put("totalDocs", this.totalDocs);
        json.put("processedDocs", this.processedDocs);
        if (null != this.activatingDoc) {
            json.put("activatingDoc", this.activatingDoc.toJSON());
        }
        json.put("stateCode", this.stateCode);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
