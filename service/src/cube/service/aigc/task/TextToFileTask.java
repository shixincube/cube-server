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
import cell.util.Utils;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Language;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.TextToFileListener;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本生成文件任务。
 */
public class TextToFileTask extends ServiceTask {

    public TextToFileTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!packet.data.has("sn") || !packet.data.has("text") || !packet.data.has("files")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        AuthToken authToken = service.getToken(token);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        List<FileLabel> fileLabelList = new ArrayList<>();
        JSONArray array = packet.data.getJSONArray("files");
        for (int i = 0; i < array.length(); ++i) {
            String fileCode = array.getString(i);
            FileLabel fileLabel = service.getFile(authToken.getDomain(), fileCode);
            fileLabelList.add(fileLabel);
        }

        final long sn = packet.data.getLong("sn");
        String text = packet.data.getString("text");

        JSONObject responseData = new JSONObject();
        responseData.put("sn", sn);

        AIGCChannel channel = service.getChannelByToken(token);
        if (null == channel) {
            boolean english = TextUtils.isTextMainlyInEnglish(text);
            channel = service.createChannel(authToken, "Unknown", Utils.randomString(16),
                    english ? Language.English : Language.Chinese);
        }

        GeneratingRecord attachment = new GeneratingRecord(fileLabelList);
        boolean success = service.generateFile(channel, text, attachment, new TextToFileListener() {
            @Override
            public void onProcessing(AIGCChannel channel) {
            }

            @Override
            public void onCompleted(GeneratingRecord result) {
                responseData.put("result", result.toCompactJSON());
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
                markResponseTime();
            }

            @Override
            public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, stateCode.code, responseData));
                markResponseTime();
            }
        });

        if (!success) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, responseData));
            markResponseTime();
        }
    }
}
