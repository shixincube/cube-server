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

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.KnowledgeDoc;
import cube.common.entity.KnowledgeScope;
import cube.common.entity.ResetKnowledgeProgress;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.listener.ResetKnowledgeStoreListener;
import org.json.JSONObject;

import java.util.List;

/**
 * 重置知识库任务。
 */
public class ResetKnowledgeStoreTask extends ServiceTask {

    public ResetKnowledgeStoreTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        KnowledgeBase base = service.getKnowledgeBase(tokenCode);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        // 是否备份
        boolean backup = packet.data.has("backup") && packet.data.getBoolean("backup");

        ResetKnowledgeProgress progress = base.resetKnowledgeStore(backup, new ResetKnowledgeStoreListener() {
            @Override
            public void onStoreDeleted(long contactId, String domain, KnowledgeScope scope) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onStoreDeleted : " + base.getAuthToken().getContactId());
            }

            @Override
            public void onStoreDeleteFailed(AIGCStateCode stateCode) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onStoreDeleteFailed : "
                        + base.getAuthToken().getContactId() + " - code: " + stateCode);
            }

            @Override
            public void onKnowledgeDocActivated(List<KnowledgeDoc> originList, List<KnowledgeDoc> activatedList) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onKnowledgeDocActivated : " + base.getAuthToken().getContactId());
            }

            @Override
            public void onKnowledgeDocActivateFailed(List<KnowledgeDoc> originList, List<KnowledgeDoc> docList,
                                                     AIGCStateCode stateCode) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onKnowledgeDocActivateFailed : "
                        + base.getAuthToken().getContactId() + " - code: " + stateCode);
            }

            @Override
            public void onCompleted(List<KnowledgeDoc> originList, List<KnowledgeDoc> completionList) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onCompleted : " + base.getAuthToken().getContactId()
                    + " - completion/origin: " + completionList.size() + "/" + originList.size());
            }
        });

        if (null == progress) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, progress.toJSON()));
            markResponseTime();
        }
    }
}