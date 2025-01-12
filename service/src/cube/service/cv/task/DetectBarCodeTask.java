/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.cv.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.BarCodeInfo;
import cube.common.state.CVStateCode;
import cube.service.ServiceTask;
import cube.service.cv.CVCellet;
import cube.service.cv.CVService;
import cube.service.cv.listener.DetectBarCodeListener;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 检测并解码条码。
 */
public class DetectBarCodeTask extends ServiceTask {

    public DetectBarCodeTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        final long start = System.currentTimeMillis();

        try {
            boolean success = service.detectBarCode(token, JSONUtils.toStringList(packet.data.getJSONArray("list")),
                    new DetectBarCodeListener() {
                        @Override
                        public void onCompleted(List<BarCodeInfo> barCodeInfos) {
                            JSONArray barCodeInfoArray = new JSONArray();
                            for (BarCodeInfo info : barCodeInfos) {
                                barCodeInfoArray.put(info.toJSON());
                            }

                            JSONObject responseJson = new JSONObject();
                            responseJson.put("result", barCodeInfoArray);
                            responseJson.put("elapsed", System.currentTimeMillis() - start);

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
