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
 * 知识文档进度。
 */
public class KnowledgeQAProgress implements JSONable {

    private final long sn;

    private AIGCChannel channel;

    private String unitName;

    private long start;

    private long end;

    private int progress;

    private int totalProgress;

    private int percent = 0;

    private int code = -1;

    private KnowledgeQAResult result;

    public KnowledgeQAProgress(AIGCChannel channel, String unitName) {
        this.sn = Utils.generateSerialNumber();
        this.channel = channel;
        this.unitName = unitName;
        this.start = System.currentTimeMillis();
    }

    public KnowledgeQAProgress(JSONObject json) {
        this.sn = json.getLong("sn");
        this.channel = new AIGCChannel(json.getJSONObject("channel"));
        this.unitName = json.getString("unit");
        this.start = json.getLong("start");
        this.end = json.getLong("end");
        this.percent = json.getInt("percent");
        this.code = json.getInt("code");
        if (json.has("result")) {
            this.result = new KnowledgeQAResult(json.getJSONObject("result"));
        }
    }

    public void defineTotalProgress(int value) {
        this.progress = 0;
        this.totalProgress = value;
    }

    public int updateProgress(int value) {
        if (this.totalProgress == 0) {
            return 100;
        }

        this.progress += value;
        this.percent = (int) Math.floor(((float) this.progress / (float) this.totalProgress) * 100.0);
        return this.percent;
    }

    public int getProgressPercent() {
        if (this.totalProgress == 0) {
            return 0;
        }

        return (int) Math.floor(((float) this.progress / (float) this.totalProgress) * 100.0);
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setResult(KnowledgeQAResult result) {
        this.result = result;
        this.end = System.currentTimeMillis();
    }

    public KnowledgeQAResult getResult() {
        return this.result;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("channel", this.channel.toCompactJSON());
        json.put("unit", this.unitName);
        json.put("start", this.start);
        json.put("end", this.end);
        json.put("percent", this.percent);
        json.put("code", this.code);
        if (null != this.result) {
            json.put("result", this.result.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
