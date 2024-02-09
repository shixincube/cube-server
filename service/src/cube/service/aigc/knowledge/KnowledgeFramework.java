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

import cube.auth.AuthToken;
import cube.common.entity.KnowledgeBaseInfo;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.service.auth.AuthService;

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
    private ConcurrentHashMap<Long, FrameworkWrapper> frameworks;

    public KnowledgeFramework(AIGCService service, AuthService authService, AbstractModule fileStorage) {
        this.service = service;
        this.authService = authService;
        this.fileStorage = fileStorage;
        this.frameworks = new ConcurrentHashMap<>();
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

        return base;
    }

    public KnowledgeBase newKnowledgeBase() {
        return null;
    }

    public void freeBase(String domain, Long contactId) {
        FrameworkWrapper framework = this.frameworks.get(contactId);
        if (null == framework) {
            return;
        }

        this.frameworks.remove(contactId);
        framework.destroy();
    }
}
