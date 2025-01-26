/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.fileprocessor;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.action.FileProcessorAction;
import cube.common.state.FileProcessorStateCode;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONObject;

/**
 * 获取媒体源任务。
 */
public class GetMediaSourceTask extends DispatcherTask {

    public GetMediaSourceTask(FileProcessorCellet cellet, TalkContext talkContext, Primitive primitive
            , Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    @Override
    public void run() {
        String tokenCode = this.getTokenCode(this.getAction());
        if (null == tokenCode) {
            // 无令牌码
            ActionDialect response = this.makeResponse(new JSONObject(), StateCode.NoAuthToken, "No token code");
            this.cellet.speak(this.talkContext, response);
            return;
        }

        Packet packet = this.getRequest();

        String domainName = packet.data.getString("domain");
        String fileCode = packet.data.getString("fileCode");
        boolean secure = packet.data.getBoolean("secure");

        String url = MediaFileManager.getInstance().getMediaSourceURL(domainName, fileCode, secure);
        if (null == url) {
            ActionDialect response = this.packResponse(packet.data, FileProcessorStateCode.Failure.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("url", url);

        ActionDialect response = this.packResponse(payload, FileProcessorStateCode.Ok.code);
        this.cellet.speak(this.talkContext, response);
    }
}
