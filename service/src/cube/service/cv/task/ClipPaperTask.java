/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.ProcessedFile;
import cube.common.state.CVStateCode;
import cube.service.ServiceTask;
import cube.service.cv.CVCellet;
import cube.service.cv.CVService;
import cube.service.cv.listener.ClipPaperListener;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 剪裁并矫正纸张任务。
 */
public class ClipPaperTask extends ServiceTask {

    public ClipPaperTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, CVStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        CVService service = ((CVCellet) this.cellet).getService();
        AuthToken token = service.getToken(tokenCode);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, CVStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!packet.data.has("list")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, CVStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            boolean success = service.clipPaper(token, JSONUtils.toStringList(packet.data.getJSONArray("list")),
                    new ClipPaperListener() {
                        @Override
                        public void onCompleted(List<ProcessedFile> processedFiles, long elapsed) {
                            JSONArray array = new JSONArray();
                            for (ProcessedFile file : processedFiles) {
                                array.put(file.toJSON());
                            }

                            JSONObject responseJson = new JSONObject();
                            responseJson.put("result", array);
                            responseJson.put("elapsed", elapsed);

                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, CVStateCode.Ok.code, responseJson));
                            markResponseTime();
                        }

                        @Override
                        public void onFailed(List<String> fileCodes, CVStateCode stateCode) {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, stateCode.code, new JSONObject()));
                            markResponseTime();
                        }
                    });

            if (!success) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, CVStateCode.InvalidData.code, new JSONObject()));
                markResponseTime();
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, CVStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
    }
}
