/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
import cube.robot.*;
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

    public RobotCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.service = new RobotService(this);
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(RobotService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(RobotService.NAME);

        this.service = null;
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
        else if (RobotAction.Perform.name.equals(action)) {
            final String name = dialect.getParamAsString("name");
            final JSONObject parameter = dialect.containsParam("parameter")
                    ? dialect.getParamAsJson("parameter") : new JSONObject();

            this.execute(new Runnable() {
                @Override
                public void run() {
                    boolean success = false;
                    if (dialect.containsParam("accountId")) {
                        success = service.perform(dialect.getParamAsLong("accountId"),
                                name, parameter);
                    }
                    else {
                        success = service.perform(name, parameter);
                    }

                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(success ? RobotStateCode.Ok.code : RobotStateCode.Failure.code,
                            new JSONObject());
                }
            });
        }
        else if (RobotAction.GetReportFile.name.equals(action)) {
            // 来自 Client 的操作
            final String filename = dialect.getParamAsString("filename");

            this.execute(new Runnable() {
                @Override
                public void run() {
                    service.downloadReportFile(filename, talkContext);
                }
            });
        }
        else if (RobotAction.ListScriptFiles.name.equals(action)) {
            // 来自 Client 的操作
            this.execute(new Runnable() {
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
        else if (RobotAction.DownloadScriptFile.name.equals(action)) {
            // 来自 Client 的操作
            this.execute(new Runnable() {
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
        else if (RobotAction.UploadScriptFile.name.equals(action)) {
            // 来自 Client 的操作
            this.execute(new Runnable() {
                @Override
                public void run() {
                    String relativePath = dialect.getParamAsString("relativePath");
                    // 备份原文件
                    service.backupScriptFile(relativePath);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(RobotStateCode.Ok.code, new JSONObject());
                }
            });
        }
        else if (RobotAction.Cancel.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    long accountId = dialect.getParamAsLong("accountId");
                    String name = dialect.getParamAsString("name");
                    // 取消任务
                    Schedule schedule = service.cancel(accountId, name);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond((null != schedule) ? RobotStateCode.Ok.code : RobotStateCode.Failure.code,
                            (null != schedule) ? schedule.toJSON() : new JSONObject());
                }
            });
        }
        else if (RobotAction.GetAccount.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    long accountId = dialect.getParamAsLong("accountId");
                    Account account = service.getAccount(accountId);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    if (null != account) {
                        responder.respond(RobotStateCode.Ok.code, account.toJSON());
                    }
                    else {
                        responder.respond(RobotStateCode.Failure.code, new JSONObject());
                    }
                }
            });
        }
        else if (RobotAction.GetOnlineList.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    List<Account> accounts = service.getOnlineList();
                    JSONArray array = new JSONArray();
                    for (Account account : accounts) {
                        array.put(account.toJSON());
                    }
                    JSONObject data = new JSONObject();
                    data.put("total", accounts.size());
                    data.put("list", array);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(RobotStateCode.Ok.code, data);
                }
            });
        }
        else if (RobotAction.GetAccountList.name.equals(action)) {
            this.execute(new Runnable() {
                @Override
                public void run() {
                    int begin = dialect.getParamAsInt("begin");
                    int end = dialect.getParamAsInt("end");
                    List<Account> accounts = service.getAccountList(begin, end);
                    JSONArray array = new JSONArray();
                    for (Account account : accounts) {
                        array.put(account.toJSON());
                    }
                    JSONObject data = new JSONObject();
                    data.put("begin", begin);
                    data.put("end", end);
                    data.put("list", array);
                    Responder responder = new Responder(dialect, RobotCellet.this, talkContext);
                    responder.respond(RobotStateCode.Ok.code, data);
                }
            });
        }
        else if (RobotAction.RegisterListener.name.equals(action)) {
            // 来自 Client 的操作
            final String name = dialect.getParamAsString("name");

            this.execute(new Runnable() {
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

            this.execute(new Runnable() {
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
