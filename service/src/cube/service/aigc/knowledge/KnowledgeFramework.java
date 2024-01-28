/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.aigc.knowledge;

import cube.auth.AuthToken;
import cube.common.entity.KnowledgeBaseInfo;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.service.auth.AuthService;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库框架。
 */
public class KnowledgeFramework {

    public final static String DefaultName = "document";

    public final static String DefaultDisplayName = "默认文档库";

    private AIGCService service;

    private AuthService authService;

    private AbstractModule fileStorage;

    /**
     * Key: 联系人 ID 。
     */
    private ConcurrentHashMap<Long, Framework> frameworkMap;

    public KnowledgeFramework(AIGCService service, AuthService authService, AbstractModule fileStorage) {
        this.service = service;
        this.authService = authService;
        this.fileStorage = fileStorage;
        this.frameworkMap = new ConcurrentHashMap<>();
    }

    public List<KnowledgeBaseInfo> getKnowledgeBaseInfos(long contactId) {
        Framework framework = null;
        synchronized (this) {
            framework = this.frameworkMap.get(contactId);
            if (null == framework) {
                framework = new Framework(contactId);
                this.frameworkMap.put(contactId, framework);
            }
        }

        return framework.baseInfoList;
    }

    public KnowledgeBase getKnowledgeBase(long contactId, String baseName) {
        Framework framework = null;
        synchronized (this) {
            framework = this.frameworkMap.get(contactId);
            if (null == framework) {
                framework = new Framework(contactId);
                this.frameworkMap.put(contactId, framework);
            }
        }

        KnowledgeBase base = framework.getKnowledgeBase(baseName);
        if (null == base) {
            if (KnowledgeFramework.DefaultName.equals(baseName)) {
                // 创建默认库
                AuthToken authToken = this.authService.queryAuthTokenByContactId(contactId);
                base = new KnowledgeBase(KnowledgeFramework.DefaultName,
                        this.service, this.service.getStorage(), authToken, this.fileStorage);
                framework.putKnowledgeBase(base);
                base.listKnowledgeDocs();
                base.listKnowledgeArticles();
            }
            else {
                KnowledgeBaseInfo info = framework.getKnowledgeBaseInfo(baseName);
                if (null != info) {
                    AuthToken authToken = this.authService.queryAuthTokenByContactId(contactId);
                    base = new KnowledgeBase(info.name,
                            this.service, this.service.getStorage(), authToken, this.fileStorage);
                    framework.putKnowledgeBase(base);
                    base.listKnowledgeDocs();
                    base.listKnowledgeArticles();
                }
            }
        }

        return base;
    }



    private class Framework {

        private final long contactId;

        private LinkedList<KnowledgeBaseInfo> baseInfoList;

        /**
         * Key: 知识库名。
         */
        private ConcurrentHashMap<String, KnowledgeBase> knowledgeMap;

        private Framework(long contactId) {
            this.contactId = contactId;
            this.baseInfoList = new LinkedList<>();
            this.knowledgeMap = new ConcurrentHashMap<>();
            this.refreshKnowledgeBaseInfo();
        }

        private KnowledgeBase getKnowledgeBase(String name) {
            return this.knowledgeMap.get(name);
        }

        private void putKnowledgeBase(KnowledgeBase knowledgeBase) {
            this.knowledgeMap.put(knowledgeBase.getName(), knowledgeBase);
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
            KnowledgeBaseInfo info = service.getStorage().readKnowledgeBaseInfo(this.contactId, baseName);
            if (null != info) {
                this.baseInfoList.add(info);
            }

            return info;
        }

        private void refreshKnowledgeBaseInfo() {
            synchronized (this.baseInfoList) {
                this.baseInfoList.clear();

                List<KnowledgeBaseInfo> list = service.getStorage().readKnowledgeBaseInfo(this.contactId);
                if (list.isEmpty()) {
                    // 写入默认库
                    KnowledgeBaseInfo info = new KnowledgeBaseInfo(this.contactId, DefaultName,
                            DefaultDisplayName, 0, 0, System.currentTimeMillis());
                    service.getStorage().writeKnowledgeBaseInfo(info);
                    this.baseInfoList.add(info);
                }
                else {
                    this.baseInfoList.addAll(list);
                }
            }
        }
    }
}
