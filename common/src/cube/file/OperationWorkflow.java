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

package cube.file;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件处理流。
 */
public class OperationWorkflow implements JSONable {

    private long sn;

    private String domain;

    private String sourceFileCode;

    private Long contactId;

    private long clientId;

    private List<OperationWork> workList;

    private String resultFilename;

    private FileLabel resultFileLabel;

    private boolean deleteProcessedFile = true;

    public OperationWorkflow() {
        this.sn = Utils.generateSerialNumber();
        this.workList = new LinkedList<>();
    }

    public OperationWorkflow(JSONObject json) {
        this.sn = json.getLong("sn");
        this.domain = json.getString("domain");
        this.sourceFileCode = json.getString("source");

        if (json.has("contactId")) {
            this.contactId = json.getLong("contactId");
        }

        if (json.has("clientId")) {
            this.clientId = json.getLong("clientId");
        }

        this.workList = new LinkedList<>();
        JSONArray works = json.getJSONArray("works");
        for (int i = 0; i < works.length(); ++i) {
            JSONObject workJson = works.getJSONObject(i);
            this.workList.add(new OperationWork(workJson));
        }

        if (json.has("resultFilename")) {
            this.resultFilename = json.getString("resultFilename");
        }

        if (json.has("resultFileLabel")) {
            this.resultFileLabel = new FileLabel(json.getJSONObject("resultFileLabel"));
        }
    }

    public long getSN() {
        return this.sn;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getContactId() {
        return this.contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public long getClientId() {
        return this.clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getSourceFileCode() {
        return this.sourceFileCode;
    }

    public void setSourceFileCode(String fileCode) {
        this.sourceFileCode = fileCode;
    }

    /**
     * 添加工作节点。
     * @param operationWork
     */
    public void append(OperationWork operationWork) {
        this.workList.add(operationWork);
    }

    /**
     * 移除工作流。
     * @param operationWork
     */
    public void remove(OperationWork operationWork) {
        this.workList.remove(operationWork);
    }

    public List<OperationWork> getWorkList() {
        return new ArrayList<>(this.workList);
    }

    public OperationWork getFirstWork() {
        return this.workList.get(0);
    }

    public void setResultFilename(String filename) {
        this.resultFilename = filename;
    }

    public String getResultFilename() {
        return this.resultFilename;
    }

    public void setResultFileLabel(FileLabel fileLabel) {
        this.resultFileLabel = fileLabel;
    }

    public FileLabel getResultFileLabel() {
        return this.resultFileLabel;
    }

    public boolean isDeleteProcessedFile() {
        return this.deleteProcessedFile;
    }

    public void setDeleteProcessedFile(boolean value) {
        this.deleteProcessedFile = value;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        JSONArray works = new JSONArray();
        for (OperationWork work : this.workList) {
            works.put(work.toJSON());
        }
        json.put("works", works);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("domain", this.domain);
        json.put("source", this.sourceFileCode);

        if (null != this.contactId) {
            json.put("contactId", this.contactId.longValue());
        }

        if (this.clientId > 0) {
            json.put("clientId", this.clientId);
        }

        if (null != this.resultFilename) {
            json.put("resultFilename", this.resultFilename);
        }

        if (null != this.resultFileLabel) {
            json.put("resultFileLabel", this.resultFileLabel.toCompactJSON());
        }

        return json;
    }
}
