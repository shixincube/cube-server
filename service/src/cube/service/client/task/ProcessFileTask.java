/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import cell.util.log.Logger;
import cube.common.action.ClientAction;
import cube.common.entity.FileResult;
import cube.common.state.FileProcessorStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 处理文件任务。
 */
public class ProcessFileTask extends ClientTask {

    private Queue<StreamBundle> bundleQueue = new ConcurrentLinkedQueue<>();

    private AtomicBoolean queueWorking = new AtomicBoolean(false);

    public ProcessFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        String fileCode = actionDialect.getParamAsString("fileCode");
        String process = actionDialect.getParamAsString("process");

        JSONObject notification = new JSONObject();
        notification.put("action", process);
        notification.put("domain", domain);
        notification.put("fileCode", fileCode);

        if (actionDialect.containsParam("parameter")) {
            notification.put("parameter", actionDialect.getParamAsJson("parameter"));
        }

        ActionDialect response = new ActionDialect(ClientAction.ProcessFile.name);
        copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileProcessorModule();
        if (null == module) {
            response.addParam("code", FileProcessorStateCode.Failure.code);
            cellet.speak(talkContext, response);
            return;
        }

        // 调用
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileProcessorStateCode.NoFile.code);
            cellet.speak(talkContext, response);
            return;
        }

        // 是否需要回传文件
        JSONObject responseData = (JSONObject) result;
        if (responseData.has("resultList")) {
            // 需要回送流
            JSONArray array = responseData.getJSONArray("resultList");
            for (int i = 0; i < array.length(); ++i) {
                FileResult prs = new FileResult(array.getJSONObject(i));

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#run - transmit stream to client : " + prs.streamName);
                }

                this.transmitFile(prs.fullPath, prs.streamName, true);
            }
        }
        else {
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#run - No stream : " + fileCode);
            }
        }

        response.addParam("code", FileProcessorStateCode.Ok.code);
        response.addParam("result", responseData);
        cellet.speak(talkContext, response);
    }

    private void transmitFile(String fullPath, String streamName, boolean deleteAfterCompletion) {
        StreamBundle streamBundle = new StreamBundle(fullPath, streamName, deleteAfterCompletion);
        this.bundleQueue.offer(streamBundle);

        if (this.queueWorking.get()) {
            return;
        }

        this.queueWorking.set(true);

        this.cellet.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                StreamBundle bundle = null;

                while (null != (bundle = bundleQueue.poll())) {
                    PrimitiveOutputStream stream = cellet.speakStream(talkContext, bundle.streamName);
                    FileInputStream fis = null;

                    File file = new File(bundle.fullPath);

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

                    if (bundle.deleteAfterCompletion) {
                        file.delete();
                    }
                }

                queueWorking.set(false);
            }
        });
    }

    protected class StreamBundle {

        protected String fullPath;

        protected String streamName;

        protected boolean deleteAfterCompletion;

        public StreamBundle(String fullPath, String streamName, boolean deleteAfterCompletion) {
            this.fullPath = fullPath;
            this.streamName = streamName;
            this.deleteAfterCompletion = deleteAfterCompletion;
        }
    }
}
