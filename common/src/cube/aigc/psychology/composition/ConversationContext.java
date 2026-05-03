/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.guidance.AbstractGuideFlow;
import cube.aigc.memory.SubtaskMemory;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.entity.Appointment;
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

    /**
     * 仅用于实时推理。
     */
    private FileLabel currentFile;

    /**
     * 仅用于实时推理。
     */
    private boolean currentPaintingValidity;

    /**
     * 仅用于实时推理。
     */
    private Attribute currentAttribute;

    /**
     * 仅用于实时推理。
     */
    private PaintingReport currentReport;

    private Subtask subtask;

    private SubtaskMemory subtaskMemory;

    private List<PaintingReport> reportList;

    private AbstractGuideFlow guideFlow;

    private Appointment appointment;

    private String strategy;

    private List<GeneratingRecord> normalRecords = new ArrayList<>();

    private boolean superAdmin;

    public ConversationContext(ConversationRelation relation, AuthToken authToken) {
        this.relation = relation;
        this.authToken = authToken;
    }

    public ConversationRelation getRelation() {
        return this.relation;
    }

    public AuthToken getAuthToken() {
        return this.authToken;
    }

    public void activateSubtask(Subtask subtask) {
        this.subtask = subtask;
        this.subtaskMemory = new SubtaskMemory();
    }

    public void deactivateSubtask() {
        this.subtask = null;
        this.subtaskMemory = null;
    }

    public Subtask getCurrentSubtask() {
        return this.subtask;
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

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Appointment getAppointment() {
        return this.appointment;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getStrategy() {
        return this.strategy;
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
        this.subtask = null;
        this.subtaskMemory = null;
        this.reportList = null;
        this.guideFlow = null;
        this.appointment = null;
    }

    public void cancelCurrentPredict() {
        this.subtask = null;
        this.subtaskMemory = null;
        this.currentAttribute = null;
        this.currentFile = null;
        this.currentPaintingValidity = false;
    }

    public FileLabel getRecentFile() {
        for (int i = this.normalRecords.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.normalRecords.get(i);
            if (null != record.queryFileLabels && !record.queryFileLabels.isEmpty()) {
                return record.queryFileLabels.get(0);
            }
        }

        return this.subtaskMemory.getRecentFile();
    }

    public SubtaskMemory getSubtaskMemory() {
        return this.subtaskMemory;
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
