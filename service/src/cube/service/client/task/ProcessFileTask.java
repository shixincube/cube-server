/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        String process = actionDialect.getParamAsString("process");
        String fileCode = null;
        String fileURL = null;

        JSONObject notification = new JSONObject();
        notification.put("action", process);
        notification.put("domain", domain);

        if (actionDialect.containsParam("fileCode")) {
            fileCode = actionDialect.getParamAsString("fileCode");
            notification.put("fileCode", fileCode);
        }
        else if (actionDialect.containsParam("fileURL")) {
            fileURL = actionDialect.getParamAsString("fileURL");
            notification.put("fileURL", fileURL);
        }

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
                Logger.d(this.getClass(), "#run - No stream : " + ((null != fileCode) ? fileCode : fileURL));
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
