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
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public final long contactId;

    public final long timestamp;

    private String name;

    private Attribute attribute;

    private FileLabel fileLabel;

    private String fileCode;

    private Theme theme;

    private boolean finished = false;

    private AIGCStateCode state;

    private List<ReportParagraph> paragraphList;

    public PsychologyReport(long contactId, Attribute attribute, FileLabel fileLabel, Theme theme) {
        this.sn = Utils.generateSerialNumber();
        this.contactId = contactId;
        this.timestamp = System.currentTimeMillis();
        this.name = theme.name + "-" + Utils.gsDateFormat.format(new Date(this.timestamp));
        this.attribute = attribute;
        this.fileLabel = fileLabel;
        this.theme = theme;
        this.state = AIGCStateCode.Processing;
    }

    public PsychologyReport(long sn, long contactId, long timestamp, String name, Attribute attribute,
                            String fileCode, Theme theme) {
        this.sn = sn;
        this.contactId = contactId;
        this.timestamp = timestamp;
        this.name = name;
        this.attribute = attribute;
        this.fileCode = fileCode;
        this.theme = theme;
        this.finished = true;
        this.state = AIGCStateCode.Ok;
    }

    public PsychologyReport(JSONObject json) {
        this.sn = json.getLong("sn");
        this.contactId = json.getLong("contactId");
        this.timestamp = json.getLong("timestamp");
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        this.theme = Theme.parse(json.getString("theme"));
        this.finished = json.getBoolean("finished");
        this.state = AIGCStateCode.parse(json.getInt("state"));
        if (json.has("paragraphList")) {
            this.paragraphList = new ArrayList<>();
            JSONArray array = json.getJSONArray("paragraphList");
            for (int i = 0; i < array.length(); ++i) {
                this.paragraphList.add(new ReportParagraph(array.getJSONObject(i)));
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public Theme getTheme() {
        return this.theme;
    }

    public void setState(AIGCStateCode state) {
        this.state = state;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    public String getFileCode() {
        return (null != this.fileCode) ? this.fileCode : this.fileLabel.getFileCode();
    }

    public List<ReportParagraph> getParagraphs() {
        return this.paragraphList;
    }

    public void setParagraphs(List<ReportParagraph> list) {
        this.paragraphList = new ArrayList<>();
        this.paragraphList.addAll(list);
        this.finished = true;
    }

    public void addParagraph(ReportParagraph paragraph) {
        if (null == this.paragraphList) {
            this.paragraphList = new ArrayList<>();
        }
        this.paragraphList.add(paragraph);
    }

    public boolean isEmpty() {
        return (null == this.paragraphList || this.paragraphList.isEmpty());
    }

    public String markdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("# ").append(this.theme.name).append("报告");

        buf.append("\n\n");
        buf.append("> ").append(this.attribute.getGenderText());
        buf.append("    ").append(this.attribute.getAgeText()).append("\n");

        buf.append("> ").append(Utils.gsDateFormat.format(new Date(this.timestamp))).append("\n");

        if (null != this.paragraphList) {
            for (ReportParagraph paragraph : this.paragraphList) {
                buf.append(paragraph.markdown(true));
            }
        }

        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        json.put("markdown", this.markdown());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());
        json.put("fileLabel", this.fileLabel.toCompactJSON());
        json.put("theme", this.theme.name);
        json.put("timestamp", this.timestamp);
        json.put("finished", this.finished);
        json.put("state", this.state.code);
        if (null != this.paragraphList) {
            JSONArray array = new JSONArray();
            for (ReportParagraph paragraph : this.paragraphList) {
                array.put(paragraph.toJSON());
            }
            json.put("paragraphList", array);
        }
        return json;
    }
}
