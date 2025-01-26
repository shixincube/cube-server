/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.command.Command;
import cube.service.aigc.command.CommandListener;
import cube.service.aigc.command.SearchCommand;
import org.json.JSONObject;

/**
 * 执行命令。
 */
public class SearchCommandTask extends ServiceTask {

    public SearchCommandTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("keyword")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String keyword = packet.data.getString("keyword");

        SearchCommand command = new SearchCommand(keyword);

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        service.executeCommand(command, new CommandListener() {
            @Override
            public void onCompleted(Command command) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, command.toJSON()));
                markResponseTime();
            }

            @Override
            public void onFailed(int code) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, code, command.toJSON()));
                markResponseTime();
            }
        });
    }
}
