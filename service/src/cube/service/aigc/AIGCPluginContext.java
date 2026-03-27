/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.util.Utils;
import cube.aigc.AppEvent;
import cube.aigc.TaskDescriptor;
import cube.auth.AuthToken;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.KnowledgeDocument;
import cube.plugin.PluginContext;
import cube.service.aigc.knowledge.KnowledgeBase;

import java.util.ArrayList;
import java.util.List;

/**
 * AIGC 插件上下文。
 */
public class AIGCPluginContext extends PluginContext {

    private AuthToken authToken;

    private String task;

    private List<FileLabel> fileLabelList;

    private AIGCUnit unit;

    private KnowledgeBase knowledgeBase;

    private KnowledgeDocument knowledgeDocument;

    private AppEvent appEvent;

    public AIGCPluginContext(AuthToken authToken, String task) {
        this.authToken = authToken;
        this.task = task;
    }

    public AIGCPluginContext(KnowledgeBase knowledgeBase, KnowledgeDocument knowledgeDocument) {
        this.knowledgeBase = knowledgeBase;
        this.knowledgeDocument = knowledgeDocument;
    }

    public AIGCPluginContext(AppEvent appEvent) {
        this.appEvent = appEvent;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public KnowledgeBase getKnowledgeBase() {
        return this.knowledgeBase;
    }

    public KnowledgeDocument getKnowledgeDoc() {
        return this.knowledgeDocument;
    }

    public AppEvent getAppEvent() {
        return this.appEvent;
    }

    public AuthToken getAuthToken() {
        return this.authToken;
    }

    public String getTask() {
        return this.task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public AIGCUnit getUnit() {
        return this.unit;
    }

    public void setUnit(AIGCUnit unit) {
        this.unit = unit;
    }

    public List<FileLabel> getFileLabelList() {
        return this.fileLabelList;
    }

    public void addFileLabel(FileLabel fileLabel) {
        if (null == this.fileLabelList) {
            this.fileLabelList = new ArrayList<>();
        }
        this.fileLabelList.add(fileLabel);
    }

    public void removeFileLabel(FileLabel fileLabel) {
        if (null == this.fileLabelList) {
            return;
        }
        this.fileLabelList.remove(fileLabel);
    }

    public TaskDescriptor makeTaskDescriptor() {
        if (null == this.authToken || null == this.task) {
            return null;
        }

        TaskDescriptor descriptor = new TaskDescriptor(Utils.generateSerialNumber(), this.authToken.getContactId(),
                this.authToken.getDomain(), this.authToken.getCode(), this.task, System.currentTimeMillis());
        if (null != this.fileLabelList) {
            for (FileLabel fileLabel : this.fileLabelList) {
                descriptor.addFileCode(fileLabel.getFileCode());
            }
        }
        if (null != this.unit) {
            descriptor.setUnitId(this.unit.getId());
        }

        return descriptor;
    }

    @Override
    public Object get(String name) {
        if (name.equalsIgnoreCase("appEvent")) {
            return this.appEvent;
        }
        else if (name.equalsIgnoreCase("knowledgeBase")) {
            return this.knowledgeBase;
        }
        else if (name.equalsIgnoreCase("knowledgeDoc")) {
            return this.knowledgeDocument;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
    }
}
