/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.knowledge;

import cube.common.entity.KnowledgeBaseInfo;
import cube.service.aigc.AIGCService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FrameworkWrapper {

    private final long contactId;

    private final AIGCService service;

    private final LinkedList<KnowledgeBaseInfo> baseInfoList;

    /**
     * Key: 知识库名。
     */
    private ConcurrentHashMap<String, KnowledgeBase> knowledgeMap;

    public FrameworkWrapper(long contactId, AIGCService service) {
        this.contactId = contactId;
        this.service = service;
        this.baseInfoList = new LinkedList<>();
        this.knowledgeMap = new ConcurrentHashMap<>();
        this.refreshKnowledgeBaseInfo();
    }

    public List<KnowledgeBaseInfo> getBaseInfos() {
        synchronized (this.baseInfoList) {
            this.baseInfoList.clear();
            List<KnowledgeBaseInfo> list = this.service.getStorage().readKnowledgeBaseInfo(this.contactId);
            this.baseInfoList.addAll(list);
            return this.baseInfoList;
        }
    }

    public KnowledgeBase getKnowledgeBase(String name) {
        return this.knowledgeMap.get(name);
    }

    public List<KnowledgeBase> getKnowledgeBaseByCategory(String category) {
        List<KnowledgeBase> list = new ArrayList<>();

        synchronized (this.baseInfoList) {
            for (KnowledgeBaseInfo info : this.baseInfoList) {
                if (null != info.category && info.category.equals(category)) {
                    KnowledgeBase base = this.getKnowledgeBase(info.name);
                    if (null != base) {
                        list.add(base);
                    }
                }
            }
        }

        return list;
    }

    public void putKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeMap.put(knowledgeBase.getName(), knowledgeBase);
    }

    public KnowledgeBase removeKnowledgeBase(String name) {
        KnowledgeBase base = this.knowledgeMap.remove(name);
        synchronized (this.baseInfoList) {
            this.baseInfoList.removeIf(info -> info.name.equals(name));
        }
        return base;
    }

    public KnowledgeBaseInfo getKnowledgeBaseInfo(String baseName) {
        synchronized (this.baseInfoList) {
            for (KnowledgeBaseInfo info : this.baseInfoList) {
                if (info.name.equals(baseName)) {
                    return info;
                }
            }
        }

        // 查询已创建的库
        KnowledgeBaseInfo info = this.service.getStorage().readKnowledgeBaseInfo(this.contactId, baseName);
        if (null != info) {
            this.baseInfoList.add(info);
        }

        return info;
    }

    public List<KnowledgeBaseInfo> getKnowledgeBaseInfoByCategory(String category) {
        List<KnowledgeBaseInfo> infos = new ArrayList<>();

        synchronized (this.baseInfoList) {
            for (KnowledgeBaseInfo info : this.baseInfoList) {
                if (null != info.category && info.category.equals(category)) {
                    infos.add(info);
                }
            }
        }
        if (!infos.isEmpty()) {
            return infos;
        }

        infos = this.service.getStorage().readKnowledgeBaseInfoByCategory(this.contactId, category);
        for (KnowledgeBaseInfo info : infos) {
            if (!this.baseInfoList.contains(info)) {
                this.baseInfoList.add(info);
            }
        }

        return infos;
    }

    public void refreshKnowledgeBaseInfo() {
        synchronized (this.baseInfoList) {
            this.baseInfoList.clear();

            List<KnowledgeBaseInfo> list = this.service.getStorage().readKnowledgeBaseInfo(this.contactId);
            if (list.isEmpty()) {
                // 写入默认库
                KnowledgeBaseInfo info = new KnowledgeBaseInfo(this.contactId, KnowledgeFramework.DefaultName,
                        KnowledgeFramework.DefaultDisplayName, null, 0, 0, System.currentTimeMillis());
                service.getStorage().writeKnowledgeBaseInfo(info);
                this.baseInfoList.add(info);
            }
            else {
                this.baseInfoList.addAll(list);
            }
        }
    }

    public void destroy() {
        this.baseInfoList.clear();
        this.knowledgeMap.clear();
    }
}
