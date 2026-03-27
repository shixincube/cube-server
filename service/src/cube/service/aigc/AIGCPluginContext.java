/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cube.aigc.AppEvent;
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

    public void setTask(String task) {
        this.task = task;
    }

    public void setUnit(AIGCUnit unit) {
        this.unit = unit;
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
