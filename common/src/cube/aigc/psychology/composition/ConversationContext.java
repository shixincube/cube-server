/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

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
 * 谈话上下文。
 */
public class ConversationContext implements JSONable {

    private ConversationRelation relation;

    private AuthToken authToken;

    private FileLabel currentFile;

    private boolean currentPaintingValidity;

    private Attribute currentAttribute;

    private PaintingReport currentReport;

    private Subtask currentSubtask;

    private List<GeneratingRecord> records;

    private List<PaintingReport> reportList;

    public ConversationContext(ConversationRelation relation, AuthToken authToken) {
        this.relation = relation;
        this.authToken = authToken;
        this.records = new ArrayList<>();
    }

    public ConversationContext(JSONObject json) {
        this.records = new ArrayList<>();
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

    public void clearAll() {
        this.currentFile = null;
        this.currentPaintingValidity = false;
        this.currentAttribute = null;
        this.currentReport = null;
        this.currentSubtask = null;
        this.records.clear();
        this.reportList = null;
    }

    public void clearCurrentPredict() {
        this.currentSubtask = null;
        this.currentAttribute = null;
        this.currentFile = null;
        this.currentPaintingValidity = false;
    }

    public FileLabel getRecentFile() {
        for (int i = this.records.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.records.get(i);
            if (null != record.queryFileLabels && !record.queryFileLabels.isEmpty()) {
                return record.queryFileLabels.get(0);
            }
        }
        return null;
    }

    public void record(GeneratingRecord record) {
        this.records.add(record);
    }

    public GeneratingRecord getRecent() {
        if (this.records.isEmpty()) {
            return null;
        }

        return this.records.get(this.records.size() - 1);
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
