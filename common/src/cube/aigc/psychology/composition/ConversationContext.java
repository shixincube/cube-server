/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Attribute;
import cube.common.JSONable;
import cube.common.entity.ComplexContext;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 谈话上下文。
 */
public class ConversationContext implements JSONable {

    private long sn;

    private String name;

    private Attribute currentAttribute;

    private FileLabel currentFile;

    private ConversationSubtask currentSubtask;

    private List<GeneratingRecord> records;

    public ConversationContext() {
        this.records = new ArrayList<>();
    }

    public ConversationContext(JSONObject json) {
        this.records = new ArrayList<>();
        this.sn = json.getLong("sn");
        this.name = json.getString("name");
    }

    public void setCurrentSubtask(ConversationSubtask subtask) {
        this.currentSubtask = subtask;
    }

    public ConversationSubtask getCurrentSubtask() {
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

    public void clearCurrent() {
        this.currentSubtask = null;
        this.currentAttribute = null;
        this.currentFile = null;
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
        json.put("sn", this.sn);
        json.put("name", this.name);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
