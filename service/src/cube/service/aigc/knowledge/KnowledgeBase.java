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
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库操作。
 */
public class KnowledgeBase {

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

        return list;
    }

    public KnowledgeDoc getKnowledgeDocByFileCode(String fileCode) {
        return this.storage.readKnowledgeDoc(fileCode);
    }

    public KnowledgeDoc importKnowledgeDoc(String fileCode) {
        KnowledgeDoc doc = this.getKnowledgeDocByFileCode(fileCode);
        if (null == doc) {
            // 创建新文档
            doc = new KnowledgeDoc(Utils.generateSerialNumber(), this.authToken.getDomain(),
                    this.authToken.getContactId(), fileCode, true);
            this.storage.writeKnowledgeDoc(doc);
        }

        GetFile getFile = new GetFile(this.authToken.getDomain(), fileCode);
        JSONObject fileLabelJson = this.fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#importKnowledgeDoc - Not find file: " + fileCode);
            return null;
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);
        doc.fileLabel = fileLabel;

        // 追加文档
        this.knowledgeRelation.appendDoc(doc);

        if (doc.activated) {
            if (null == this.knowledgeRelation.unit) {
                Logger.e(this.getClass(), "#importKnowledgeDoc - Not find KnowledgeComprehension unit");
                return null;
            }

            this.activateKnowledgeDoc(doc);
        }

        return doc;
    }

    public void activateKnowledgeDoc(KnowledgeDoc doc) {
        Packet packet = new Packet(AIGCAction.ActivateKnowledgeDoc.name, doc.toJSON());
        ActionDialect dialect = this.service.getCellet().transmit(this.knowledgeRelation.unit.getContext(),
                packet.toDialect());
        if (null == dialect) {
            Logger.w(this.getClass(),"#activateKnowledgeDoc - Request unit error: "
                    + this.knowledgeRelation.unit.getCapability().getName());
            return;
        }

        Packet response = new Packet(dialect);
        int state = Packet.extractCode(response);
        if (state != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#activateKnowledgeDoc - Unit return error: " + state);
            return;
        }

        JSONObject payload = Packet.extractDataPayload(response);
        if (!payload.has("fileCode")) {
            // 单元下载节点失败
        }
    }

    public void performKnowledgeQA(KnowledgeDoc doc, String modelName) {
        AIGCUnit unit = this.matchUnit(modelName);
        if (null == unit) {
            Logger.w(this.getClass(), "#importKnowledgeDoc - Select unit error: " + modelName);
            return;
        }
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

        public void appendDoc(KnowledgeDoc doc) {
            if (this.docList.contains(doc)) {
                return;
            }

            this.docList.add(doc);
        }
    }
}
