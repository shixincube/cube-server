/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.knowledge;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.notice.LoadFile;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCHook;
import cube.service.aigc.AIGCPluginContext;
import cube.service.aigc.AIGCService;
import cube.service.aigc.AIGCStorage;
import cube.service.aigc.listener.*;
import cube.service.aigc.resource.Agent;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.FileType;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 知识库操作。
 */
public class KnowledgeBase {

    public final static String EMPTY_BASE_ANSWER = "您的知识库里没有数据，您可以先向知识库里导入文档或者添加文章，再向我提问。";

    public final static int DEFAULT_TOP_K = 10;

    public final static int DEFAULT_FETCH_K = 30;

    private final KnowledgeBaseInfo baseInfo;

    private AIGCService service;

    private AIGCStorage storage;

    private AuthToken authToken;

    private AbstractModule fileStorage;

    private KnowledgeResource resource;

    private KnowledgeScope scope;

    private AtomicBoolean lock;

    private KnowledgeProgress progress;

    private ResetKnowledgeProgress resetProgress;

    private KnowledgeQAProgress activateProgress;

    private AtomicBoolean qaLock;

    public KnowledgeBase(KnowledgeBaseInfo baseInfo, AIGCService service, AIGCStorage storage,
                         AuthToken authToken, AbstractModule fileStorage) {
        this.baseInfo = baseInfo;
        this.service = service;
        this.storage = storage;
        this.authToken = authToken;
        this.fileStorage = fileStorage;
        this.resource = new KnowledgeResource();
        this.scope = getProfile().scope;
        this.lock = new AtomicBoolean(false);
        this.qaLock = new AtomicBoolean(false);
    }

    public String getName() {
        return this.baseInfo.name;
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
        return this.listKnowledgeDocs(false);
    }

    public List<KnowledgeDoc> listKnowledgeDocs(boolean force) {
        synchronized (this) {
            if (force) {
                this.resource.clearDocs();
                this.resource.listDocTime = 0;
            }

            if (null != this.resource.docList && System.currentTimeMillis() - this.resource.listDocTime < 30 * 1000) {
                Logger.d(this.getClass(), "#listKnowledgeDocs - " + this.getName() +
                        " - " + this.getAuthToken().getContactId() +
                        " - Read from memory");
                return this.resource.docList;
            }

            List<KnowledgeDoc> list = (KnowledgeScope.Private == this.scope) ?
                    this.storage.readKnowledgeDocList(this.authToken.getDomain(), this.authToken.getContactId(),
                            this.baseInfo.name)
                    : this.storage.readKnowledgeDocList(this.authToken.getDomain(), this.baseInfo.name);

            Iterator<KnowledgeDoc> iter = list.iterator();
            while (iter.hasNext()) {
                KnowledgeDoc doc = iter.next();
                GetFile getFile = new GetFile(this.authToken.getDomain(), doc.fileCode);
                JSONObject fileLabelJson = this.fileStorage.notify(getFile);
                if (null == fileLabelJson) {
                    // 文件已被删除
                    iter.remove();
                    this.storage.deleteKnowledgeDoc(doc.baseName, doc.fileCode);
                    continue;
                }

                doc.setFileLabel(new FileLabel(fileLabelJson));
            }

            this.resource.listDocTime = System.currentTimeMillis();
            this.resource.appendDocs(list);

            return list;
        }
    }

    public KnowledgeDoc getKnowledgeDocByFileCode(String fileCode) {
        KnowledgeDoc doc = this.storage.readKnowledgeDoc(this.baseInfo.name, fileCode);
        if (null != doc) {
            GetFile getFile = new GetFile(this.authToken.getDomain(), doc.fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null != fileLabelJson) {
                doc.setFileLabel(new FileLabel(fileLabelJson));
            }
        }
        return doc;
    }

    public KnowledgeDoc importKnowledgeDoc(String fileCode, String splitter) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#importKnowledgeDoc - " + this.getName() + " - Store locked: " + fileCode);
            return null;
        }
        this.lock.set(true);

        KnowledgeDoc activatedDoc = null;

        try {
            GetFile getFile = new GetFile(this.authToken.getDomain(), fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null == fileLabelJson) {
                this.storage.deleteKnowledgeDoc(this.baseInfo.name, fileCode);
                Logger.e(this.getClass(), "#importKnowledgeDoc - " + this.getName() + " - Not find file: " + fileCode);
                this.lock.set(false);
                return null;
            }

            FileLabel fileLabel = new FileLabel(fileLabelJson);

            KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
            boolean newDoc = false;
            if (null == doc) {
                newDoc = true;
                // 创建新文档
                doc = new KnowledgeDoc(Utils.generateSerialNumber(), this.authToken.getDomain(),
                        this.authToken.getContactId(), fileCode, this.baseInfo.name, fileLabel.getFileName(),
                        false, -1, this.scope);
            }

            if (null == doc.getFileLabel()) {
                doc.setFileLabel(fileLabel);
            }

            // 设置分割器
            doc.splitter = splitter;

            if (newDoc) {
                // 写入库
                this.storage.writeKnowledgeDoc(doc);
            }

            activatedDoc = doc;

            if (!doc.activated) {
                if (!this.resource.checkUnit()) {
                    Logger.e(this.getClass(), "#importKnowledgeDoc - " + this.getName() + " - Not find Knowledge unit");
                    this.lock.set(false);
                    return null;
                }

                activatedDoc = this.activateKnowledgeDoc(doc);
                if (null == activatedDoc) {
                    Logger.e(this.getClass(), "#importKnowledgeDoc - " + this.getName() + " - Unit return error");
                    this.storage.deleteKnowledgeDoc(this.baseInfo.name, fileCode);
                    this.lock.set(false);
                    return null;
                }
            }

            // 追加文档
            this.resource.appendDoc(activatedDoc);

            // Hook
            AIGCHook hook = this.service.getPluginSystem().getImportKnowledgeDocHook();
            hook.apply(new AIGCPluginContext(this, activatedDoc));

            Logger.d(this.getClass(), "#importKnowledgeDoc - " + this.getName() + " - file code: " + fileCode);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#importKnowledgeDoc - " + this.getName(), e);
        } finally {
            this.lock.set(false);
        }

        return activatedDoc;
    }

    public KnowledgeDoc removeKnowledgeDoc(String fileCode) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#removeKnowledgeDoc - " + this.getName() + " - Store locked: " + fileCode);
            return null;
        }
        this.lock.set(true);

        KnowledgeDoc doc = null;
        try {
            doc = this.getKnowledgeDocByFileCode(fileCode);
            if (null != doc) {
                if (doc.activated) {
                    if (this.resource.checkUnit()) {
                        KnowledgeDoc deactivatedDoc = this.deactivateKnowledgeDoc(doc);
                        doc = deactivatedDoc;
                    }
                    else {
                        Logger.w(this.getClass(), "#removeKnowledgeDoc - " + this.getName() + " - Unit error: " + fileCode);
                        return null;
                    }
                }

                // 删除文档
                this.storage.deleteKnowledgeDoc(this.baseInfo.name, fileCode);
                // 删除分段
                this.storage.deleteKnowledgeSegments(doc.getId());

                this.resource.removeDoc(doc);
            }
            else {
                doc = this.resource.removeDoc(fileCode);
            }

            if (null != doc) {
                // Hook
                AIGCHook hook = this.service.getPluginSystem().getRemoveKnowledgeDoc();
                hook.apply(new AIGCPluginContext(this, doc));
            }

            Logger.d(this.getClass(), "#removeKnowledgeDoc - " + this.getName() + " - file code: " + fileCode);
        } catch (Exception e) {
            // Nothing
        } finally {
            this.lock.set(false);
        }

        return doc;
    }

    public List<KnowledgeSegment> getKnowledgeSegments(long docId, int startIndex, int endIndex) {
        return this.storage.readKnowledgeSegments(docId, startIndex, endIndex);
    }

    public int numKnowledgeSegments(long docId) {
        return this.storage.countKnowledgeSegments(docId);
    }

    /**
     * 获取操作进度。
     *
     * @return
     */
    public KnowledgeProgress getProgress() {
        return this.progress;
    }

    /**
     * 批量导入知识文档。
     *
     * @param fileCodeList
     * @param splitter
     * @param listener
     * @return
     */
    public KnowledgeProgress batchImportKnowledgeDocuments(List<String> fileCodeList, String splitter,
                                                           KnowledgeProgressListener listener) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#batchImportKnowledgeDocuments - Store locked: " + this.baseInfo.name);
            listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.Busy.code));
            return new KnowledgeProgress(AIGCStateCode.Busy.code);
        }
        this.lock.set(true);

        try {
            // 检查单元
            if (!this.resource.checkUnit()) {
                listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.UnitNoReady.code));
                this.lock.set(false);
                return new KnowledgeProgress(AIGCStateCode.UnitNoReady.code);
            }

            // 获取所有文件信息
            List<KnowledgeDoc> allList = new ArrayList<>();

            for (String fileCode : fileCodeList) {
                KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
                if (null == doc) {
                    // 创建新文档
                    doc = new KnowledgeDoc(Utils.generateSerialNumber(), this.authToken.getDomain(),
                            this.authToken.getContactId(), fileCode, this.baseInfo.name, null,
                            false, -1, this.scope);

                    GetFile getFile = new GetFile(this.authToken.getDomain(), fileCode);
                    JSONObject fileLabelJson = this.fileStorage.notify(getFile);
                    if (null == fileLabelJson) {
                        Logger.w(this.getClass(), "#batchImportKnowledgeDocuments - " +
                                this.baseInfo.name + " - Not find file: " + fileCode);
                        continue;
                    }

                    doc.setFileLabel(new FileLabel(fileLabelJson));
                }
                else {
                    // 该文件已经导入，跳过
                    if (doc.activated) {
                        continue;
                    }
                }

                if (null == doc.getFileLabel()) {
                    // 删除不存在的文档
                    this.storage.deleteKnowledgeDoc(this.baseInfo.name, fileCode);
                    continue;
                }

                // 设置分割器
                doc.splitter = splitter;
                allList.add(doc);
            }

            if (allList.isEmpty()) {
                Logger.w(this.getClass(), "#batchImportKnowledgeDocuments - " + this.baseInfo.name + " - No file");
                listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.NoData.code));
                this.lock.set(false);
                return new KnowledgeProgress(AIGCStateCode.NoData.code);
            }

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#batchImportKnowledgeDocuments - " +
                        this.baseInfo.name + " - Total file num: " + allList.size());
            }

            // 分批
            int batchSize = 10;
            final List<List<KnowledgeDoc>> batchList = new ArrayList<>();
            int index = 0;
            while (index < allList.size()) {
                List<KnowledgeDoc> docList = new ArrayList<>();
                while (docList.size() < batchSize) {
                    KnowledgeDoc doc = allList.get(index);
                    docList.add(doc);
                    ++index;
                    if (index >= allList.size()) {
                        break;
                    }
                }
                batchList.add(docList);
            }

            final List<KnowledgeDoc> completionList = new ArrayList<>();

            // 创建进度
            this.progress = new KnowledgeProgress(AIGCStateCode.Processing.code);
            // 设置总文档数
            this.progress.setTotalDocs(allList.size());

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (List<KnowledgeDoc> batch : batchList) {
                            // 更新进度
                            progress.setProcessingDoc(batch.get(0));

                            // 按照批次发送给单元
                            JSONArray array = new JSONArray();
                            for (KnowledgeDoc doc : batch) {
                                array.put(doc.toJSON());
                            }
                            JSONObject payload = new JSONObject();
                            payload.put("list", array);
                            payload.put("name", baseInfo.name);
                            Packet packet = new Packet(AIGCAction.BatchActivateKnowledgeDocs.name, payload);
                            ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                                    packet.toDialect(), 3 * 60 * 1000);
                            if (null == dialect) {
                                Logger.w(this.getClass(),"#batchImportKnowledgeDocuments - " +
                                        baseInfo.name + " - Request unit error: "
                                        + resource.unit.getCapability().getName());
                                // 更新进度
                                progress.setStateCode(AIGCStateCode.UnitError.code);
                                progress.setEndTime(System.currentTimeMillis());
                                listener.onFailed(KnowledgeBase.this, progress);
                                continue;
                            }

                            Packet response = new Packet(dialect);
                            int state = Packet.extractCode(response);
                            if (state != AIGCStateCode.Ok.code) {
                                Logger.w(this.getClass(), "#batchImportKnowledgeDocuments - " +
                                        baseInfo.name + " - Unit return error: " + state);
                                // 更新进度
                                progress.setStateCode(AIGCStateCode.Failure.code);
                                progress.setEndTime(System.currentTimeMillis());
                                listener.onFailed(KnowledgeBase.this, progress);
                                continue;
                            }

                            // 更新进度
                            progress.setProcessingDoc(batch.get(batch.size() - 1));

                            // 更新完成列表
                            JSONArray resultArray = Packet.extractDataPayload(response).getJSONArray("list");
                            for (int i = 0; i < resultArray.length(); ++i) {
                                KnowledgeDoc doc = new KnowledgeDoc(resultArray.getJSONObject(i));
                                completionList.add(doc);
                            }

                            // 更新进度
                            progress.setProcessedDocs(completionList.size());

                            // 回调
                            listener.onProgress(KnowledgeBase.this, progress);
                        }

                        // 取消活跃文档
                        progress.setProcessingDoc(null);

                        // 获取最新列表
                        List<KnowledgeDoc> currentList = listKnowledgeDocs(true);
                        for (KnowledgeDoc doc : completionList) {
                            if (currentList.contains(doc)) {
                                // 已经存在
                                storage.updateKnowledgeDoc(doc);
                            }
                            else {
                                // 新文档
                                storage.writeKnowledgeDoc(doc);
                            }

                            // Hook
                            AIGCHook hook = service.getPluginSystem().getImportKnowledgeDocHook();
                            hook.apply(new AIGCPluginContext(KnowledgeBase.this, doc));
                        }

                        // 刷新列表
                        listKnowledgeDocs(true);

                        // 更新进度
                        progress.setStateCode(AIGCStateCode.Ok.code);
                        progress.setEndTime(System.currentTimeMillis());
                        listener.onCompleted(KnowledgeBase.this, progress);
                    } catch (Exception e) {
                        progress.setStateCode(AIGCStateCode.Failure.code);
                        progress.setEndTime(System.currentTimeMillis());
                        listener.onFailed(KnowledgeBase.this, progress);
                        Logger.e(KnowledgeBase.class, "#batchImportKnowledgeDocuments - " + baseInfo.name, e);
                    } finally {
                        // 解锁
                        lock.set(false);
                    }
                }
            })).start();

            return this.progress;
        } catch (Exception e) {
            // 异常
            listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.Failure.code));
            this.lock.set(false);
            return new KnowledgeProgress(AIGCStateCode.Failure.code);
        }
    }

    /**
     * 批量移除知识文档。
     *
     * @param fileCodeList
     * @param listener
     * @return
     */
    public KnowledgeProgress batchRemoveKnowledgeDocuments(List<String> fileCodeList, KnowledgeProgressListener listener) {
        if (this.lock.get()) {
            Logger.w(this.getClass(), "#batchRemoveKnowledgeDocuments - Store locked: " + this.baseInfo.name);
            listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.Busy.code));
            return new KnowledgeProgress(AIGCStateCode.Busy.code);
        }
        this.lock.set(true);

        try {
            // 检查单元
            if (!this.resource.checkUnit()) {
                listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.UnitNoReady.code));
                this.lock.set(false);
                return new KnowledgeProgress(AIGCStateCode.UnitNoReady.code);
            }

            // 检查文档
            List<KnowledgeDoc> allList = new ArrayList<>();
            for (String fileCode : fileCodeList) {
                KnowledgeDoc doc = this.resource.getKnowledgeDoc(fileCode);
                if (null != doc) {
                    allList.add(doc);
                }
            }

            if (allList.isEmpty()) {
                Logger.w(this.getClass(), "#batchRemoveKnowledgeDocuments - " + this.baseInfo.name + " - No file");
                listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.NoData.code));
                this.lock.set(false);
                return new KnowledgeProgress(AIGCStateCode.NoData.code);
            }

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#batchRemoveKnowledgeDocuments - " +
                        this.baseInfo.name + " - Total file num: " + allList.size());
            }

            // 分批
            int batchSize = 10;
            final List<List<KnowledgeDoc>> batchList = new ArrayList<>();
            int index = 0;
            while (index < allList.size()) {
                List<KnowledgeDoc> docList = new ArrayList<>();
                while (docList.size() < batchSize) {
                    KnowledgeDoc doc = allList.get(index);
                    docList.add(doc);
                    ++index;
                    if (index >= allList.size()) {
                        break;
                    }
                }
                batchList.add(docList);
            }

            final List<KnowledgeDoc> completionList = new ArrayList<>();

            // 创建进度
            this.progress = new KnowledgeProgress(AIGCStateCode.Processing.code);
            // 设置总文档数
            this.progress.setTotalDocs(allList.size());

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (List<KnowledgeDoc> batch : batchList) {
                            // 更新进度
                            progress.setProcessingDoc(batch.get(0));

                            // 按照批次发送给单元
                            JSONArray array = new JSONArray();
                            for (KnowledgeDoc doc : batch) {
                                array.put(doc.toJSON());
                            }
                            JSONObject payload = new JSONObject();
                            payload.put("list", array);
                            payload.put("name", baseInfo.name);
                            Packet packet = new Packet(AIGCAction.BatchDeactivateKnowledgeDocs.name, payload);
                            ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                                    packet.toDialect(), 3 * 60 * 1000);
                            if (null == dialect) {
                                Logger.w(this.getClass(),"#batchRemoveKnowledgeDocuments - " +
                                        baseInfo.name + " - Request unit error: "
                                        + resource.unit.getCapability().getName());
                                // 更新进度
                                progress.setStateCode(AIGCStateCode.UnitError.code);
                                progress.setEndTime(System.currentTimeMillis());
                                listener.onFailed(KnowledgeBase.this, progress);
                                continue;
                            }

                            Packet response = new Packet(dialect);
                            int state = Packet.extractCode(response);
                            if (state != AIGCStateCode.Ok.code) {
                                Logger.w(this.getClass(), "#batchRemoveKnowledgeDocuments - " +
                                        baseInfo.name + " - Unit return error: " + state);
                                // 更新进度
                                progress.setStateCode(AIGCStateCode.Failure.code);
                                progress.setEndTime(System.currentTimeMillis());
                                listener.onFailed(KnowledgeBase.this, progress);
                                continue;
                            }

                            // 更新进度
                            progress.setProcessingDoc(batch.get(batch.size() - 1));

                            // 更新完成列表
                            JSONArray resultArray = Packet.extractDataPayload(response).getJSONArray("list");
                            for (int i = 0; i < resultArray.length(); ++i) {
                                KnowledgeDoc doc = new KnowledgeDoc(resultArray.getJSONObject(i));
                                completionList.add(doc);
                            }

                            // 更新进度
                            progress.setProcessedDocs(completionList.size());

                            // 回调
                            listener.onProgress(KnowledgeBase.this, progress);
                        }

                        // 取消活跃文档
                        progress.setProcessingDoc(null);

                        // 删除文档
                        for (KnowledgeDoc doc : completionList) {
                            storage.deleteKnowledgeDoc(doc.baseName, doc.fileCode);
                            resource.removeDoc(doc);
                        }

                        // 更新进度
                        progress.setStateCode(AIGCStateCode.Ok.code);
                        progress.setEndTime(System.currentTimeMillis());
                        listener.onCompleted(KnowledgeBase.this, progress);
                    } catch (Exception e) {
                        progress.setStateCode(AIGCStateCode.Failure.code);
                        progress.setEndTime(System.currentTimeMillis());
                        listener.onFailed(KnowledgeBase.this, progress);
                        Logger.e(KnowledgeBase.class, "#batchRemoveKnowledgeDocuments - " + baseInfo.name, e);
                    } finally {
                        // 解锁
                        lock.set(false);
                    }
                }
            })).start();

            return this.progress;
        } catch (Exception e) {
            // 异常
            listener.onFailed(this, new KnowledgeProgress(AIGCStateCode.Failure.code));
            this.lock.set(false);
            return new KnowledgeProgress(AIGCStateCode.Failure.code);
        }
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
            Logger.w(this.getClass(),"#activateKnowledgeDoc - " + this.baseInfo.name + " - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#activateKnowledgeDoc - " + this.baseInfo.name + " - Unit return error: " + state);
            return null;
        }

        // 更新文档
        KnowledgeDoc activatedDoc = new KnowledgeDoc(Packet.extractDataPayload(response));
        if (activatedDoc.numSegments <= 0) {
            Logger.w(this.getClass(), "#activateKnowledgeDoc - " +
                    this.baseInfo.name + " - Num of segments error: " + activatedDoc.numSegments);
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
        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeDoc.name, doc.toJSON());
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#deactivateKnowledgeDoc - " + this.baseInfo.name + " - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deactivateKnowledgeDoc - " + this.baseInfo.name + " - Unit return error: " + state);
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
            Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                    this.baseInfo.name + " - Store locked: " + this.authToken.getContactId());
            return null;
        }
        this.lock.set(true);

        this.resetProgress = new ResetKnowledgeProgress();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!resource.checkUnit()) {
                        // 单元错误
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.UnitNoReady.code);
                        listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.UnitNoReady);
                        lock.set(false);
                        return;
                    }

                    // 备份
                    if (backup) {
                        // 更新进度
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_BACKUP_STORE);
                        // 回调
                        listener.onProgress(KnowledgeBase.this, resetProgress);

                        JSONObject payload = new JSONObject();
                        if (KnowledgeScope.Private == scope) {
                            payload.put("contactId", authToken.getContactId());
                        } else {
                            payload.put("domain", authToken.getDomain());
                        }
                        payload.put("name", baseInfo.name);
                        Packet packet = new Packet(AIGCAction.BackupKnowledgeStore.name, payload);
                        ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                                packet.toDialect(), 90 * 1000);
                        if (null == dialect) {
                            Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                    baseInfo.name + " - Request unit error: "
                                    + resource.unit.getCapability().getName());
                            // 回调
                            listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.UnitError);
                            // 更新进度
                            resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                            resetProgress.setStateCode(AIGCStateCode.UnitError.code);
                            lock.set(false);
                            return;
                        }

                        Packet response = new Packet(dialect);
                        int state = Packet.extractCode(response);
                        if (state != AIGCStateCode.Ok.code) {
                            Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                    baseInfo.name + " - Unit return error: " + state);
                            // 回调
                            listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.parse(state));
                            // 更新进度
                            resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                            resetProgress.setStateCode(AIGCStateCode.Failure.code);
                            lock.set(false);
                            return;
                        }
                    }

                    // 更新进度
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_DELETE_STORE);
                    // 回调
                    listener.onProgress(KnowledgeBase.this, resetProgress);

                    // 删除库
                    JSONObject payload = new JSONObject();
                    if (KnowledgeScope.Private == scope) {
                        payload.put("contactId", authToken.getContactId());
                    } else {
                        payload.put("domain", authToken.getDomain());
                    }
                    payload.put("name", baseInfo.name);
                    Packet packet = new Packet(AIGCAction.DeleteKnowledgeStore.name, payload);
                    ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                            packet.toDialect(), 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                baseInfo.name + " - Request unit error: "
                                + resource.unit.getCapability().getName());
                        // 回调
                        listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.UnitError);
                        // 更新进度
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.UnitError.code);
                        lock.set(false);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    int state = Packet.extractCode(response);
                    if (state != AIGCStateCode.Ok.code) {
                        Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                baseInfo.name + " - Unit return error: " + state);
                        // 回调
                        listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.parse(state));
                        // 更新进度
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.Failure.code);
                        lock.set(false);
                        return;
                    }

                    // 更新进度
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_ACTIVATE_DOC);
                    // 回调
                    listener.onProgress(KnowledgeBase.this, resetProgress);

                    List<KnowledgeDoc> list = listKnowledgeDocs();
                    if (null == list || list.isEmpty()) {
                        // 更新进度
                        resetProgress.setTotalDocs(0);
                        resetProgress.setProcessedDocs(0);
                        resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                        resetProgress.setStateCode(AIGCStateCode.Ok.code);
                        // 回调
                        listener.onProgress(KnowledgeBase.this, resetProgress);
                        listener.onCompleted(KnowledgeBase.this, list, list);
                        lock.set(false);
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
                        payload.put("name", baseInfo.name);
                        packet = new Packet(AIGCAction.BatchActivateKnowledgeDocs.name, payload);
                        dialect = service.getCellet().transmit(resource.unit.getContext(),
                                packet.toDialect(), 3 * 60 * 1000);
                        if (null == dialect) {
                            Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                    baseInfo.name + " - Request unit error: "
                                    + resource.unit.getCapability().getName());
                            // 回调
                            listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.UnitError);
                            // 更新进度
                            resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                            resetProgress.setStateCode(AIGCStateCode.UnitError.code);
                            lock.set(false);
                            return;
                        }

                        response = new Packet(dialect);
                        state = Packet.extractCode(response);
                        if (state != AIGCStateCode.Ok.code) {
                            Logger.w(this.getClass(), "#resetKnowledgeStore - " +
                                    baseInfo.name + " - Unit return error: " + state);
                            // 回调
                            listener.onFailed(KnowledgeBase.this, resetProgress, AIGCStateCode.parse(state));
                            // 更新进度
                            resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                            resetProgress.setStateCode(AIGCStateCode.Failure.code);
                            lock.set(false);
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

                        // 更新进度
                        resetProgress.setProcessedDocs(resetProgress.getProcessedDocs() + resultList.size());

                        // 回调
                        listener.onProgress(KnowledgeBase.this, resetProgress);
                        // 更新完成列表
                        completionList.addAll(resultList);
                    }

                    // 更新进度
                    resetProgress.setProgress(ResetKnowledgeProgress.PROGRESS_END);
                    resetProgress.setStateCode(AIGCStateCode.Ok.code);
                    resetProgress.setActivatingDoc(null);
                    // 回调
                    listener.onProgress(KnowledgeBase.this, resetProgress);
                    listener.onCompleted(KnowledgeBase.this, list, completionList);

                    Logger.i(this.getClass(), "#resetKnowledgeStore - " +
                            baseInfo.name + " - Resets knowledge base completed: "
                            + authToken.getContactId());

                    // 更新内存数据
                    listKnowledgeDocs(true);
                    listKnowledgeArticles(true);
                    lock.set(false);
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#resetKnowledgeStore", e);
                } finally {
                    // 解锁
                    lock.set(false);
                }
            }
        });
        thread.start();

        return this.resetProgress;
    }

    /**
     * 删除当前库的所有数据。
     *
     * @return
     */
    public boolean destroy() {
        if (!this.resource.checkUnit()) {
            return false;
        }

        List<KnowledgeDoc> docList = this.listKnowledgeDocs(true);

        for (KnowledgeDoc doc : docList) {
            // 删除文档记录
            this.storage.deleteKnowledgeDoc(this.baseInfo.name, doc.fileCode);
            // 删除文档分段记录
            this.storage.deleteKnowledgeSegments(doc.getId());
        }

        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 删除库
                    JSONObject payload = new JSONObject();
                    if (KnowledgeScope.Private == scope) {
                        payload.put("contactId", authToken.getContactId());
                    } else {
                        payload.put("domain", authToken.getDomain());
                    }
                    payload.put("name", baseInfo.name);
                    Packet packet = new Packet(AIGCAction.DeleteKnowledgeStore.name, payload);
                    ActionDialect dialect = service.getCellet().transmit(resource.unit.getContext(),
                            packet.toDialect(), 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(), "#destroy - " + baseInfo.name + " - Request unit error: "
                                + resource.unit.getCapability().getName());
                        return;
                    }

                    Packet response = new Packet(dialect);
                    int state = Packet.extractCode(response);
                    if (state != AIGCStateCode.Ok.code) {
                        Logger.w(this.getClass(), "#destroy - " + baseInfo.name + " - Unit return error: " + state);
                    }
                } catch (Exception e) {
                    // Nothing
                }
            }
        })).start();

        return true;
    }

    public JSONArray getKnowledgeBackups() {
        if (!this.resource.checkUnit()) {
            return null;
        }

        JSONObject payload = new JSONObject();
        if (KnowledgeScope.Private == this.scope) {
            payload.put("contactId", this.authToken.getContactId());
        }
        else {
            payload.put("domain", this.authToken.getDomain());
        }
        payload.put("name", this.baseInfo.name);
        Packet packet = new Packet(AIGCAction.GetBackupKnowledgeStores.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect(), 30 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#getKnowledgeBackups - " + baseInfo.name + " - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getKnowledgeBackups - " + baseInfo.name + " - Unit return error: " + state);
            return null;
        }

        return Packet.extractDataPayload(response).getJSONArray("list");
    }

    public List<KnowledgeArticle> getKnowledgeArticles(long startTime, long endTime, boolean activated) {
        List<KnowledgeArticle> articles = this.listKnowledgeArticles();
        List<KnowledgeArticle> list = new ArrayList<>();
        for (KnowledgeArticle article : articles) {
            if (article.getTimestamp() >= startTime && article.getTimestamp() <= endTime &&
                article.activated == activated) {
                list.add(article);
            }
        }
        return list;
    }

    public KnowledgeArticle getKnowledgeArticle(long articleId) {
        KnowledgeArticle article = this.storage.readKnowledgeArticle(articleId);
        return article;
    }

    public KnowledgeArticle updateKnowledgeArticle(KnowledgeArticle article) {
        KnowledgeArticle result = this.storage.updateKnowledgeArticle(article);
        return result;
    }

    public KnowledgeArticle appendKnowledgeArticle(KnowledgeArticle article) {
        // 重置 ID，由服务器生成
        article.resetId();

        if (this.storage.writeKnowledgeArticle(article)) {
            // 更新
            this.resource.appendArticle(article);

            if (null == article.summarization) {
                List<String> contentList = this.trimArticleContent(article.content, 300);

                Logger.d(KnowledgeBase.class, "#appendKnowledgeArticle - "
                        + baseInfo.name + " - Content list length: "
                        + contentList.size());

                List<String> resultList = new ArrayList<>();
                AtomicInteger count = new AtomicInteger(0);

                for (String content : contentList) {
                    // 为文章生成摘要
                    boolean success = this.service.generateSummarization(content, new SummarizationListener() {
                        @Override
                        public void onCompleted(String text, String summarization) {
                            resultList.add(summarization);

                            count.incrementAndGet();
                            if (count.get() == contentList.size()) {
                                StringBuilder buf = new StringBuilder();
                                for (String result : resultList) {
                                    buf.append(result).append("。\n");
                                }
                                buf.delete(buf.length() - 1, buf.length());

                                article.summarization = buf.toString();
                                storage.updateKnowledgeArticleSummarization(article.getId(), article.summarization);
                            }
                        }

                        @Override
                        public void onFailed(String text, AIGCStateCode stateCode) {
                            Logger.w(KnowledgeBase.class, "#appendKnowledgeArticle - "
                                    + baseInfo.name + " - Generate summarization failed - id: "
                                    + article.getId() + " - code: " + stateCode.code);

                            count.incrementAndGet();
                            if (count.get() == contentList.size() && !resultList.isEmpty()) {
                                StringBuilder buf = new StringBuilder();
                                for (String result : resultList) {
                                    buf.append(result).append("。\n");
                                }
                                buf.delete(buf.length() - 1, buf.length());

                                article.summarization = buf.toString();
                                storage.updateKnowledgeArticleSummarization(article.getId(), article.summarization);
                            }
                        }
                    });

                    if (!success) {
                        count.incrementAndGet();
                    }
                }
            }

            return article;
        }

        return null;
    }

    public List<Long> removeKnowledgeArticles(List<Long> idList) {
        for (long id : idList) {
            this.resource.removeArticle(id);
        }
        return this.storage.deleteKnowledgeArticles(idList);
    }

    /**
     * 获取文章列表。
     *
     * @return
     */
    public List<KnowledgeArticle> listKnowledgeArticles() {
        return this.listKnowledgeArticles(false);
    }

    public List<KnowledgeArticle> listKnowledgeArticles(boolean force) {
        synchronized (this) {
            if (force) {
                this.resource.clearArticles();
                this.resource.listArticleTime = 0;
            }

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
    private List<KnowledgeArticle> queryActivatedArticles() {
        if (!this.resource.checkUnit()) {
            return new ArrayList<>();
        }

        JSONObject payload = new JSONObject();
        if (KnowledgeScope.Private == this.scope) {
            payload.put("contactId", this.authToken.getContactId());
        }
        else {
            payload.put("domain", this.authToken.getDomain());
        }
        Packet packet = new Packet(AIGCAction.ListKnowledgeArticles.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                packet.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#queryActivatedArticles - " + baseInfo.name + " - Request unit error: "
                    + this.resource.unit.getCapability().getName());
            return new ArrayList<>();
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#queryActivatedArticles - " + baseInfo.name + " - Unit return error: " + state);
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
        if (!this.resource.checkUnit()) {
            Logger.w(this.getClass(),"#activateKnowledgeArticles - " + baseInfo.name + " - No knowledge unit");
            return null;
        }

        List<KnowledgeArticle> result = new ArrayList<>();

        List<KnowledgeArticle> articleList = this.storage.readKnowledgeArticles(articleIdList);

        for (KnowledgeArticle article : articleList) {
            JSONObject payload = new JSONObject();
            payload.put("article", article.toJSON());
            payload.put("contactId", this.authToken.getContactId());
            Packet packet = new Packet(AIGCAction.ActivateKnowledgeArticle.name, payload);
            ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                    packet.toDialect(), 2 * 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(),"#activateKnowledgeArticles - " + baseInfo.name + " - Request unit error: "
                        + this.resource.unit.getCapability().getName());
                continue;
            }

            Packet response = new Packet(dialect);
            int state = Packet.extractCode(response);
            if (state != AIGCStateCode.Ok.code) {
                Logger.w(this.getClass(), "#activateKnowledgeArticles - "
                        + baseInfo.name + " - Unit return error: " + state);
                continue;
            }

            KnowledgeArticle activated = new KnowledgeArticle(Packet.extractDataPayload(response));
            activated.content = article.content;
            activated.summarization = article.summarization;
            result.add(activated);
        }

        return result;
    }

    public List<KnowledgeArticle> deactivateKnowledgeArticles(List<Long> articleIdList) {
        if (!this.resource.checkUnit()) {
            Logger.w(this.getClass(),"#deactivateKnowledgeArticles - " + baseInfo.name + " - No knowledge unit");
            return null;
        }

        List<Long> idsList = new ArrayList<>();

        List<KnowledgeArticle> articleList = this.storage.readKnowledgeArticles(articleIdList);
        for (KnowledgeArticle article : articleList) {
            JSONObject payload = new JSONObject();
            payload.put("article", article.toCompactJSON());
            payload.put("contactId", this.authToken.getContactId());
            Packet packet = new Packet(AIGCAction.DeactivateKnowledgeArticle.name, payload);
            ActionDialect dialect = this.service.getCellet().transmit(this.resource.unit.getContext(),
                    packet.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(),"#deactivateKnowledgeArticles - " + baseInfo.name + " - Request unit error: "
                        + this.resource.unit.getCapability().getName());
                continue;
            }

            Packet response = new Packet(dialect);
            int state = Packet.extractCode(response);
            if (state != AIGCStateCode.Ok.code) {
                Logger.w(this.getClass(), "#deactivateKnowledgeArticles - "
                        + baseInfo.name + " - Unit return error: " + state);
                continue;
            }

            JSONArray idsArray = Packet.extractDataPayload(response).getJSONArray("ids");
            for (int i = 0; i < idsArray.length(); ++i) {
                idsList.add(idsArray.getLong(i));
            }
        }

        return this.storage.readKnowledgeArticles(idsList);
    }

    /**
     * 查询所有文章分类。
     *
     * @return
     */
    public List<String> queryAllArticleCategories() {
        return this.storage.queryAllArticleCategories(this.authToken.getContactId());
    }

    /**
     * 执行知识库 QA
     *
     * @param channelCode
     * @param unitName
     * @param query
     * @param searchTopK
     * @param knowledgeCategories
     * @param recordList
     * @param listener
     * @return
     */
    public boolean performKnowledgeQA(String channelCode, String unitName, String query, int searchTopK,
                                      List<String> knowledgeCategories, List<GeneratingRecord> recordList,
                                      KnowledgeQAListener listener) {
        query = query.replaceAll("\n", "");

        Logger.d(this.getClass(), "#performKnowledgeQA - " + baseInfo.name + " - Channel: " + channelCode +
                "/" + unitName + "/" + query);

        final AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#performKnowledgeQA - "
                    + baseInfo.name + " - Can NOT find channel: " + channelCode);
            return false;
        }

        // 处理记录里的附件文件
        List<String> attachmentContents = new ArrayList<>();
        if (null != recordList && !recordList.isEmpty()) {
            attachmentContents = this.processQueryAttachments(recordList);
        }

        // 如果没有设置知识库文档，返回提示
        boolean noDocument = null == this.resource.docList || this.resource.docList.isEmpty();
        boolean noArticle = null == this.resource.articleList || this.resource.articleList.isEmpty();

        if (noArticle && !attachmentContents.isEmpty()) {
            noArticle = false;
        }

        final String queryForResult = query;

        if (noDocument && noArticle) {
            Logger.d(this.getClass(), "#performKnowledgeQA - "
                    + baseInfo.name + " - No knowledge document or article in base: " + channelCode);
            this.service.getCellet().getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    long sn = Utils.generateSerialNumber();
                    channel.setLastUnitMetaSn(sn);

                    KnowledgeQAResult result = new KnowledgeQAResult(queryForResult, "");
                    GeneratingRecord record = new GeneratingRecord(sn, unitName, queryForResult, EMPTY_BASE_ANSWER,
                            "", System.currentTimeMillis(), new ComplexContext(ComplexContext.Type.Lightweight));
                    result.record = record;
                    listener.onCompleted(channel, result);
                }
            });
            return true;
        }

        AIGCUnit unit = this.service.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#performKnowledgeQA - "
                    + baseInfo.name + " - Select unit error: " + unitName);
            return false;
        }

        PromptMetadata promptMetadata = null;
        // 根据文件名匹配文档，读取文件内容进行问题推理
        promptMetadata = this.extractDocumentContent(query, 5);
        if (null != promptMetadata && !promptMetadata.isEmpty()) {
            Logger.d(this.getClass(), "#performKnowledgeQA - "
                    + baseInfo.name + " - Generate prompt from document - num:" +
                    promptMetadata.metadataList.size());
            // 使用文档推理结果生成提示词
            promptMetadata = promptMetadata.generatePrompt(query);
            if (null == promptMetadata) {
                promptMetadata = this.generatePrompt(query, searchTopK);
            }
        }
        else {
            // 获取提示词
//            boolean brisk = !unitName.equalsIgnoreCase(ModelConfig.CHAT_UNIT);
            promptMetadata = this.generatePrompt(query, searchTopK);
        }

        if (null == promptMetadata) {
            Logger.w(this.getClass(), "#performKnowledgeQA - "
                    + baseInfo.name + " - Generate prompt failed in channel: " + channelCode);
            return false;
        }

        // 优化提示词
        String prompt = this.optimizePrompt(unitName, promptMetadata, query, knowledgeCategories, attachmentContents);

        // 查询知识概念
        List<GeneratingRecord> paraphrases = this.makeParaphrases(knowledgeCategories,
                ModelConfig.getPromptLengthLimit(unitName) - prompt.length(), unitName);

        final KnowledgeQAResult result = new KnowledgeQAResult(query, prompt);

        if (unit.getCapability().getName().equals(ModelConfig.BAIZE_NEXT_UNIT)) {
            // 特定单元执行 conversation
            this.service.converseBy(channel, unit, prompt, new ConversationListener() {
                @Override
                public void onConversation(AIGCChannel channel, AIGCConversationResponse response) {
                    result.conversationResponse = response;
                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode errorCode) {
                    listener.onFailed(channel, errorCode);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - "
                            + baseInfo.name + " - Single conversation failed: "
                            + errorCode.code);
                }
            });
        }
        else {
            // 执行 Generate Text
            Logger.d(KnowledgeBase.class, "#performKnowledgeQA - " + baseInfo.name +
                    "\n----------------------------------------\n" +
                    prompt +
                    "\n----------------------------------------");
            final List<KnowledgeSource> sources = promptMetadata.mergeSources();
            this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                    paraphrases, null, true, new GenerateTextListener() {
                @Override
                public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                    // 结果记录
                    result.record = record;

                    // 来源
                    result.sources.addAll(sources);

                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                    listener.onFailed(channel, stateCode);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - " +
                            baseInfo.name + " - Generates text failed: " + stateCode.code);
                }
            });
        }

        return true;
    }

    private PromptMetadata extractDocumentContent(String query, int topN) {
        List<String> wordList = new ArrayList<>();
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
        List<Keyword> keywordList = analyzer.analyze(query, 10);
        for (Keyword keyword : keywordList) {
            wordList.add(keyword.getWord());
        }

        List<String[]> fileResults = this.storage.matchKnowledgeDocWithFileName(this.baseInfo.name, wordList);
        if (null == fileResults) {
            return null;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#extractDocumentContent - "
                    + baseInfo.name + " - File num: " + fileResults.size());
        }

        List<FileNameMatching> matchingList = new ArrayList<>();
        for (String[] data : fileResults) {
            FileNameMatching matching = new FileNameMatching(data);
            // 计算匹配词数量
            int num = 0;
            for (String word : wordList) {
                if (matching.fileName.contains(word)) {
                    ++num;
                }
            }
            matching.numMatching = num;
            matchingList.add(matching);
        }

        // 降序排序文件
        Collections.sort(matchingList);

        PromptMetadata metadata = new PromptMetadata();

        for (int i = 0; i < Math.min(topN, matchingList.size()); ++i) {
            StringBuilder content = new StringBuilder();

            FileNameMatching fm = matchingList.get(i);
            GetFile getFile = new GetFile(this.authToken.getDomain(), fm.fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null == fileLabelJson) {
                Logger.w(this.getClass(), "#extractDocumentContent - "
                        + baseInfo.name + " - Not find file: " + fm.fileCode);
                continue;
            }

            FileLabel fileLabel = new FileLabel(fileLabelJson);

            if (fileLabel.getFileSize() >= ModelConfig.BAIZE_CONTEXT_LIMIT - 50) {
                Logger.d(this.getClass(), "#extractDocumentContent - "
                        + baseInfo.name + " - File size overflow: " + fileLabel.getFileSize());
                continue;
            }

            if (fileLabel.getFileType() == FileType.TEXT
                    || fileLabel.getFileType() == FileType.TXT
                    || fileLabel.getFileType() == FileType.MD
                    || fileLabel.getFileType() == FileType.LOG) {
                String fullpath = this.fileStorage.notify(new LoadFile(fileLabel.getDomain().getName(), fileLabel.getFileCode()));
                if (null == fullpath) {
                    Logger.w(this.getClass(), "#extractDocumentContent - "
                            + baseInfo.name + " - Load file error: " + fileLabel.getFileCode());
                    continue;
                }

                try {
                    List<String> lines = Files.readAllLines(Paths.get(fullpath));
                    for (String text : lines) {
                        if (text.trim().length() < 3) {
                            continue;
                        }

                        // 记录内容
                        content.append(text).append("\n");
                        // 判断长度
                        if (content.length() > ModelConfig.BAIZE_CONTEXT_LIMIT) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#extractDocumentContent - "
                            + baseInfo.name + " - Read file error: " + fullpath);
                }

                if (content.length() < 3) {
                    Logger.w(this.getClass(), "#extractDocumentContent - "
                            + baseInfo.name + " - No content: " + this.baseInfo.name);
                    continue;
                }

                // 将所有内容推送给模型推理
                String prompt = Consts.formatExtractContent(content.toString(), query);
                GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, prompt,
                        new GeneratingOption(), null, null);
                if (null == result) {
                    Logger.w(this.getClass(), "#extractDocumentContent - "
                            + baseInfo.name + " - Unit error, no answer: " + this.baseInfo.name);
                    continue;
                }

                if (result.answer.contains(Consts.NO_CONTENT_SENTENCE)) {
                    Logger.d(this.getClass(), "#extractDocumentContent - "
                            + baseInfo.name + " - No content: " + result);
                    continue;
                }

                // 记录
                if (null == metadata.answer) {
                    metadata.answer = result.answer;
                }
                else {
                    metadata.answer = metadata.answer + "\n" + result;
                }

                metadata.addDocumentMetadata(fm.fileCode);
            }
            else {
                Logger.w(this.getClass(), "#extractDocumentContent - " + baseInfo.name + " - File type error: "
                        + fileLabel.getFileName() + " - "
                        + fileLabel.getFileType().getMimeType());
            }
        }

        return metadata;
    }

    private class FileNameMatching implements Comparable<FileNameMatching> {

        protected String fileCode;
        protected String fileName;

        /**
         * 匹配关键词数量
         */
        protected int numMatching;

        public FileNameMatching(String[] data) {
            this.fileCode = data[0];
            this.fileName = data[1];
        }

        @Override
        public int compareTo(FileNameMatching fm) {
            return fm.numMatching - this.numMatching;
        }
    }

    private List<GeneratingRecord> makeParaphrases(List<String> knowledgeCategories, int lengthLimit,
                                                   String unitName) {
        if (null == knowledgeCategories || lengthLimit < 1) {
            return null;
        }

        List<KnowledgeParaphrase> paraphraseList = new ArrayList<>();
        for (String category : knowledgeCategories) {
            List<KnowledgeParaphrase> list = this.storage.readKnowledgeParaphrases(category);
            if (list.isEmpty()) {
                continue;
            }
            paraphraseList.addAll(list);
        }

        List<GeneratingRecord> result = new ArrayList<>();
        int total = 0;
        for (KnowledgeParaphrase paraphrase : paraphraseList) {
            total += paraphrase.getWord().length() + paraphrase.getParaphrase().length();
            if (total >= lengthLimit) {
                break;
            }
            GeneratingRecord record = new GeneratingRecord(unitName,
                    paraphrase.getWord(), paraphrase.getParaphrase());
            result.add(record);
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * 处理查询附件。
     *
     * @param records
     * @return 返回仅当次需要处理的文本。
     */
    private List<String> processQueryAttachments(List<GeneratingRecord> records) {
        List<String> contents = new ArrayList<>();
        List<FileLabel> fileLabels = new ArrayList<>();

        for (GeneratingRecord record : records) {
            if (record.hasQueryAddition()) {
                for (String text : record.queryAdditions) {
                    String[] buf = text.split("\n");
                    for (String s : buf) {
                        if (s.trim().length() <= 1) {
                            continue;
                        }
                        contents.add(s);
                    }
                }
            }

            if (record.hasQueryFile()) {
                fileLabels.addAll(record.queryFileLabels);
            }
        }

        // 判断是否有重复文件
        if (null != this.resource.docList && !this.resource.docList.isEmpty()) {
            for (KnowledgeDoc doc : this.resource.docList) {
                for (int i = 0; i < fileLabels.size(); ++i) {
                    FileLabel fileLabel = fileLabels.get(i);
                    if (fileLabel.getFileCode().equals(doc.fileCode)) {
                        // 文件码相同
                        fileLabels.remove(fileLabel);
                        break;
                    }
                    else if (fileLabel.getFileName().equalsIgnoreCase(doc.getFileLabel().getFileName())) {
                        // 文件名相同
                        fileLabels.remove(fileLabel);
                        break;
                    }
                }
            }
        }

        if (!fileLabels.isEmpty()) {
            for (FileLabel fileLabel : fileLabels) {
                KnowledgeDoc doc = this.importKnowledgeDoc(fileLabel.getFileCode(), KnowledgeDoc.SPLITTER_LINE);
                if (null == doc) {
                    Logger.w(this.getClass(), "#processQueryAttachments - Import knowledge doc failed - fileCode: "
                            + fileLabel.getFileCode());
                }
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#processQueryAttachments - Process query attachments - num: "
                    + contents.size() + "|" + fileLabels.size());
        }

        return contents;
    }

    private String optimizePrompt(String unitName, PromptMetadata promptMetadata, String query,
                                  List<String> knowledgeCategories, List<String> attachmentContents) {
        int totalWords = 0;
        String[] lines = promptMetadata.prompt.split("\n");
        LinkedList<String> lineList = new LinkedList<>();
        for (String line : lines) {
            if (line.trim().length() == 0) {
                continue;
            }

            if (lineList.contains(line)) {
                // 删除重复
                continue;
            }
            totalWords += line.length();
            lineList.add(line);
        }

        final int maxWords = ModelConfig.getPromptLengthLimit(unitName) - 60;
        if (totalWords < maxWords) {
            // 补充内容
            // 提取第一行
            String first = lineList.pollFirst();

            // 先处理附件内容
            if (null != attachmentContents && !attachmentContents.isEmpty()) {
                // 读取附件内容
                StringBuilder contentBuf = new StringBuilder();
                for (String content : attachmentContents) {
                    if (contentBuf.length() + content.length() > ModelConfig.BAIZE_CONTEXT_LIMIT) {
                        break;
                    }
                    contentBuf.append(content).append("\n");
                }

                Logger.d(KnowledgeBase.class, "#optimizePrompt - Use attachment content - length: "
                        + contentBuf.length());

                String contentPrompt = Consts.formatQuestion(contentBuf.toString(), query);
                // 对内容进行推理
                GeneratingRecord generating = this.service.syncGenerateText(authToken, ModelConfig.CHAT_UNIT, contentPrompt,
                        new GeneratingOption());
                String result = (null != generating) ? generating.answer : null;
                if (null != result) {
                    // 添加结果
                    lineList.addFirst(result);
                    // 添加源
                    promptMetadata.addMetadata(contentBuf.toString());
                    // 更新长度
                    totalWords += result.length();

                    Logger.d(KnowledgeBase.class, "#optimizePrompt - Use attachment content - result length: "
                            + result.length() + ", total: " + totalWords);
                }
            }

            // 按照分类读取文章
            List<KnowledgeArticle> articleList = new ArrayList<>();

            boolean allCategories = false;
            if (null != knowledgeCategories && !knowledgeCategories.isEmpty()) {
                allCategories = knowledgeCategories.get(0).equals("*");

                if (knowledgeCategories.get(0).equals("-")) {
                    // "-" 符号表示不使用分类
                    knowledgeCategories.clear();
                }
            }

            if (allCategories) {
                // 从数据库里匹配文章
                TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
                List<Keyword> keywords = analyzer.analyze(query, 5);
                if (!keywords.isEmpty()) {
                    // 根据关键字匹配文章
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

                    // 按照命中的关键词数量进行从高到低排序
                    articleList = this.sortArticles(articleList, keywords);

                    Logger.d(KnowledgeBase.class, "#optimizePrompt - Matching articles [All] - num:" + articleList.size());
                }
            }
            else if (null != knowledgeCategories && !knowledgeCategories.isEmpty()) {
                for (String category : knowledgeCategories) {
                    List<KnowledgeArticle> list = this.storage.readKnowledgeArticles(category);
                    articleList.addAll(list);
                }

                Logger.d(KnowledgeBase.class, "#optimizePrompt - Matching articles - num:" + articleList.size());
            }
            else {
                Logger.d(KnowledgeBase.class, "#optimizePrompt - No matching articles");
            }

            if (!articleList.isEmpty() && totalWords < maxWords) {
                for (KnowledgeArticle article : articleList) {
                    // 排除重复文章
                    if (promptMetadata.containsMetadata(article)) {
                        continue;
                    }

                    // 对内容进行解析
                    String[] data = article.content.split("\n");
                    StringBuilder buf = new StringBuilder();
                    for (String text : data) {
                        if (text.trim().length() <= 1) {
                            continue;
                        }
                        buf.append(text).append("\n");
                        if (buf.length() >= ModelConfig.BAIZE_CONTEXT_LIMIT) {
                            break;
                        }
                    }
                    buf.delete(buf.length() - 1, buf.length());

                    String articlePrompt = Consts.formatQuestion(buf.toString(), Consts.KNOWLEDGE_SECTION_PROMPT);
                    // 对文章内容进行推理
                    GeneratingRecord generating = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_X_UNIT, articlePrompt,
                            new GeneratingOption());
                    String articleResult = (null != generating) ? generating.answer : null;
                    if (null == articleResult) {
                        articleResult = article.summarization;
                    }

                    if (null != articleResult) {
                        // 将结果加入上下文
                        lineList.addFirst(articleResult);
                        // 加入源
                        promptMetadata.addMetadata(article);
                        // 更新内容大小
                        totalWords += articleResult.length();
                        if (totalWords >= maxWords) {
                            break;
                        }
                    }
                }
            }

            // 恢复第一行
            lineList.addFirst(first);
        }
        else if (totalWords > maxWords) {
            // 内容超过上下文限制
            // 提取第一行
            String first = lineList.pollFirst();
            // 提取最后一行
            String last = lineList.pollLast();

            int numWords = first.length() + last.length();
            List<String> newList = new ArrayList<>();
            for (String line : lineList) {
                numWords += line.length();
                if (numWords >= maxWords) {
                    break;
                }
                // 添加
                newList.add(line);
            }

            // 清空
            lineList.clear();
            // 重填
            lineList.addAll(newList);

            // 恢复第一行
            lineList.addFirst(first);
            // 恢复最后一行
            lineList.addLast(last);
        }

        if (lineList.size() <= 2) {
            return Consts.NO_CONTENT_SENTENCE;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0, len = lineList.size(); i < len; ++i) {
            String line = lineList.get(i);
            buf.append(line).append("\n");
            if (i == len - 2) {
                buf.append("\n");
            }
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

    public KnowledgeQAProgress getActivateProgress() {
        return this.activateProgress;
    }

    /**
     * 使用全量数据进行问答。
     *
     * @param channelCode
     * @param unitName
     * @param matchingSchema
     * @param listener
     * @return
     */
    public KnowledgeQAProgress performKnowledgeQA(String channelCode, String unitName,
                                      KnowledgeMatchingSchema matchingSchema, KnowledgeQAListener listener) {
        if (this.qaLock.get()) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Knowledge base is performing QA - " + channelCode);
            return null;
        }
        this.qaLock.set(true);

        Logger.d(this.getClass(), "#performKnowledgeQA [section/category] - " + channelCode +
                "/" + unitName + "/" + matchingSchema.getSectionQuery() + "/"
                + matchingSchema.getComprehensiveQuery() + "/" + matchingSchema.getCategory());

        final AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Can NOT find channel: " + channelCode);
            this.qaLock.set(false);
            return null;
        }

        final AIGCUnit unit = (Agent.getInstance() != null) ? Agent.getInstance().selectUnit(unitName)
                : this.service.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Select unit error: " + unitName);
            this.qaLock.set(false);
            return null;
        }

        List<KnowledgeArticle> articles = this.storage.readKnowledgeArticles(matchingSchema.getCategory(),
                1, System.currentTimeMillis());
//        this.storage.matchKnowledgeArticles(this.authToken.getDomain(), this.authToken.getContactId(), category);
        if (articles.isEmpty()) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Article list is empty");
            this.qaLock.set(false);
            return null;
        }

        this.activateProgress = new KnowledgeQAProgress(channel, unitName);

        // 倒序
        Collections.reverse(articles);

        // 总文本数量
        List<String> contentList = new ArrayList<>();
        for (int i = 0; i < articles.size() && i < matchingSchema.getMaxArticles(); ++i) {
            KnowledgeArticle article = articles.get(i);
            contentList.addAll(trimArticleContent(article.content, 1100));
        }

        // 定义总进度
        this.activateProgress.defineTotalProgress(contentList.size() + 1);

        AtomicInteger count = new AtomicInteger(0);
        List<GeneratingRecord> pipelineRecordList = new ArrayList<>();

        (new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.d(KnowledgeBase.class, "#performKnowledgeQA - Waiting for section query");

                synchronized (qaLock) {
                    try {
                        // 等待通道的文章结果
                        qaLock.wait((long) contentList.size() * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Logger.d(KnowledgeBase.class, "#performKnowledgeQA - Section query finish: " + pipelineRecordList.size());

                if (!pipelineRecordList.isEmpty()) {
                    final int limit = 1500;
                    StringBuilder buf = new StringBuilder();
                    for (GeneratingRecord record : pipelineRecordList) {
                        String[] paragraphs = record.answer.split("\n");
                        // 跳过第一行
                        for (int i = 1; i < paragraphs.length; ++i) {
                            String text = paragraphs[i].trim();
                            if (text.length() <= 2) {
                                continue;
                            }

                            // 是否是 1. 2. 形式开头
                            if (TextUtils.startsWithNumberSign(text)) {
                                text = text.substring(2).trim();
                            }

                            buf.append(text).append("\n");
                            if (buf.length() > limit) {
                                break;
                            }
                        }

                        if (buf.length() > limit) {
                            break;
                        }
                    }
                    buf.delete(buf.length() - 1, buf.length());

                    String prompt = Consts.formatQuestion(buf.toString(), matchingSchema.getComprehensiveQuery());

                    service.generateText(channel, unit, matchingSchema.getComprehensiveQuery(), prompt, new GeneratingOption(),
                            null, 0, null, null, false,
                            new GenerateTextListener() {
                        @Override
                        public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                            activateProgress.setCode(AIGCStateCode.Ok.code);

                            KnowledgeQAResult result = new KnowledgeQAResult(matchingSchema.getComprehensiveQuery(), prompt);
                            result.record = record;
                            activateProgress.setResult(result);

                            int percent = activateProgress.updateProgress(1);
                            Logger.d(KnowledgeBase.class, "#performKnowledgeQA [onGenerated] - Progress percent: " + percent + "%");

                            // 回调
                            listener.onCompleted(channel, result);

                            // 更新锁状态
                            qaLock.set(false);
                        }

                        @Override
                        public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                            activateProgress.setCode(stateCode.code);
                            int percent = activateProgress.updateProgress(1);
                            Logger.d(KnowledgeBase.class, "#performKnowledgeQA [onFailed] - Progress percent: " + percent + "%");

                            // 回调
                            listener.onFailed(channel, stateCode);

                            // 更新锁状态
                            qaLock.set(false);
                        }
                    });
                }
                else {
                    activateProgress.setCode(AIGCStateCode.NoData.code);
                    int percent = activateProgress.updateProgress(1);
                    Logger.d(KnowledgeBase.class, "#performKnowledgeQA - Progress percent: " + percent + "%");

                    // 回调
                    listener.onFailed(channel, AIGCStateCode.NoData);

                    // 更新锁状态
                    qaLock.set(false);
                }
            }
        })).start();

        for (String text : contentList) {
            // 格式化提示词
            String prompt = Consts.formatQuestion(text, matchingSchema.getSectionQuery());

            this.service.generateText(channel, unit, matchingSchema.getSectionQuery(),
                prompt, new GeneratingOption(), null, 0, null, null,
                    false, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                        synchronized (pipelineRecordList) {
                            pipelineRecordList.add(record);
                        }

                        // 更新进度
                        count.incrementAndGet();
                        int percent = activateProgress.updateProgress(1);
                        Logger.d(KnowledgeBase.class, "#performKnowledgeQA [onGenerated] - Progress percent: "
                                + percent + "% - " + count.get() + "/" + contentList.size());

                        if (count.get() == contentList.size()) {
                            synchronized (qaLock) {
                                qaLock.notifyAll();
                            }
                        }
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        // 更新进度
                        count.incrementAndGet();
                        int percent = activateProgress.updateProgress(1);
                        Logger.d(KnowledgeBase.class, "#performKnowledgeQA [onFailed] - Progress percent: "
                                + percent + "% - " + count.get() + "/" + contentList.size());

                        if (count.get() == contentList.size()) {
                            synchronized (qaLock) {
                                qaLock.notifyAll();
                            }
                        }
                    }
                });
        }

        return activateProgress;
    }

    private List<String> trimArticleContent(String content, int maxWords) {
        List<String> list = new ArrayList<>();

        int limit = maxWords;
        if (content.length() > maxWords) {
            limit = (int) Math.floor((float) content.length() * 0.5);
            if (limit > maxWords) {
                limit = (int) Math.floor((float) limit * 0.5);
            }
        }

        StringBuilder buf = new StringBuilder();
        int num = 0;
        String[] array = content.split("\n");
        for (String text : array) {
            num += text.length();
            buf.append(text).append("\n");
            if (num >= limit) {
                list.add(buf.toString());
                num = 0;
                buf = new StringBuilder();
            }
        }

        if (num > 0) {
            list.add(buf.toString());
        }

        return list;
    }

    private PromptMetadata generatePrompt(String query, int topK) {
        if (!this.resource.checkUnit()) {
            Logger.w(this.getClass(),"#generatePrompt - No unit for knowledge base");
            return null;
        }

        JSONObject payload = new JSONObject();
        // 检索库
        payload.put("pathKey", (KnowledgeScope.Private == this.scope) ?
                Long.toString(this.authToken.getContactId()) : this.authToken.getDomain());
        payload.put("contactId", this.authToken.getContactId());
        payload.put("name", this.baseInfo.name);
        payload.put("query", query);
        payload.put("topK", topK);
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
        PromptMetadata promptMetadata = null;
        try {
            promptMetadata = new PromptMetadata(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return promptMetadata;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.baseInfo.name);
        json.put("profile", this.getProfile().toJSON());
        json.put("locked", this.lock.get());
        json.put("numDocuments", (null != this.resource.docList) ? this.resource.docList.size() : 0);
        json.put("numArticles", (null != this.resource.articleList) ? this.resource.articleList.size() : 0);
        return json;
    }


    public class KnowledgeResource {

        private List<KnowledgeDoc> docList;

        protected long listDocTime;

        private List<KnowledgeArticle> articleList;

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

        public void clearDocs() {
            if (null == this.docList) {
                this.docList = new ArrayList<>();
            }
            else {
                this.docList.clear();
            }
        }

        public void appendDocs(List<KnowledgeDoc> list) {
            if (null == this.docList) {
                this.docList = new ArrayList<>();
            }

            for (KnowledgeDoc doc : list) {
                if (this.docList.contains(doc)) {
                    this.docList.remove(doc);
                }

                this.docList.add(doc);
            }
        }

        public void appendDoc(KnowledgeDoc doc) {
            if (null == this.docList) {
                this.docList = new ArrayList<>();
            }

            if (this.docList.contains(doc)) {
                this.docList.remove(doc);
            }

            this.docList.add(doc);
        }

        public void removeDoc(KnowledgeDoc doc) {
            if (null == this.docList) {
                return;
            }

            this.docList.remove(doc);
        }

        public KnowledgeDoc removeDoc(String fileCode) {
            if (null == this.docList) {
                return null;
            }

            for (KnowledgeDoc doc : this.docList) {
                if (doc.fileCode.equals(fileCode)) {
                    this.docList.remove(doc);
                    return doc;
                }
            }
            return null;
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

    public class PromptMetadata {

        public String prompt;

        public List<Metadata> metadataList;

        private String answer;

        public PromptMetadata() {
            this.metadataList = new ArrayList<>();
        }

        public PromptMetadata(JSONObject json) {
            this.prompt = json.getString("prompt");
            this.metadataList = new ArrayList<>();
            if (json.has("metadata")) {
                JSONArray array = json.getJSONArray("metadata");
                for (int i = 0; i < array.length(); ++i) {
                    Metadata metadata = new Metadata(array.getJSONObject(i));
                    this.metadataList.add(metadata);
                }
            }
        }

        public boolean isEmpty() {
            return this.metadataList.isEmpty();
        }

        public void addMetadata(KnowledgeArticle article) {
            this.metadataList.add(new Metadata(Metadata.ARTICLE_PREFIX + article.getId(), 300));
        }

        public void addMetadata(String text) {
            this.metadataList.add(new Metadata(Metadata.TEXT_PREFIX + text, 200));
        }

        public void addDocumentMetadata(String fileCode) {
            this.metadataList.add(new Metadata(Metadata.DOCUMENT_PREFIX + fileCode, 300));
        }

        public boolean containsMetadata(KnowledgeArticle article) {
            return this.metadataList.contains(article);
        }

        public PromptMetadata generatePrompt(String query) {
            if (null == this.answer) {
                return null;
            }

            String prompt = Consts.formatQuestion(this.answer, query);
            PromptMetadata metadata = new PromptMetadata();
            metadata.prompt = prompt;
            metadata.metadataList.addAll(this.metadataList);
            return metadata;
        }

        public void mergeAnswer(PromptMetadata other) {
            if (null == other.answer) {
                return;
            }

            StringBuilder buf = new StringBuilder();
            String[] lines = this.prompt.split("\n");
            // 首行
            buf.append(lines[0]).append("\n");
            // 合并答案
            buf.append(other.answer);
            // 逐条恢复
            for (int i = 1; i < lines.length; ++i) {
                buf.append(lines[i]).append("\n");
            }
            if (buf.length() > 2) {
                buf.delete(buf.length() - 1, buf.length());
            }

            // 新提示词
            this.prompt = buf.toString();

            // 添加源
            this.metadataList.addAll(other.metadataList);
        }

        /**
         * 将相同的源进行合并。
         *
         * @return
         */
        public List<KnowledgeSource> mergeSources() {
            List<KnowledgeSource> list = new ArrayList<>();

            List<Metadata> mdList = new ArrayList<>();
            for (Metadata metadata : this.metadataList) {
                if (mdList.contains(metadata)) {
                    continue;
                }
                mdList.add(metadata);
            }

            for (Metadata metadata : mdList) {
                KnowledgeSource source = metadata.getSource();
                if (null != source) {
                    list.add(source);
                }
            }

            return list;
        }

        public class Metadata {

            public final static String DOCUMENT_PREFIX = "./tmp/";

            public final static String ARTICLE_PREFIX = "article:";

            public final static String TEXT_PREFIX = "text:";

            public String content;

            public String source;

            public float score;

            public Metadata(JSONObject json) {
                this.content = json.has("content") ? json.getString("content") : null;
                this.source = json.getString("source");
                String s = json.getString("score");
                this.score = Float.parseFloat(s);
            }

            public Metadata(String source, float score) {
                this.source = source;
                this.score = score;
            }

            private String getSourceKey() {
                if (this.source.startsWith(DOCUMENT_PREFIX)) {
                    int end = this.source.lastIndexOf(".");
                    if (end <= 0) {
                        end = this.source.length();
                    }
                    return this.source.substring(DOCUMENT_PREFIX.length(), end);
                }
                else if (this.source.startsWith(ARTICLE_PREFIX)) {
                    return this.source.substring(ARTICLE_PREFIX.length());
                }
                else {
                    return null;
                }
            }

            public KnowledgeSource getSource() {
                // 判断是文档还是文章
                if (this.source.startsWith(DOCUMENT_PREFIX)) {
                    int end = this.source.lastIndexOf(".");
                    if (end <= 0) {
                        end = this.source.length();
                    }
                    String fileCode = this.source.substring(DOCUMENT_PREFIX.length(), end);
                    KnowledgeDoc doc = getKnowledgeDocByFileCode(fileCode);
                    if (null == doc) {
                        return null;
                    }

                    return new KnowledgeSource(doc);
                }
                else if (this.source.startsWith(ARTICLE_PREFIX)) {
                    String id = this.source.substring(ARTICLE_PREFIX.length());
                    try {
                        long articleId = Long.parseLong(id);
                        KnowledgeArticle article = storage.readKnowledgeArticle(articleId);
                        if (null == article) {
                            return null;
                        }

                        return new KnowledgeSource(article);
                    } catch (Exception e) {
                        // Nothing
                    }
                }
                else if (this.source.startsWith(TEXT_PREFIX)) {
                    String text = this.source.substring(TEXT_PREFIX.length());
                    return new KnowledgeSource(text);
                }

                return null;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Metadata) {
                    if (this.source.equals(((Metadata) obj).source)) {
                        return true;
                    }

                    // File Code 或 ID 重复
                    String key = this.getSourceKey();
                    String otherKey = ((Metadata) obj).getSourceKey();
                    if (null != key && null != otherKey) {
                        return key.equals(otherKey);
                    }
                }
                return false;
            }

            @Override
            public int hashCode() {
                return this.source.hashCode();
            }
        }
    }
}
