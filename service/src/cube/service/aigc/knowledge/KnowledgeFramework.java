/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.KnowledgeBaseInfo;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.service.auth.AuthService;
import cube.util.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Long, FrameworkWrapper> frameworks;

    public KnowledgeFramework(AIGCService service, AuthService authService, AbstractModule fileStorage) {
        this.service = service;
        this.authService = authService;
        this.fileStorage = fileStorage;
        this.frameworks = new HashMap<>();
    }

    public List<KnowledgeBaseInfo> getKnowledgeBaseInfos(long contactId) {
        FrameworkWrapper framework = null;

        synchronized (this) {
            framework = this.frameworks.get(contactId);
            if (null == framework) {
                framework = new FrameworkWrapper(contactId, this.service);
                this.frameworks.put(contactId, framework);
            }
        }

        return framework.getBaseInfos();
    }

    public KnowledgeBase getKnowledgeBase(long contactId, String baseName) {
        FrameworkWrapper framework = null;

        synchronized (this) {
            framework = this.frameworks.get(contactId);
            if (null == framework) {
                framework = new FrameworkWrapper(contactId, this.service);
                this.frameworks.put(contactId, framework);
            }

            KnowledgeBase base = framework.getKnowledgeBase(baseName);
            if (null == base) {
                KnowledgeBaseInfo info = framework.getKnowledgeBaseInfo(baseName);
                if (null != info) {
                    AuthToken authToken = this.authService.queryAuthTokenByContactId(contactId);
                    base = new KnowledgeBase(info,
                            this.service, this.service.getStorage(), authToken, this.fileStorage);
                    framework.putKnowledgeBase(base);
                    base.listKnowledgeDocs();
                    base.listKnowledgeArticles();
                }
            }

            // 判断是否是默认库
            if (null == base && KnowledgeFramework.DefaultName.equals(baseName)) {
                // 创建默认库
                AuthToken authToken = this.authService.queryAuthTokenByContactId(contactId);
                // 新库
                KnowledgeBaseInfo info = this.newKnowledgeBase(authToken.getCode(), KnowledgeFramework.DefaultName,
                        KnowledgeFramework.DefaultDisplayName, KnowledgeFramework.DefaultDisplayName);
                if (null != info) {
                    base = new KnowledgeBase(info,
                            this.service, this.service.getStorage(), authToken, this.fileStorage);
                    framework.putKnowledgeBase(base);
                    base.listKnowledgeDocs();
                    base.listKnowledgeArticles();
                }
            }

            return base;
        }
    }

    public List<KnowledgeBase> getKnowledgeBaseByCategory(long contactId, String category) {
        FrameworkWrapper framework = null;
        synchronized (this) {
            framework = this.frameworks.get(contactId);
            if (null == framework) {
                framework = new FrameworkWrapper(contactId, this.service);
                this.frameworks.put(contactId, framework);
            }

            List<KnowledgeBase> baseList = framework.getKnowledgeBaseByCategory(category);
            if (baseList.isEmpty()) {
                List<KnowledgeBaseInfo> infoList = framework.getKnowledgeBaseInfoByCategory(category);
                AuthToken authToken = this.authService.queryAuthTokenByContactId(contactId);

                for (KnowledgeBaseInfo info : infoList) {
                    KnowledgeBase base = new KnowledgeBase(info,
                            this.service, this.service.getStorage(), authToken, this.fileStorage);
                    framework.putKnowledgeBase(base);
                    base.listKnowledgeDocs();
                    base.listKnowledgeArticles();
                    baseList.add(base);
                }
            }
            return baseList;
        }
    }

    public KnowledgeBaseInfo newKnowledgeBase(String token, String name, String displayName, String category) {
        if (TextUtils.isChineseWord(name)) {
            Logger.w(this.getClass(), "#newKnowledgeBase - Knowledge base name is chinese: " + name);
            return null;
        }

        AuthToken authToken = this.service.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#newKnowledgeBase - " + name + " - Token error: " + token);
            return null;
        }

        long contactId = authToken.getContactId();

        KnowledgeBaseInfo info = this.service.getStorage().readKnowledgeBaseInfo(contactId, name);
        if (null != info) {
            Logger.w(this.getClass(), "#newKnowledgeBase - Knowledge base already exist: " + name);
            return null;
        }

        info = new KnowledgeBaseInfo(contactId, name, displayName, category, 0, 0, System.currentTimeMillis());
        // 写入信息
        if (this.service.getStorage().writeKnowledgeBaseInfo(info)) {
            // 获取实例
            if (null != this.getKnowledgeBase(contactId, name)) {
                return info;
            }
        }

        return null;
    }

    public KnowledgeBaseInfo deleteKnowledgeBase(String token, String name) {
        if (DefaultName.equals(name)) {
            Logger.w(this.getClass(), "#deleteKnowledgeBase - Default knowledge base can NOT delete: " + name);
            return null;
        }

        AuthToken authToken = this.service.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#deleteKnowledgeBase - " + name + " - Token error: " + token);
            return null;
        }

        long contactId = authToken.getContactId();

        KnowledgeBase base = this.getKnowledgeBase(contactId, name);
        if (null == base) {
            Logger.w(this.getClass(), "#deleteKnowledgeBase - Knowledge base is null: " + name);
            return null;
        }

        // 删除库的所有数据
        base.destroy();

        synchronized (this) {
            KnowledgeBaseInfo info = null;
            FrameworkWrapper framework = this.frameworks.get(contactId);
            if (null != framework) {
                info = framework.getKnowledgeBaseInfo(name);
                framework.removeKnowledgeBase(name);
            }

            this.service.getStorage().deleteKnowledgeBaseInfo(contactId, name);

            return info;
        }
    }

    public KnowledgeBaseInfo updateKnowledgeBase(String token, KnowledgeBaseInfo info) {
        if (DefaultName.equals(info.name)) {
            Logger.w(this.getClass(), "#updateKnowledgeBase - Default knowledge base can NOT update: " + info.name);
            return null;
        }

        AuthToken authToken = this.service.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#updateKnowledgeBase - Token error: " + token);
            return null;
        }

        long contactId = authToken.getContactId();

        if (this.service.getStorage().updateKnowledgeBaseInfo(contactId, info)) {
            KnowledgeBase base = this.getKnowledgeBase(contactId, info.name);
            if (null != base) {
                synchronized (this) {
                    FrameworkWrapper framework = this.frameworks.get(contactId);
                    if (null != framework) {
                        framework.refreshKnowledgeBaseInfo();
                        return framework.getKnowledgeBaseInfo(info.name);
                    }
                }
            }
        }

        return null;
    }

    public void freeBase(String domain, Long contactId) {
        synchronized (this) {
            FrameworkWrapper framework = this.frameworks.get(contactId);
            if (null == framework) {
                return;
            }

            this.frameworks.remove(contactId);
            framework.destroy();
        }
    }
}
