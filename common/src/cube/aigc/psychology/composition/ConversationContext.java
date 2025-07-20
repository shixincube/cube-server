/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.guidance.AbstractGuideFlow;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话上下文。
 */
public class ConversationContext implements JSONable {

    private ConversationRelation relation;

    private AuthToken authToken;

    private FileLabel currentFile;

    private boolean currentPaintingValidity;

    private Attribute currentAttribute;

    private PaintingReport currentReport;

    private Subtask currentSubtask;

    private List<PaintingReport> reportList;

    private AbstractGuideFlow guideFlow;

    private List<GeneratingRecord> taskRecords = new ArrayList<>();

    private List<GeneratingRecord> normalRecords = new ArrayList<>();

    private boolean superAdmin;

    public ConversationContext(ConversationRelation relation, AuthToken authToken) {
        this.relation = relation;
        this.authToken = authToken;
    }

    public long getRelationId() {
        return this.relation.getId();
    }

    public AuthToken getAuthToken() {
        return this.authToken;
    }

    public void setCurrentSubtask(Subtask subtask) {
        this.currentSubtask = subtask;
    }

    public Subtask getCurrentSubtask() {
        return this.currentSubtask;
    }

    public void setCurrentAttribute(Attribute attribute) {
        this.currentAttribute = attribute;
    }

    public Attribute getCurrentAttribute() {
        return this.currentAttribute;
    }

    public void setCurrentFile(FileLabel fileLabel) {
        this.currentFile = fileLabel;
    }

    public FileLabel getCurrentFile() {
        return this.currentFile;
    }

    public void setCurrentPaintingValidity(boolean valid) {
        this.currentPaintingValidity = valid;
    }

    public boolean getCurrentPaintingValidity() {
        return this.currentPaintingValidity;
    }

    public void setCurrentReport(PaintingReport report) {
        this.currentReport = report;
    }

    public PaintingReport getCurrentReport() {
        return this.currentReport;
    }

    public void setReportList(List<PaintingReport> list) {
        this.reportList = list;
    }

    public List<PaintingReport> getReportList() {
        return this.reportList;
    }

    public void setGuideFlow(AbstractGuideFlow guideFlow) {
        this.guideFlow = guideFlow;
    }

    public AbstractGuideFlow getGuideFlow() {
        return this.guideFlow;
    }

    public void setSuperAdmin(boolean value) {
        this.superAdmin = value;
    }

    public boolean isSuperAdmin() {
        return this.superAdmin;
    }

    public void cancelAll() {
        this.currentFile = null;
        this.currentPaintingValidity = false;
        this.currentAttribute = null;
        this.currentReport = null;
        this.currentSubtask = null;
        this.taskRecords.clear();
        this.reportList = null;
        this.guideFlow = null;
    }

    public void cancelCurrentPredict() {
        this.currentSubtask = null;
        this.currentAttribute = null;
        this.currentFile = null;
        this.currentPaintingValidity = false;
    }

    public void cancelCurrentSubtask() {
        this.currentSubtask = null;
    }

    public FileLabel getRecentFile() {
        for (int i = this.normalRecords.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.normalRecords.get(i);
            if (null != record.queryFileLabels && !record.queryFileLabels.isEmpty()) {
                return record.queryFileLabels.get(0);
            }
        }
        for (int i = this.taskRecords.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.taskRecords.get(i);
            if (null != record.queryFileLabels && !record.queryFileLabels.isEmpty()) {
                return record.queryFileLabels.get(0);
            }
        }
        return null;
    }

    public void recordTask(GeneratingRecord record) {
        this.taskRecords.add(record);
    }

    public GeneratingRecord getRecentTaskRecord() {
        if (this.taskRecords.isEmpty()) {
            return null;
        }

        return this.taskRecords.get(this.taskRecords.size() - 1);
    }

    public void recordNormal(GeneratingRecord record) {
        this.normalRecords.add(record);
    }

    /**
     * 获取时间倒序的历史记录。
     *
     * @param num
     * @return
     */
    public List<GeneratingRecord> getNormalHistories(int num) {
        List<GeneratingRecord> list = new ArrayList<>();
        for (int i = this.normalRecords.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.normalRecords.get(i);
            list.add(record);
            if (list.size() >= num) {
                break;
            }
        }
        return list;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
