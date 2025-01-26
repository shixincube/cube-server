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
import cube.common.entity.FileLabel;
import cube.common.entity.ObjectDetectionResult;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ObjectDetectionListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 图像里的物体检测。
 */
public class ObjectDetectionTask extends ServiceTask {

    public ObjectDetectionTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);

        if (null == token ||
                !packet.data.has("code") ||
                !packet.data.has("fileCodeList") ||
                !packet.data.has("sn")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        long sn = packet.data.getLong("sn");
        String channelCode = packet.data.getString("code");

        JSONArray array = packet.data.getJSONArray("fileCodeList");
        List<String> fileCodes = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            fileCodes.add(array.getString(i));
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        // 执行 Object Detection
        boolean success = service.objectDetection(channelCode, fileCodes, new ObjectDetectionListener() {
            @Override
            public void onCompleted(List<FileLabel> inputList, List<ObjectDetectionResult> resultList) {
                JSONArray resultArray = new JSONArray();
                for (ObjectDetectionResult result : resultList) {
                    resultArray.put(result.toJSON());
                }
                JSONObject data = new JSONObject();
                data.put("sn", sn);
                data.put("code", channelCode);
                data.put("list", resultArray);
                data.put("total", resultList.size());
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, data));
                markResponseTime();
            }

            @Override
            public void onFailed(List<FileLabel> sourceList, AIGCStateCode stateCode) {
                JSONObject data = new JSONObject();
                data.put("sn", sn);
                data.put("code", channelCode);
                data.put("stateCode", stateCode.code);
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, stateCode.code, data));
                markResponseTime();
            }
        });

        if (!success) {
            JSONObject data = new JSONObject();
            data.put("sn", sn);
            data.put("code", channelCode);
            data.put("fileCodeList", array);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, data));
            markResponseTime();
        }
    }
}
