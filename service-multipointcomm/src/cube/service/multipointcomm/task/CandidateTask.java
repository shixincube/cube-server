/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.action.MultipointCommAction;
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;
import cube.service.multipointcomm.signaling.CandidateSignaling;

/**
 * Candidate 信令任务。
 */
public class CandidateTask extends ServiceTask {

    public CandidateTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 解析信令
        CandidateSignaling candidate = new CandidateSignaling(packet.data);

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        // 处理 Candidate
        MultipointCommStateCode state = service.processCandidate(candidate);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MultipointCommAction.CandidateAck.name,
                        state.code, candidate.toCompactJSON()));
        markResponseTime();
    }
}
