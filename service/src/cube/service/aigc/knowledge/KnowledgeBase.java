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
import cube.service.aigc.listener.ChatListener;
import cube.service.aigc.listener.ConversationListener;
import cube.service.aigc.listener.KnowledgeQAListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

/**
 * 知识库操作。
 */
public class KnowledgeBase {

    public final static String EMPTY_BASE_ANSWER = "您的知识库里没有配置文档，您可以先向知识库里导入文档，再向我提问。";

    private AIGCService service;

    private AIGCStorage storage;

    private AuthToken authToken;

    private AbstractModule fileStorage;

    private KnowledgeRelation knowledgeRelation;

    public KnowledgeBase(AIGCService service, AIGCStorage storage, AuthToken authToken, AbstractModule fileStorage) {
        this.service = service;
        this.storage = storage;
        this.authToken = authToken;
        this.fileStorage = fileStorage;
        this.knowledgeRelation = new KnowledgeRelation();
    }

    /**
     * 获取知识库概况。
     *
     * @return
     */
    public KnowledgeProfile getProfile() {
        KnowledgeProfile profile = this.storage.readKnowledgeProfile(this.authToken.getContactId());
        if (null == profile) {
            // 没有记录，返回不可用的侧写
            profile = new KnowledgeProfile(0, this.authToken.getContactId(),
                    KnowledgeProfile.STATE_FORBIDDEN, 0);
        }
        return profile;
    }

    /**
     * 获取装载的知识库文档。
     *
     * @return
     */
    public List<KnowledgeDoc> getKnowledgeDocs() {
        List<KnowledgeDoc> list = this.storage.readKnowledgeDocList(this.authToken.getDomain(), this.authToken.getContactId());

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

        this.knowledgeRelation.docList = list;

        this.knowledgeRelation.checkUnit();

        return list;
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
        KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
        if (null == doc) {
            // 创建新文档
            doc = new KnowledgeDoc(Utils.generateSerialNumber(), this.authToken.getDomain(),
                    this.authToken.getContactId(), fileCode, true, -1);
            this.storage.writeKnowledgeDoc(doc);
        }

        if (null == doc.fileLabel) {
            GetFile getFile = new GetFile(this.authToken.getDomain(), fileCode);
            JSONObject fileLabelJson = this.fileStorage.notify(getFile);
            if (null == fileLabelJson) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Not find file: " + fileCode);
                return null;
            }

            FileLabel fileLabel = new FileLabel(fileLabelJson);
            doc.fileLabel = fileLabel;
        }

        KnowledgeDoc activatedDoc = doc;

        if (doc.activated) {
            if (!this.knowledgeRelation.checkUnit()) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Not find KnowledgeComprehension unit");
                this.storage.deleteKnowledgeDoc(fileCode);
                return null;
            }

            activatedDoc = this.activateKnowledgeDoc(doc);
            if (null == activatedDoc) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Unit return error");
                this.storage.deleteKnowledgeDoc(fileCode);
                return null;
            }
        }

        // 追加文档
        this.knowledgeRelation.appendDoc(activatedDoc);

        Logger.d(this.getClass(), "#importKnowledgeDoc - file code: " + fileCode);
        return activatedDoc;
    }

    public KnowledgeDoc removeKnowledgeDoc(String fileCode) {
        if (null == this.knowledgeRelation.docList) {
            this.getKnowledgeDocs();
        }

        KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
        if (null != doc) {
            this.storage.deleteKnowledgeDoc(fileCode);

            this.knowledgeRelation.removeDoc(doc);

            if (doc.activated) {
                if (this.knowledgeRelation.checkUnit()) {
                    KnowledgeDoc deactivatedDoc = this.deactivateKnowledgeDoc(doc,
                            this.knowledgeRelation.docList);
                    return deactivatedDoc;
                }
            }
        }
        else {
            this.knowledgeRelation.removeDoc(fileCode);
        }

        return doc;
    }

    /**
     * 激活指定知识库文档。
     *
     * @param doc
     * @return 返回已激活的文档。
     */
    public KnowledgeDoc activateKnowledgeDoc(KnowledgeDoc doc) {
        Packet packet = new Packet(AIGCAction.ActivateKnowledgeDoc.name, doc.toJSON());
        ActionDialect dialect = this.service.getCellet().transmit(this.knowledgeRelation.unit.getContext(),
                packet.toDialect(), 3 * 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(),"#activateKnowledgeDoc - Request unit error: "
                    + this.knowledgeRelation.unit.getCapability().getName());
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
        this.storage.updateKnowledgeDoc(activatedDoc);
        return activatedDoc;
    }

    /**
     * 解除指定的知识库文档。
     *
     * @param doc
     * @param newList
     * @return
     */
    public KnowledgeDoc deactivateKnowledgeDoc(KnowledgeDoc doc, List<KnowledgeDoc> newList) {
        JSONArray array = new JSONArray();
        for (KnowledgeDoc kd : newList) {
            array.put(kd.toJSON());
        }
        JSONObject payload = new JSONObject();
        payload.put("doc", doc.toJSON());
        payload.put("newList", array);

        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeDoc.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.knowledgeRelation.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#deactivateKnowledgeDoc - Request unit error: "
                    + this.knowledgeRelation.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deactivateKnowledgeDoc - Unit return error: " + state);
            return null;
        }

        KnowledgeDoc deactivatedDoc = new KnowledgeDoc(Packet.extractDataPayload(response));
        return deactivatedDoc;
    }

    public boolean performKnowledgeQA(String channelCode, String unitName, String query,
                                      KnowledgeQAListener listener) {
        Logger.d(this.getClass(), "#performKnowledgeQA - Channel: " + channelCode +
                "/" + unitName + "/" + query);

        final AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Can NOT find channel: " + channelCode);
            return false;
        }

        if (null == this.knowledgeRelation.docList) {
            this.getKnowledgeDocs();
        }
        // 如果没有设置知识库文档，返回提示
        if (this.knowledgeRelation.docList.isEmpty()) {
            Logger.d(this.getClass(), "#performKnowledgeQA - No knowledge doc in base: " + channelCode);
            this.service.getCellet().getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    KnowledgeQAResult result = new KnowledgeQAResult(query);
                    result.prompt = "";
                    AIGCChatRecord chatRecord = new AIGCChatRecord(query, EMPTY_BASE_ANSWER, System.currentTimeMillis());
                    result.chatRecord = chatRecord;
                    listener.onCompleted(channel, result);
                }
            });
            return true;
        }

        // 获取提示词
        final KnowledgeQAResult result = this.generatePrompt(query);
        if (null == result) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Generate prompt failed in channel: " + channelCode);
            return false;
        }

        AIGCUnit unit = this.service.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#performKnowledgeQA - Select unit error: " + unitName);
            return false;
        }

        if (unit.getCapability().getName().equals("MOSS")) {
            // MOSS 单元执行 conversation
            this.service.singleConversation(channel, unit, result.prompt, new ConversationListener() {
                @Override
                public void onConversation(AIGCChannel channel, AIGCConversationResponse response) {
                    result.conversationResponse = response;
                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel, int errorCode) {
                    listener.onFailed(channel);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - Single conversation failed: " + channelCode);
                }
            });
        }
        else {
            // 其他单元执行 chat
            this.service.singleChat(channel, unit, result.prompt, new ChatListener() {
                @Override
                public void onChat(AIGCChannel channel, AIGCChatRecord record) {
                    result.chatRecord = record;
                    listener.onCompleted(channel, result);
                }

                @Override
                public void onFailed(AIGCChannel channel) {
                    listener.onFailed(channel);
                    Logger.w(KnowledgeBase.class, "#performKnowledgeQA - Single chat failed: " + channelCode);
                }
            });
        }

        return true;
    }

    private KnowledgeQAResult generatePrompt(String query) {
        if (null == this.knowledgeRelation.docList) {
            this.getKnowledgeDocs();
        }

        JSONArray docArray = new JSONArray();
        for (KnowledgeDoc doc : this.knowledgeRelation.docList) {
            docArray.put(doc.toJSON());
        }

        if (!this.knowledgeRelation.checkUnit()) {
            Logger.w(this.getClass(),"#generatePrompt - No unit for knowledge base");
            return null;
        }

        JSONObject payload = new JSONObject();
        payload.put("contactId", this.authToken.getContactId());
        payload.put("query", query);
        payload.put("docList", docArray);
        Packet packet = new Packet(AIGCAction.GeneratePrompt.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(this.knowledgeRelation.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#generatePrompt - Request unit error: "
                    + this.knowledgeRelation.unit.getCapability().getName());
            return null;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#generatePrompt - Unit return error: " + state);
            return null;
        }

        KnowledgeQAResult result = new KnowledgeQAResult(query);

        JSONObject data = Packet.extractDataPayload(response);
        result.prompt = data.getString("prompt");
        return result;
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

    public class KnowledgeRelation {

        protected List<KnowledgeDoc> docList;

        protected AIGCUnit unit;

        public KnowledgeRelation() {
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
    }
}