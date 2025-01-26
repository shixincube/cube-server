/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件处理流。
 */
public class OperationWorkflow implements JSONable {

    /**
     * 序号。
     */
    private long sn;

    /**
     * 域名称。
     */
    private String domain;

    /**
     * 源文件的文件码。
     */
    private String sourceFileCode;

    /**
     * 源本地文件。
     */
    private File sourceFile;

    /**
     * 联系人 ID 。
     */
    private Long contactId;

    /**
     * 服务器客户端的 ID 。
     */
    private long clientId;

    /**
     * 操作工作列表。
     */
    private List<OperationWork> workList;

    /**
     * 结果文件名。
     */
    private String resultFilename;

    /**
     * 结果文件的文件标签。
     */
    private FileLabel resultFileLabel;

    /**
     * 是否删除过程文件。
     */
    private boolean deleteProcessedFile = true;

    public OperationWorkflow() {
        this.sn = Utils.generateSerialNumber();
        this.workList = new LinkedList<>();
    }

    public OperationWorkflow(File file) {
        this.sn = Utils.generateSerialNumber();
        this.workList = new LinkedList<>();
        this.sourceFile = file;
    }

    public OperationWorkflow(JSONObject json) {
        this.sn = json.getLong("sn");
        this.domain = json.getString("domain");

        if (json.has("source")) {
            this.sourceFileCode = json.getString("source");
        }

        if (json.has("sourceFile")) {
            this.sourceFile = new File(json.getString("sourceFile"));
        }

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

    public File getSourceFile() {
        return this.sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
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

        if (null != this.sourceFileCode) {
            json.put("source", this.sourceFileCode);
        }

        if (null != this.sourceFile) {
            json.put("sourceFile", this.sourceFile.getAbsolutePath());
        }

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
