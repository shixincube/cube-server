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

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

/**
 * 心理学报告。
 */
public class PsychologyReport implements JSONable {

//    public final static String PHASE_PREDICT = "PREDICT";
//    public final static String PHASE_PREDICT_FAILED = "PREDICT_FAILED";
//    public final static String PHASE_INFER = "INFER";
//    public final static String PHASE_INFER_FAILED = "INFER_FAILED";
//    public final static String PHASE_FINISH = "FINISH";

    public final long sn;

    public final String token;

    public final long timestamp;

    private ReportAttribute reportAttribute;

    private FileLabel fileLabel;

    private Theme theme;

    private boolean finished = false;

    public PsychologyReport(String token, ReportAttribute reportAttribute, FileLabel fileLabel, Theme theme) {
        this.sn = Utils.generateSerialNumber();
        this.token = token;
        this.reportAttribute = reportAttribute;
        this.fileLabel = fileLabel;
        this.theme = theme;
        this.timestamp = System.currentTimeMillis();
    }

    public PsychologyReport(JSONObject json) {
        this.sn = json.getLong("sn");
        this.token = json.getString("token");
        this.reportAttribute = new ReportAttribute(json.getJSONObject("attribute"));
        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        this.theme = Theme.parse(json.getString("theme"));
        this.timestamp = json.getLong("timestamp");
        this.finished = json.getBoolean("finished");
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("token", this.token);
        json.put("attribute", this.reportAttribute.toJSON());
        json.put("fileLabel", this.fileLabel.toJSON());
        json.put("theme", this.theme.name);
        json.put("timestamp", this.timestamp);
        json.put("finished", this.finished);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
