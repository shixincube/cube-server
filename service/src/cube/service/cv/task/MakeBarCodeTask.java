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
import cube.common.entity.BarCode;
import cube.common.entity.FileLabel;
import cube.common.state.CVStateCode;
import cube.service.ServiceTask;
import cube.service.cv.CVCellet;
import cube.service.cv.CVService;
import cube.service.cv.ToolKit;
import cube.service.cv.listener.CombineBarCodeListener;
import cube.util.PrintUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 制作条码。
 */
public class MakeBarCodeTask extends ServiceTask {

    public MakeBarCodeTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        JSONArray result = new JSONArray();
        int amount = 0;

        boolean merge = packet.data.has("merge") && packet.data.getBoolean("merge");
        String layout = packet.data.has("layout") ? packet.data.getString("layout") : "tile";
        String size = packet.data.has("size") ? packet.data.getString("size") : "small";
        String paper = packet.data.has("paper") ? packet.data.getString("paper") : null;

        try {
            if (merge) {
                List<BarCode> list = new ArrayList<>();
                JSONArray array = packet.data.getJSONArray("list");
                if (array.length() == 0) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, CVStateCode.InvalidData.code, new JSONObject()));
                    markResponseTime();
                    return;
                }

                for (int i = 0; i < array.length(); ++i) {
                    BarCode barCode = new BarCode(array.getJSONObject(i));
                    barCode.setContainer(BarCode.parseContainer(size));
                    list.add(barCode);
                }

                if (layout.equalsIgnoreCase("single")) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#run - Use work endpoint");
                    }

                    boolean success = service.combineBarcodes(token, list, new CombineBarCodeListener() {
                        @Override
                        public void onCompleted(List<BarCode> barCodeList, FileLabel fileLabel) {
                            result.put(fileLabel.toJSON());
                            JSONObject responseJson = new JSONObject();
                            responseJson.put("list", result);
                            responseJson.put("amount", 1);
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, CVStateCode.Ok.code, responseJson));
                            markResponseTime();
                        }

                        @Override
                        public void onFailed(List<BarCode> barCodeList, CVStateCode stateCode) {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, stateCode.code, new JSONObject()));
                            markResponseTime();
                        }
                    });
                    if (!success) {
                        this.cellet.speak(this.talkContext,
                                this.makeResponse(dialect, packet, CVStateCode.IllegalOperation.code, new JSONObject()));
                        markResponseTime();
                        return;
                    }
                }
                else {
                    FileLabel fileLabel = ToolKit.getInstance().makeBarCodeA4Paper(token, list, layout);
                    if (null != fileLabel) {
                        ++amount;
                        result.put(fileLabel.toJSON());
                    }

                    JSONObject responseJson = new JSONObject();
                    responseJson.put("list", result);
                    responseJson.put("amount", amount);

                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, CVStateCode.Ok.code, responseJson));
                    markResponseTime();
                }
            }
            else {
                JSONArray array = packet.data.getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject info = array.getJSONObject(i);
                    BarCode barCode = new BarCode(info);
                    if (null != paper) {
                        FileLabel fileLabel = ToolKit.getInstance().makeBarCodePaper(token, barCode, PrintUtils.PaperA4Ultra);
                        if (null != fileLabel) {
                            ++amount;
                            result.put(fileLabel.toJSON());
                        }
                        else {
                            result.put(new JSONObject());
                        }
                    }
                    else {
                        FileLabel fileLabel = ToolKit.getInstance().makeBarCode(token, barCode);
                        if (null != fileLabel) {
                            ++amount;
                            result.put(fileLabel.toJSON());
                        }
                        else {
                            result.put(new JSONObject());
                        }
                    }
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("list", result);
                responseJson.put("amount", amount);

                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, CVStateCode.Ok.code, responseJson));
                markResponseTime();
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);

            JSONObject responseJson = new JSONObject();
            responseJson.put("list", result);
            responseJson.put("amount", amount);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, CVStateCode.Failure.code, responseJson));
            markResponseTime();
        }
    }
}
