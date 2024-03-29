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

package cube.service.fileprocessor.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.Base64;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.common.state.FileProcessorStateCode;
import cube.file.OperationWork;
import cube.file.OperationWorkflow;
import cube.file.OperationWorkflowListener;
import cube.file.operation.SteganographyOperation;
import cube.service.ServiceTask;
import cube.service.fileprocessor.FileProcessorService;
import cube.service.fileprocessor.FileProcessorServiceCellet;
import cube.vision.Size;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 隐写任务。
 */
public class SteganographicTask extends ServiceTask {

    private File presetFile = new File("storage/assets/zhong.png");

    public SteganographicTask(Cellet cellet, TalkContext talkContext, Primitive primitive,
                              ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 请求的数据
        JSONObject data = packet.data;

        if (data.has("content")) {
            // 进行编码

            String content = data.getString("content");
            // Base64 解码
            try {
                byte[] bytes = Base64.decode(content);
                content = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Logger.e(this.getClass(), "", e);
                // 应答
                ActionDialect response = this.makeResponse(action, packet,
                        FileProcessorAction.Steganographic.name, FileProcessorStateCode.InvalidParameter.code, data);
                this.cellet.speak(this.talkContext, response);
                markResponseTime();
                return;
            }

            Long contactId = 10000L;

            Object mutex = new Object();

            OperationWorkflow workflow = new OperationWorkflow(this.presetFile);
            workflow.setDomain(AuthConsts.DEFAULT_DOMAIN);

            SteganographyOperation operation = new SteganographyOperation(content);
            workflow.append(new OperationWork(operation));

            Size markSize = operation.getWatermarkSize();

            FileProcessorService service = ((FileProcessorServiceCellet) this.cellet).getService();
            service.launchOperationWorkFlow(workflow, new OperationWorkflowListener() {
                @Override
                public void onWorkflowStarted(OperationWorkflow workflow) {
                    // Nothing
                }

                @Override
                public void onWorkflowStopped(OperationWorkflow workflow) {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            });

            synchronized (mutex) {
                try {
                    mutex.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            FileLabel fileLabel = workflow.getResultFileLabel();
            if (null == fileLabel) {
                File file = new File(service.getWorkPath().toFile(), workflow.getResultFilename());
                fileLabel = service.writeFileToStorageService(contactId, workflow.getDomain(), file);
            }

            if (null == fileLabel) {
                // 应答
                ActionDialect response = this.makeResponse(action, packet,
                        FileProcessorAction.Steganographic.name, FileProcessorStateCode.NoFile.code, data);
                this.cellet.speak(this.talkContext, response);
                markResponseTime();
                return;
            }

            JSONObject resultData = new JSONObject();
            resultData.put("fileLabel", fileLabel.toCompactJSON());
            resultData.put("maskCode", this.encodeSize(markSize));
            // 应答
            ActionDialect response = this.makeResponse(action, packet,
                    FileProcessorStateCode.Ok.code, resultData);
            this.cellet.speak(this.talkContext, response);
            markResponseTime();
        }
        else if (data.has("fileCode") && data.has("maskCode")) {
            // 进行解码

            String fileCode = data.getString("fileCode");
            String maskCode = data.getString("maskCode");

            Long contactId = 10000L;

            Object mutex = new Object();

            Size size = decodeSize(maskCode);

            OperationWorkflow workflow = new OperationWorkflow();
            workflow.setSourceFileCode(fileCode);
            workflow.setDomain(AuthConsts.DEFAULT_DOMAIN);

            SteganographyOperation operation = new SteganographyOperation(size);
            workflow.append(new OperationWork(operation));

            FileProcessorService service = ((FileProcessorServiceCellet) this.cellet).getService();
            service.launchOperationWorkFlow(workflow, new OperationWorkflowListener() {
                @Override
                public void onWorkflowStarted(OperationWorkflow workflow) {
                    // Nothing
                }

                @Override
                public void onWorkflowStopped(OperationWorkflow workflow) {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            });

            synchronized (mutex) {
                try {
                    mutex.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            FileLabel fileLabel = workflow.getResultFileLabel();
            if (null == fileLabel) {
                File file = new File(service.getWorkPath().toFile(), workflow.getResultFilename());
                fileLabel = service.writeFileToStorageService(contactId, workflow.getDomain(), file);
            }

            if (null == fileLabel) {
                // 应答
                ActionDialect response = this.makeResponse(action, packet,
                        FileProcessorAction.Steganographic.name, FileProcessorStateCode.NoFile.code, data);
                this.cellet.speak(this.talkContext, response);
                markResponseTime();
                return;
            }

            JSONObject resultData = new JSONObject();
            resultData.put("fileLabel", fileLabel.toCompactJSON());
            // 应答
            ActionDialect response = this.makeResponse(action, packet,
                    FileProcessorStateCode.Ok.code, resultData);
            this.cellet.speak(this.talkContext, response);
            markResponseTime();
        }
        else {
            ActionDialect response = this.makeResponse(action, packet,
                    FileProcessorStateCode.Failure.code, data);
            this.cellet.speak(this.talkContext, response);
            markResponseTime();
        }
    }

    private String encodeSize(Size size) {
        StringBuilder buf = new StringBuilder();

        int[] values = new int[] { size.height, size.width };
        for (int value : values) {
            if (value < 10) {
                buf.append(Utils.randomString(3));
                buf.append(value);
            }
            else if (value < 100) {
                buf.append(Utils.randomString(2));
                buf.append(value);
            }
            else if (value < 1000) {
                buf.append(Utils.randomString(1));
                buf.append(value);
            }
            else {
                buf.append(value);
            }
        }

        return buf.toString();
    }

    private Size decodeSize(String string) {
        if (string.length() != 8) {
            return null;
        }

        int height = 0;
        int width = 0;

        String[] values = new String[] {
                string.substring(0, 4),
                string.substring(4, 8)
        };

        for (String value : values) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < value.length(); ++i) {
                char c = value.charAt(i);
                if (c >= '0' && c <= '9') {
                    buf.append(c);
                }
            }

            if (height == 0) {
                try {
                    height = Integer.parseInt(buf.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (width == 0) {
                try {
                    width = Integer.parseInt(buf.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return new Size(width, height);
    }
}
