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

package cube.service.client.task;

import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.action.FileStorageAction;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 获取文件信息。
 */
public class GetFileTask extends ClientTask {

    public GetFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        String fileCode = actionDialect.getParamAsString("fileCode");
        boolean transmitting = actionDialect.containsParam("transmitting") && actionDialect.getParamAsBool("transmitting");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.GetFile.name);
        notification.put("domain", domain);
        notification.put("fileCode", fileCode);
        notification.put("transmitting", transmitting);

        ActionDialect response = new ActionDialect(ClientAction.GetFile.name);
        copyNotifier(response);

        // 获取文件信息
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            cellet.speak(talkContext, response);
            return;
        }

        JSONObject responseData = (JSONObject) result;
        if (transmitting && responseData.has("fullPath")) {
            String fullPath = responseData.getString("fullPath");
            responseData.remove("fullPath");
            // 发送文件数据
            this.transmitFile(fullPath, responseData.getString("fileName"));
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("fileLabel", responseData);
        cellet.speak(talkContext, response);
    }

    private void transmitFile(String fullPath, String streamName) {
        final PrimitiveOutputStream stream = cellet.speakStream(talkContext, streamName);
        (new Thread() {
            @Override
            public void run() {
                FileInputStream fis = null;

                File file = new File(fullPath);

                try {
                    fis = new FileInputStream(file);
                    byte[] bytes = new byte[4096];
                    int length = 0;
                    while ((length = fis.read(bytes)) > 0) {
                        stream.write(bytes, 0, length);
                    }

                    stream.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != fis) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }

                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }).start();
    }
}
