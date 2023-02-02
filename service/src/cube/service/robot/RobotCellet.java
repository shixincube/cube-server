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

package cube.service.robot;

import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.robot.RobotAction;
import cube.robot.RobotStateCode;
import cube.robot.ScriptFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 机器人 Cellet 服务。
 */
public class RobotCellet extends AbstractCellet {

    public final static String NAME = "Robot";

    private RobotService service;

    private ExecutorService executor;

    public RobotCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.service = new RobotService(this);
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(RobotService.NAME, this.service);

        this.executor = Executors.newCachedThreadPool();

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(RobotService.NAME);

        this.service = null;

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (RobotAction.Event.name.equals(action)) {
            // 来自 Dispatcher 的事件
            String name = dialect.getParamAsString("name");
            JSONObject data = dialect.getParamAsJson("data");
            this.service.transferEvent(name, data);
        }
        else if (RobotAction.Fulfill.name.equals(action)) {
            // 来自 Client 的操作
            final String name = dialect.getParamAsString("name");
            final JSONObject parameter = dialect.containsParam("parameter")
                    ? dialect.getParamAsJson("parameter") : new JSONObject();

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    boolean success = service.fulfill(name, parameter);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(success ? RobotStateCode.Ok.code : RobotStateCode.Failure.code,
                            new JSONObject());
                }
            });
        }
        else if (RobotAction.GetReportFile.name.equals(action)) {
            // 来自 Client 的操作
            final String filename = dialect.getParamAsString("filename");

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    service.downloadReportFile(filename, talkContext);
                }
            });
        }
        else if (RobotAction.ListScriptFiles.name.equals(action)) {
            // 来自 Client 的操作
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    List<ScriptFile> list = service.listScriptFiles();
                    JSONObject data = new JSONObject();
                    data.put("total", list.size());

                    JSONArray array = new JSONArray();
                    for (ScriptFile file : list) {
                        array.put(file.toJSON());
                    }
                    data.put("list", array);

                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(RobotStateCode.Ok.code, data);
                }
            });
        }
        else if (RobotAction.GetScriptFile.name.equals(action)) {
            // 来自 Client 的操作
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    String relativePath = dialect.getParamAsString("relativePath");
                    ScriptFile file = service.transmitScriptFile(talkContext, relativePath);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    if (null != file) {
                        responder.respond(RobotStateCode.Ok.code, file.toJSON());
                    }
                    else {
                        responder.respond(RobotStateCode.Failure.code, new JSONObject());
                    }
                }
            });
        }
        else if (RobotAction.RegisterListener.name.equals(action)) {
            // 来自 Client 的操作
            final String name = dialect.getParamAsString("name");

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    int code = RobotStateCode.Unknown.code;

                    if (service.registerListener(name, talkContext)) {
                        code = RobotStateCode.Ok.code;
                    }
                    else {
                        code = RobotStateCode.Failure.code;
                    }

                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    boolean result = responder.respond(code, new JSONObject());
                    if (!result) {
                        Logger.w(RobotCellet.class, "#onListened - respond failed: " + name);
                    }
                }
            });
        }
        else if (RobotAction.DeregisterListener.name.equals(action)) {
            // 来自 Client 的操作
            final String name = dialect.getParamAsString("name");

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    int code = RobotStateCode.Unknown.code;

                    if (service.deregisterListener(name, talkContext)) {
                        code = RobotStateCode.Ok.code;
                    }
                    else {
                        code = RobotStateCode.Failure.code;
                    }

                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    boolean result = responder.respond(code, new JSONObject());
                    if (!result) {
                        Logger.w(RobotCellet.class, "#onListened - respond failed: " + name);
                    }
                }
            });
        }
    }

    @Override
    public void onListened(TalkContext context, PrimitiveInputStream stream) {
        // 处理上传文件
        this.service.processUploadFile(stream);
    }
}
