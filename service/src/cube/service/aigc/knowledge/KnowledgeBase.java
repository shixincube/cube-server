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

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.service.aigc.AIGCStorage;
import cube.service.aigc.listener.*;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 知识库操作。
 */
public class KnowledgeBase {

    public final static String EMPTY_BASE_ANSWER = "您的知识库里没有配置文档，您可以先向知识库里导入文档，再向我提问。";

    private String name;

    private String description;

    private AIGCService service;

    private AIGCStorage storage;

    private AuthToken authToken;

    private AbstractModule fileStorage;

    private KnowledgeResource resource;

    private KnowledgeScope scope;

    private AtomicBoolean lock;

    private ResetKnowledgeProgress resetProgress;

    public KnowledgeBase(String name, String description, AIGCService service, AIGCStorage storage,
                         AuthToken authToken, AbstractModule fileStorage) {
        this.name = name;
        this.description = description;
        this.service = service;
        this.storage = storage;
        this.authToken = authToken;
        this.fileStorage = fileStorage;
        this.resource = new KnowledgeResource();
        this.scope = getProfile().scope;
        this.lock = new AtomicBoolean(false);
    }

    public void init() {
        this.listKnowledgeDocs();
        this.listKnowledgeArticles();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public AuthToken getAuthToken() {
        return this.authToken;
    }

    /**
     * 获取知识库侧写。
     *
     * @return
     */
    public KnowledgeProfile getProfile() {
        KnowledgeProfile profile = this.storage.readKnowledgeProfile(this.authToken.getContactId());
        if (null == profile) {
            // 没有记录，返回不可用的侧写
            profile = new KnowledgeProfile(0, this.authToken.getContactId(), this.authToken.getDomain(),
                    KnowledgeProfile.STATE_FORBIDDEN, 0, KnowledgeScope.Private);
        }
        return profile;
    }

    /**
     * 更新知识库侧写。
     *
     * @param state
     * @param maxSize
     * @param scope
     * @return
     */
    public KnowledgeProfile updateProfile(int state, long maxSize, KnowledgeScope scope) {
        KnowledgeProfile profile = this.storage.updateKnowledgeProfile(this.authToken.getContactId(),
                this.authToken.getDomain(), state, maxSize, scope);
        this.scope = profile.scope;
        this.listKnowledgeDocs();
        this.listKnowledgeArticles();
        return profile;
    }

    /**
     * 获取装载的知识库文档。
     *
     * @return
     */
    public List<KnowledgeDoc> listKnowledgeDocs() {
        synchronized (this) {
            if (null != this.resource.docList && System.currentTimeMillis() - this.resource.listDocTime < 30 * 1000) {
                Logger.d(this.getClass(), "#listKnowledgeDocs - Read from memory");
                return this.resource.docList;
            }

            List<KnowledgeDoc> list = (KnowledgeScope.Private == this.scope) ?
                    this.storage.readKnowledgeDocList(this.authToken.getDomain(), this.authToken.getContactId())
                    : this.storage.readKnowledgeDocList(this.authToken.getDomain());

            Iterator<KnowledgeDoc> iter = list.iterator();
            while (iter.hasNext()) {
                KnowledgeDoc doc = iter.next();
                GetFile getFile = new GetFile(this.authToken.getDomain(), doc.fileCode);
                JSONObject fileLabelJson = this.fileStorage.notify(getFile);
                if (null == fileLabelJson) {
                    // 文件已被删除
                    iter.remove();
                    this.storage.deleteKnowledgeDoc(doc.fileCode);
                    continue;
                }

                doc.fileLabel = new FileLabel(fileLabelJson);
            }

            this.resource.docList = list;
            this.resource.listDocTime = System.currentTimeMillis();

            return list;
        }
    }

    public KnowledgeDoc getKnowledgeDocByFileCode(String fileCode) {
        KnowledgeDoc doc = this.storage.readKnowledgeDoc(fileCode);
        if (null != doc) {
            GetFile getFile = new GetFile(this.authToken.getDomain(), doc.fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null != fileLabelJson) {
                doc.fileLabel = new FileLabel(fileLabelJson);
            }
        }
        return doc;
    }

    public KnowledgeDoc importKnowledgeDoc(String fileCode) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#importKnowledgeDoc - Store locked: " + fileCode);
            return null;
        }
        this.lock.set(true);

        KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
        if (null == doc) {
            // 创建新文档
            doc = new KnowledgeDoc(Utils.generateSerialNumber(), this.authToken.getDomain(),
                    this.authToken.getContactId(), fileCode, false, -1, this.scope);
            this.storage.writeKnowledgeDoc(doc);
        }

        if (null == doc.fileLabel) {
            GetFile getFile = new GetFile(this.authToken.getDomain(), fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null == fileLabelJson) {
                this.storage.deleteKnowledgeDoc(fileCode);
                Logger.e(this.getClass(), "#importKnowledgeDoc - Not find file: " + fileCode);
                this.lock.set(false);
                return null;
            }

            FileLabel fileLabel = new FileLabel(fileLabelJson);
            doc.fileLabel = fileLabel;
        }

        KnowledgeDoc activatedDoc = doc;

        if (!doc.activated) {
            if (!this.resource.checkUnit()) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Not find Knowledge unit");
                this.lock.set(false);
                return null;
            }

            activatedDoc = this.activateKnowledgeDoc(doc);
            if (null == activatedDoc) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Unit return error");
                this.storage.deleteKnowledgeDoc(fileCode);
                this.lock.set(false);
                return null;
            }
        }

        // 追加文档
        this.resource.appendDoc(activatedDoc);

        Logger.d(this.getClass(), "#importKnowledgeDoc - file code: " + fileCode);
        this.lock.set(false);
        return activatedDoc;
    }

    public KnowledgeDoc removeKnowledgeDoc(String fileCode) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#removeKnowledgeDoc - Store locked: " + fileCode);
            return null;
        }
        this.lock.set(true);

        if (!this.resource.checkUnit()) {
            Logger.e(this.getClass(), "#removeKnowledgeDoc - No unit: " + fileCode);
            this.lock.set(false);
            return null;
        }

        KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
        if (null != doc) {
            this.storage.deleteKnowledgeDoc(fileCode);

            this.resource.removeDoc(doc);

            if (doc.activated) {
                if (this.resource.checkUnit()) {
                    KnowledgeDoc deactivatedDoc = this.deactivateKnowledgeDoc(doc);
                    doc = deactivatedDoc;
                }
            }
        }
        else {
            this.resource.removeDoc(fileCode);
        }

        this.lock.set(false);
        return doc;
    }

    /**
     * 激活指定知识库文档。
     *
     * @param doc
     * @return 返回已激活的文档。
     */
    private KnowledgeDoc activateKnowledgeDoc(KnowledgeDoc doc) {
        Packet packet = new Packet(AIGCAction.ActivateKnowledgeDoc.name, doc.toJSON());
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect(), 3 * 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#activateKnowledgeDoc - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#activateKnowledgeDoc - Unit return error: " + state);
            return null;
        }

        // 更新文档
        KnowledgeDoc activatedDoc = new KnowledgeDoc(Packet.extractDataPayload(response));
        if (activatedDoc.numSegments <= 0) {
            Logger.w(this.getClass(), "#activateKnowledgeDoc - Num of segments error: " + activatedDoc.numSegments);
            return null;
        }

        this.storage.updateKnowledgeDoc(activatedDoc);
        return activatedDoc;
    }

    /**
     * 解除指定的知识库文档。
     *
     * @param doc
     * @return
     */
    private KnowledgeDoc deactivateKnowledgeDoc(KnowledgeDoc doc) {
        JSONObject payload = new JSONObject();
        payload.put("doc", doc.toJSON());

        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeDoc.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#deactivateKnowledgeDoc - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deactivateKnowledgeDoc - Unit return error: " + state);
            return null;
        }

        KnowledgeDoc deactivatedDoc = new KnowledgeDoc(Packet.extractDataPayload(response));
        this.storage.updateKnowledgeDoc(deactivatedDoc);
        return deactivatedDoc;
    }

    /**
     * 返回重置进度。
     *
     * @return
     */
    public ResetKnowledgeProgress getResetProgress() {
        return this.resetProgress;
    }

    /**
     * 重置知识仓库。
     *
     * @param backup
     * @param listener
     * @return
     */
    public synchronized ResetKnowledgeProgress resetKnowledgeStore(boolean backup,
                                                                   ResetKnowledgeStoreListener listener) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#resetKnowledgeStore - Store locked: " + this.authToken.getContactId());
            return null;
        }

        this.lock.set(true);
        this.resetProgress = new ResetKnowledgeProgress();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!resource.checkUnit()) {
                    // 单元错误
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                    resetProgress.setStateCode(AIGCStateCode.UnitNoReady.code);
                    return;
                }

                // 更新进度
                resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_DELETE_STORE);

                // 删除库
                JSONObject payload = new JSONObject();
                if (KnowledgeScope.Private == scope) {
                    payload.put("contactId", authToken.getContactId());
                }
                else {
                    payload.put("domain", authToken.getDomain());
                }
                Packet packet = new Packet(AIGCAction.DeleteKnowledgeStore.name, payload);
                ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                        packet.toDialect(), 60 * 1000);
                if (null == dialect) {
                    Logger.w(this.getClass(),"#resetKnowledgeStore - Request unit error: "
                            + resource.unit.getCapability().getName());
                    // 回调
                    listener.onStoreDeleteFailed(AIGCStateCode.UnitError);
                    // 解锁
                    lock.set(false);
                    // 更新进度
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                    resetProgress.setStateCode(AIGCStateCode.UnitError.code);
                    return;
                }

                Packet response = new Packet(dialect);
                int state = Packet.extractCode(response);
                if (state != AIGCStateCode.Ok.code) {
                    Logger.w(this.getClass(), "#resetKnowledgeStore - Unit return error: " + state);
                    // 回调
                    listener.onStoreDeleteFailed(AIGCStateCode.parse(state));
                    // 解锁
                    lock.set(false);
                    // 更新进度
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                    resetProgress.setStateCode(AIGCStateCode.Failure.code);
                    return;
                }

                // 回调
                listener.onStoreDeleted(authToken.getContactId(), getAuthToken().getDomain(), scope);

                // 更新进度
                resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_ACTIVATE_DOC);

                List<KnowledgeDoc> list = listKnowledgeDocs();
                if (list.isEmpty()) {
                    // 回调
                    listener.onCompleted(list, list);
                    // 解锁
                    lock.set(false);
                    // 更新进度
                    resetProgress.setTotalDocs(list.size());
                    resetProgress.setProcessedDocs(list.size());
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                    resetProgress.setStateCode(AIGCStateCode.Ok.code);
                    return;
                }

                // 更新进度
                resetProgress.setTotalDocs(list.size());
                resetProgress.setProcessedDocs(0);

                // 将文档设置为未激活
                for (KnowledgeDoc doc : list) {
                    doc.activated = false;
                    doc.numSegments = 0;
                    storage.updateKnowledgeDoc(doc);
                }

                // 分批
                int batchSize = 10;
                List<List<KnowledgeDoc>> batchList = new ArrayList<>();
                int index = 0;
                while (index < list.size()) {
                    List<KnowledgeDoc> docList = new ArrayList<>();
                    while (docList.size() < batchSize) {
                        KnowledgeDoc doc = list.get(index);
                        docList.add(doc);
                        ++index;
                        if (index >= list.size()) {
                            break;
                        }
                    }
                    batchList.add(docList);
                }

                List<KnowledgeDoc> completionList = new ArrayList<>();

                for (List<KnowledgeDoc> batch : batchList) {
                    // 更新进度
                    resetProgress.setActivatingDoc(batch.get(0));

                    // 按照批次发送给单元
                    JSONArray array = new JSONArray();
                    for (KnowledgeDoc doc : batch) {
                        array.put(doc.toJSON());
                    }
                    payload = new JSONObject();
                    payload.put("list", array);
                    packet = new Packet(AIGCAction.BatchActivateKnowledgeDocs.name, payload);
                    dialect = service.getCellet().transmit(resource.unit.getContext(),
                            packet.toDialect(), 3 * 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(),"#resetKnowledgeStore - Request unit error: "
                                + resource.unit.getCapability().getName());
                        // 回调
                        listener.onKnowledgeDocActivateFailed(list, batch, AIGCStateCode.UnitError);
                        // 解锁
                        lock.set(false);
                        // 更新进度
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.UnitError.code);
                        return;
                    }

                    response = new Packet(dialect);
                    state = Packet.extractCode(response);
                    if (state != AIGCStateCode.Ok.code) {
                        Logger.w(this.getClass(), "#resetKnowledgeStore - Unit return error: " + state);
                        // 回调
                        listener.onKnowledgeDocActivateFailed(list, batch, AIGCStateCode.parse(state));
                        // 解锁
                        lock.set(false);
                        // 更新进度
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.Failure.code);
                        return;
                    }

                    // 更新进度
                    resetProgress.setActivatingDoc(batch.get(batch.size() - 1));

                    JSONArray resultArray = Packet.extractDataPayload(response).getJSONArray("list");
                    List<KnowledgeDoc> resultList = new ArrayList<>();
                    for (int i = 0; i < resultArray.length(); ++i) {
                        KnowledgeDoc doc = new KnowledgeDoc(resultArray.getJSONObject(i));
                        resultList.add(doc);
                        storage.updateKnowledgeDoc(doc);
                    }

                    // 回调
                    listener.onKnowledgeDocActivated(list, resultList);
                    completionList.addAll(resultList);

                    // 更新进度
                    resetProgress.setProcessedDocs(resetProgress.getProcessedDocs() + resultList.size());
                }

                // 回调
                listener.onCompleted(list, completionList);
                // 解锁
                lock.set(false);
                // 更新进度
                resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                resetProgress.setStateCode(AIGCStateCode.Ok.code);
                resetProgress.setActivatingDoc(null);
            }
        });
        thread.start();

        return this.resetProgress;
    }

    public List<KnowledgeArticle> getKnowledgeArticles(String category, long startTime, long endTime) {
        List<KnowledgeArticle> list = this.storage.readKnowledgeArticles(category, startTime, endTime);
        return list;
    }

    public KnowledgeArticle appendKnowledgeArticle(KnowledgeArticle article) {
        // 重置 ID，由服务器生成
        article.resetId();

        if (this.storage.writeKnowledgeArticle(article)) {
            if (null == article.summarization) {
                // 为文章生成摘要
                this.service.generateSummarization(article.content, new SummarizationListener() {
                    @Override
                    public void onCompleted(String text, String summarization) {
                        article.summarization = summarization;
                        storage.updateKnowledgeArticleSummarization(article.getId(), summarization);
                    }

                    @Override
                    public void onFailed(String text, AIGCStateCode stateCode) {
                        Logger.w(KnowledgeBase.class, "#appendKnowledgeArticle - Generate summarization failed - id: "
                                + article.getId() + " - code: " + stateCode.code);
                    }
                });
            }

            return article;
        }

        return null;
    }

    public List<Long> removeKnowledgeArticles(List<Long> idList) {
        return this.storage.deleteKnowledgeArticles(idList);
    }

    /**
     * 获取文章列表。
     *
     * @return
     */
    public List<KnowledgeArticle> listKnowledgeArticles() {
        synchronized (this) {
            if (null != this.resource.articleList && System.currentTimeMillis() - this.resource.listArticleTime < 30 * 1000) {
                return this.resource.articleList;
            }

            this.resource.articleList = this.storage.readKnowledgeArticles(this.authToken.getDomain(),
                    this.authToken.getContactId());
            this.resource.listArticleTime = System.currentTimeMillis();
            return this.resource.articleList;
        }
    }

    /**
     * 获取已激活的文章。
     *
     * @return
     */
    private List<KnowledgeArticle> queryKnowledgeArticles() {
        if (!this.resource.checkUnit()) {
            return new ArrayList<>();
        }

        JSONObject payload = new JSONObject();
        payload.put("contactId", this.authToken.getContactId());
        Packet packet = new Packet(AIGCAction.ListKnowledgeArticles.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#listKnowledgeArticles - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return new ArrayList<>();
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#listKnowledgeArticles - Unit return error: " + state);
            return new ArrayList<>();
        }

        List<Long> idList = new ArrayList<>();
        JSONArray ids = Packet.extractDataPayload(response).getJSONArray("ids");
        for (int i = 0; i < ids.length(); ++i) {
            idList.add(ids.getLong(i));
        }

        List<KnowledgeArticle> result = this.storage.readKnowledgeArticles(idList);
        return result;
    }

    /**
     * 为指定的联系人激活知识库文章。
     *
     * @param articleIdList
     * @return
     */
    public List<KnowledgeArticle> activateKnowledgeArticles(List<Long> articleIdList) {
        List<KnowledgeArticle> articleList = this.storage.readKnowledgeArticles(articleIdList);

        if (!this.resource.checkUnit()) {
            Logger.w(this.getClass(),"#activateKnowledgeArticles - No knowledge unit");
            return null;
        }

        for (KnowledgeArticle article : articleList) {
            JSONObject payload = new JSONObject();
            payload.put("article", article.toJSON());
            payload.put("contactId", this.authToken.getContactId());
            Packet packet = new Packet(AIGCAction.ActivateKnowledgeArticle.name, payload);
            ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                    packet.toDialect(), 2 * 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(),"#activateKnowledgeArticles - Request unit error: "
                        + this.resource.unit.getCapability().getName());
                continue;
            }

            Packet response = new Packet(dialect);
            int state = Packet.extractCode(response);
            if (state != AIGCStateCode.Ok.code) {
                Logger.w(this.getClass(), "#activateKnowledgeArticles - Unit return error: " + state);
                continue;
            }

            KnowledgeArticle activated = new KnowledgeArticle(Packet.extractDataPayload(response));
            activated.content = article.content;
            activated.summarization = article.summarization;
            this.resource.appendArticle(activated);
        }

        return this.resource.articleList;
    }

    public List<KnowledgeArticle> deactivateKnowledgeArticles() {
        this.resource.checkUnit();

        List<KnowledgeArticle> result = new ArrayList<>();

        JSONObject payload = new JSONObject();
        payload.put("contactId", this.authToken.getContactId());
        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeArticle.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#deactivateKnowledgeArticles - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return result;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deactivateKnowledgeArticles - Unit return error: " + state);
            return result;
        }

        JSONArray idsArray = Packet.extractDataPayload(response).getJSONArray("ids");
        List<Long> idsList = new ArrayList<>();
        for (int i = 0; i < idsArray.length(); ++i) {
            idsList.add(idsArray.getLong(i));
        }
        result = this.storage.readKnowledgeArticles(idsList);

        this.resource.clearArticles();
        return result;
    }

    /**
     * 执行知识库 QA
     *
     * @param channelCode
     * @param unitName
     * @param query
     * @param searchTopK
     * @param searchFetchK
     * @param listener
     * @return
     */
    public boolean performKnowledgeQA(String channelCode, String unitName, String query,
                                      int searchTopK, int searchFetchK,
                                      KnowledgeQAListener listener) {
        return this.performKnowledgeQA(channelCode, unitName, query,
                searchTopK, searchFetchK, null, listener);
    }

    /**
     * 执行知识库 QA
     *
     * @param channelCode
     * @param unitName
     * @param query
     * @param searchTopK
     * @param searchFetchK
     * @param knowledgeCategory
     * @param listener
     * @return
     */
    public boolean performKnowledgeQA(String channelCode, String unitName, String query,
                                      int searchTopK, int searchFetchK,
                                      String knowledgeCategory, KnowledgeQAListener listener) {
        Logger.d(this.getClass(), "#performKnowledgeQA - Channel: " + channelCode +
                "/" + unitName + "/" + query);

        final AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Can NOT find channel: " + channelCode);
            return false;
        }

        // 如果没有设置知识库文档，返回提示
        boolean noDocument = null == this.resource.docList || this.resource.docList.isEmpty();
        boolean noArticle = null == this.resource.articleList || this.resource.articleList.isEmpty();
        if (noDocument && noArticle) {
            Logger.d(this.getClass(), "#performKnowledgeQA - No knowledge document or article in base: " + channelCode);
            this.service.getCellet().getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    long sn = Utils.generateSerialNumber();
                    channel.setLastUnitMetaSn(sn);

                    KnowledgeQAResult result = new KnowledgeQAResult(query, "");
                    AIGCGenerationRecord record = new AIGCGenerationRecord(sn, unitName, query, EMPTY_BASE_ANSWER,
                            System.currentTimeMillis(), new ComplexContext(ComplexContext.Type.Simplex));
                    result.record = record;
                    listener.onCompleted(channel, result);
                }
            });
            return true;
        }

        // 获取提示词
        boolean brisk = !unitName.equalsIgnoreCase(ModelConfig.BAIZE_UNIT);
        String prompt = this.generatePrompt(query, searchTopK, searchFetchK, brisk);
        if (null == prompt) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Generate prompt failed in channel: " + channelCode);
            return false;
        }

        AIGCUnit unit = this.service.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Select unit error: " + unitName);
            return false;
        }

        prompt = this.optimizePrompt(unitName, prompt, query);

        final KnowledgeQAResult result = new KnowledgeQAResult(query, prompt);

        if (unit.getCapability().getName().equals("MOSS")) {
            // MOSS 单元执行 conversation
            this.service.singleConversation(channel, unit, prompt, new ConversationListener() {
                @Override
                public void onConversation(AIGCChannel channel, AIGCConversationResponse response) {
                    result.conversationResponse = response;
                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode errorCode) {
                    listener.onFailed(channel, errorCode);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - Single conversation failed: "
                            + errorCode.code);
                }
            });
        }
        else {
            // 其他单元执行 chat
            this.service.singleChat(channel, unit, query, prompt, null, true,
                    new ChatListener() {
                @Override
                public void onChat(AIGCChannel channel, AIGCGenerationRecord record) {
                    result.record = record;
                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                    listener.onFailed(channel, stateCode);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - Single chat failed: " + stateCode.code);
                }
            });
        }

        return true;
    }

    private String optimizePrompt(String unitName, String prompt, String query) {
        int totalWords = 0;
        String[] lines = prompt.split("\n");
        LinkedList<String> lineList = new LinkedList<>();
        for (String line : lines) {
            if (lineList.contains(line)) {
                // 删除重复
                continue;
            }
            totalWords += line.length();
            lineList.add(line);
        }

        final int maxWords = ModelConfig.isExtraLongPromptUnit(unitName) ? 5000 : 1000;
        if (totalWords < maxWords) {
            // 提取第一行
            String first = lineList.pollFirst();

            // 从数据库里匹配文章
            TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
            List<Keyword> keywords = analyzer.analyze(query, 3);
            if (!keywords.isEmpty()) {
                // 尝试获取文章
                List<KnowledgeArticle> articleList = new ArrayList<>();
                for (Keyword keyword : keywords) {
                    List<KnowledgeArticle> articles = this.storage.matchKnowledgeArticles(this.authToken.getDomain(),
                            this.authToken.getContactId(), keyword.getWord());
                    for (KnowledgeArticle article : articles) {
                        if (articleList.contains(article)) {
                            // 排重
                            continue;
                        }
                        articleList.add(article);
                    }
                }

                if (!articleList.isEmpty()) {
                    // 按照命中的关键词数量进行从高到低排序
                    articleList = this.sortArticles(articleList, keywords);
                    for (KnowledgeArticle article : articleList) {
                        lineList.addFirst(article.content);
                        // 计算内容大小
                        totalWords += article.content.length();
                        if (totalWords > maxWords) {
                            break;
                        }
                    }
                }
            }

            // 恢复第一行
            lineList.addFirst(first);
        }

        StringBuilder buf = new StringBuilder();
        for (String line : lineList) {
            buf.append(line).append("\n");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    private List<KnowledgeArticle> sortArticles(List<KnowledgeArticle> list, List<Keyword> keywords) {
        for (KnowledgeArticle article : list) {
            int num = 0;
            for (Keyword keyword : keywords) {
                if (article.title.contains(keyword.getWord())) {
                    num += 1;
                }

                if (null != article.summarization && article.summarization.contains(keyword.getWord())) {
                    num += 1;
                }
            }
            article.numKeywords = num;
        }

        Collections.sort(list, new Comparator<KnowledgeArticle>() {
            @Override
            public int compare(KnowledgeArticle article1, KnowledgeArticle article2) {
                return article2.numKeywords - article1.numKeywords;
            }
        });

        return list;
    }

    /**
     * 使用全量数据进行问答。
     *
     * @param channelCode
     * @param unitName
     * @param pipelineQuery
     * @param comprehensiveQuery
     * @param category
     * @param maxArticles
     * @param maxParaphrases
     * @param listener
     * @return
     */
    public boolean performKnowledgeQA(String channelCode, String unitName,
                                      String pipelineQuery, String comprehensiveQuery,
                                      String category, int maxArticles, int maxParaphrases,
                                      KnowledgeQAListener listener) {
        Logger.d(this.getClass(), "#performKnowledgeQA [category] - Channel: " + channelCode +
                "/" + unitName + "/" + comprehensiveQuery + "/" + category);

        final AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Can NOT find channel: " + channelCode);
            return false;
        }

        AIGCUnit unit = this.service.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Select unit error: " + unitName);
            return false;
        }

//        Consts.formatQuestion();

        List<KnowledgeArticle> articles = this.storage.readKnowledgeArticles(category);


        return true;
    }

    private String generatePrompt(String query, int topK, int fetchK, boolean brisk) {
        if (!this.resource.checkUnit()) {
            Logger.w(this.getClass(),"#generatePrompt - No unit for knowledge base");
            return null;
        }

        JSONObject payload = new JSONObject();
        // 检索路径
        payload.put("pathKey", (KnowledgeScope.Private == this.scope) ?
                Long.toString(this.authToken.getContactId()) : this.authToken.getDomain());
        payload.put("contactId", this.authToken.getContactId());
        payload.put("query", query);
        payload.put("topK", topK);
        payload.put("fetchK", fetchK);
        payload.put("brisk", brisk);
        Packet packet = new Packet(AIGCAction.GeneratePrompt.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#generatePrompt - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#generatePrompt - Unit return error: " + state);
            return null;
        }

        JSONObject data = Packet.extractDataPayload(response);
        return data.getString("prompt");
    }

    private AIGCUnit matchUnit(String modelName) {
        ModelConfig config = this.storage.getModelConfig(modelName);
        if (null == config) {
            return null;
        }

        AIGCUnit unit = null;

        if (config.getParameter().has("unit")) {
            String unitName = config.getParameter().getString("unit");
            unit = this.service.selectUnitByName(unitName);
        }
        else {
            unit = this.service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.ImprovedConversational);
            if (null == unit) {
                unit = this.service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
            }
        }

        return unit;
    }

    public class KnowledgeResource {

        protected List<KnowledgeDoc> docList;

        protected long listDocTime;

        protected List<KnowledgeArticle> articleList;

        protected long listArticleTime;

        protected Map<String, List<KnowledgeParaphrase>> paraphraseMap;

        protected AIGCUnit unit;

        public KnowledgeResource() {
            this.unit = service.selectUnitBySubtask(
                    AICapability.NaturalLanguageProcessing.KnowledgeComprehension);
        }

        public boolean checkUnit() {
            if (null == this.unit) {
                this.reselectUnit();
            }
            else if (!this.unit.getContext().isValid()) {
                this.reselectUnit();
            }

            return (null != this.unit && this.unit.getContext().isValid());
        }

        public void reselectUnit() {
            this.unit = service.selectUnitBySubtask(
                    AICapability.NaturalLanguageProcessing.KnowledgeComprehension);
        }

        public KnowledgeDoc getKnowledgeDoc(String fileCode) {
            if (null == this.docList) {
                return null;
            }

            for (KnowledgeDoc doc : this.docList) {
                if (doc.fileCode.equals(fileCode)) {
                    return doc;
                }
            }

            return null;
        }

        public void appendDoc(KnowledgeDoc doc) {
            if (this.docList.contains(doc)) {
                this.docList.remove(doc);
            }

            this.docList.add(doc);
        }

        public void removeDoc(KnowledgeDoc doc) {
            this.docList.remove(doc);
        }

        public void removeDoc(String fileCode) {
            for (KnowledgeDoc doc : this.docList) {
                if (doc.fileCode.equals(fileCode)) {
                    this.docList.remove(doc);
                    break;
                }
            }
        }

        public void appendArticle(KnowledgeArticle article) {
            if (null == this.articleList) {
                this.articleList = new ArrayList<>();
            }

            if (!this.articleList.contains(article)) {
                this.articleList.add(article);
            }
        }

        public void removeArticle(long articleId) {
            if (null == this.articleList) {
                return;
            }

            for (int i = 0; i < this.articleList.size(); ++i) {
                KnowledgeArticle article = this.articleList.get(i);
                if (article.getId().longValue() == articleId) {
                    this.articleList.remove(i);
                    break;
                }
            }
        }

        public void clearArticles() {
            if (null == this.articleList) {
                return;
            }

            this.articleList.clear();
        }

        public List<KnowledgeParaphrase> getKnowledgeParaphrases(String category) {
            if (null == this.paraphraseMap) {
                this.paraphraseMap = new ConcurrentHashMap<>();
            }

            return this.paraphraseMap.get(category);
        }

        public void addKnowledgeParaphrases(String category, List<KnowledgeParaphrase> list) {
            if (null == this.paraphraseMap) {
                this.paraphraseMap = new ConcurrentHashMap<>();
            }

            this.paraphraseMap.put(category, list);
        }
    }
}
