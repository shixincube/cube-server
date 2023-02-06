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

import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.robot.*;
import cube.service.client.ClientManager;
import cube.service.client.ServerClient;
import cube.service.robot.mission.AbstractMission;
import cube.service.robot.mission.DouYinDailyOperation;
import cube.service.robot.mission.WeiXinMessageList;
import cube.service.robot.mission.DouYinAccountData;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 机器人服务。
 */
public class RobotService extends AbstractModule {

    public final static String NAME = "Robot";

    private final static String EVENT_REPORT = "Report";

    private final RobotCellet cellet;

    private RoboengineImpl roboengine;

    /**
     * 客户端监听事件名映射。
     */
    private Map<String, List<ServerClient>> eventNameClientMap;

    public RobotService(RobotCellet cellet) {
        this.cellet = cellet;
        this.eventNameClientMap = new HashMap<>();
    }

    @Override
    public void start() {
        try {
            Properties config = ConfigUtils.readProperties("config/robot.properties");
            String apiHost = config.getProperty("api.host", "127.0.0.1");
            int apiPort = Integer.parseInt(config.getProperty("api.port", "2280"));
            String apiToken = config.getProperty("api.token", "");

            this.roboengine = new RoboengineImpl();
            (new Thread() {
                @Override
                public void run() {
                    roboengine.start(apiHost, apiPort, apiToken);
                    Logger.i(RobotService.class, "Roboengine agent started");
                    checkMissions();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (null != this.roboengine) {
            this.roboengine.stop();
            this.roboengine = null;
        }
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    /**
     * 注册监听器。
     *
     * @param name
     * @param talkContext
     * @return
     */
    public boolean registerListener(String name, TalkContext talkContext) {
        ServerClient client = ClientManager.getInstance().getClient(talkContext);
        if (null == client) {
            Logger.w(this.getClass(), "#registerListener - Can NOT find server client: " + talkContext.getSessionHost());
            return false;
        }

        synchronized (this.eventNameClientMap) {
            List<ServerClient> list = this.eventNameClientMap.get(name);
            if (null == list) {
                list = new ArrayList<>();
                list.add(client);
                this.eventNameClientMap.put(name, list);
            }
            else {
                if (!list.contains(client)) {
                    list.add(client);
                }
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#registerListener - register " + name + " from " + talkContext.getSessionHost());
        }

        return true;
    }

    /**
     * 注销监听器。
     *
     * @param name
     * @param talkContext
     * @return
     */
    public boolean deregisterListener(String name, TalkContext talkContext) {
        ServerClient client = ClientManager.getInstance().getClient(talkContext);
        if (null == client) {
            Logger.w(this.getClass(), "#deregisterListener - Can NOT find server client: " + talkContext.getSessionHost());
            return false;
        }

        synchronized (this.eventNameClientMap) {
            List<ServerClient> list = this.eventNameClientMap.get(name);
            if (null != list) {
                list.remove(client);
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#deregisterListener - deregister " + name + " from " + talkContext.getSessionHost());
        }

        return true;
    }

    public void transferEvent(String name, JSONObject data) {
        if (EVENT_REPORT.equals(name)) {
            try {
                synchronized (this.eventNameClientMap) {
                    List<ServerClient> list = this.eventNameClientMap.get(name);
                    if (null != list && !list.isEmpty()) {
                        Iterator<ServerClient> iter = list.iterator();
                        while (iter.hasNext()) {
                            ServerClient client = iter.next();

                            TalkContext context = client.getTalkContext();
                            if (null == context) {
                                iter.remove();
                                continue;
                            }

                            ActionDialect dialect = new ActionDialect(RobotAction.Event.name);
                            dialect.addParam("name", name);
                            dialect.addParam("data", data);
                            this.cellet.speak(context, dialect);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#transferEvent - event: " + name, e);
            }
        }
        else {
            Logger.w(this.getClass(), "#transferEvent - Unknown event: " + name);
        }
    }

    private AbstractMission createMission(String name) {
        if (TaskNames.DouYinAccountData.equals(name)) {
            return new DouYinAccountData(this.roboengine);
        }
        else if (TaskNames.DouYinDailyOperation.equals(name)) {
            return new DouYinDailyOperation(this.roboengine);
        }
        else if (TaskNames.WeiXinMessageList.equals(name)) {
            return new WeiXinMessageList(this.roboengine);
        }

        return null;
    }

    /**
     * 返回脚本文件列表。
     *
     * @return 返回脚本文件列表。
     */
    public List<ScriptFile> listScriptFiles() {
        List<ScriptFile> result = new ArrayList<>();

        try {
            List<Path> files = new ArrayList<>();

            Files.list(AbstractMission.sWorkingPath).forEach(path -> {
                if (Files.isDirectory(path)) {
                    try {
                        Files.list(path).forEach(subPath -> {
                            if (!Files.isDirectory(subPath)) {
                                if (subPath.toString().toLowerCase().endsWith(".js")) {
                                    files.add(subPath);
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    if (path.toString().toLowerCase().endsWith(".js")) {
                        files.add(path);
                    }
                }
            });

            int parentStringIndex = AbstractMission.sWorkingPath.toString().length() + 1;

            for (Path file : files) {
                String pathString = file.toString();
                pathString = pathString.substring(parentStringIndex);
                ScriptFile scriptFile = new ScriptFile(file.toFile(), pathString);
                result.add(scriptFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 以流形式向客户端发送文件数据。
     *
     * @param talkContext
     * @param relativePath
     * @return
     */
    public ScriptFile transmitScriptFile(TalkContext talkContext, String relativePath) {
        File file = new File(AbstractMission.sWorkingPath.toFile(), relativePath);
        if (!file.exists()) {
            return null;
        }

        FileInputStream fis = null;
        PrimitiveOutputStream os = this.cellet.speakStream(talkContext, relativePath);

        try {
            fis = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int length = 0;
            while ((length = fis.read(buf)) > 0) {
                os.write(buf, 0, length);
            }
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#transmitScriptFile", e);
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new ScriptFile(file, relativePath);
    }

    public ScriptFile backupScriptFile(String relativePath) {
        File file = new File(AbstractMission.sWorkingPath.toFile(), relativePath);
        if (!file.exists()) {
            return null;
        }

        Path source = Paths.get(file.getAbsolutePath());

        String pathString = FileUtils.extractPath(file.getAbsolutePath());
        Path target = Paths.get(pathString + "/" + file.getName() + ".bak");

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ScriptFile(file, relativePath);
    }

    /**
     * 以立即执行方式执行任务。
     * 指定任务将随机选择机器人进行任务执行。
     *
     * @param missionName
     * @param parameter
     * @return
     */
    public boolean fulfill(String missionName, JSONObject parameter) {
        AbstractMission mission = this.createMission(missionName);
        if (null == mission) {
            return false;
        }

        return this.fulfill(mission, parameter);
    }

    /**
     * 以立即执行方式执行任务。
     * 指定任务将随机选择机器人进行任务执行。
     *
     * @param mission
     * @param parameter
     * @return
     */
    public boolean fulfill(AbstractMission mission, JSONObject parameter) {
        // 设置参数
        mission.setParameter(parameter);

        // 检查
        mission.checkMission();

        if (!mission.isTaskReady()) {
            Logger.i(this.getClass(), "The task is NOT ready");
            return false;
        }

        // 查找可用设备
        List<Account> list = this.roboengine.getOnlineAccounts();
        if (list.isEmpty()) {
            Logger.i(this.getClass(), "No online accounts");
            return false;
        }

        // 找到空闲的账号
        Account account = null;
        for (Account item : list) {
            if (!item.taskRunning) {
                account = item;
                break;
            }
        }

        if (null == account) {
            account = list.get(Utils.randomInt(0, list.size() - 1));
        }

        // 查询计划表
        Schedule schedule = this.roboengine.querySchedule(account.id, mission.getTask().id);
        if (null == schedule) {
            schedule = this.roboengine.newSchedule(mission.getTask().id, account.id, System.currentTimeMillis());
            if (null == schedule) {
                Logger.w(this.getClass(), "New schedule failed - task: " + mission.getTask().id
                    + " , account: " + account.id);
                return false;
            }
        }

        // 上传任务脚本文件
        if (!mission.uploadScriptFiles()) {
            Logger.w(this.getClass(), "Upload script file failed");
            return false;
        }

        // 推送
        return this.roboengine.pushSchedule(schedule);
    }

    /**
     * 以立即执行方式执行任务。
     * 指定任务将随机选择机器人进行任务执行。
     *
     * @param accountId
     * @param mission
     * @param parameter
     * @return
     */
    public boolean fulfill(long accountId, AbstractMission mission, JSONObject parameter) {
        List<Account> accounts = this.roboengine.getOnlineAccounts();
        if (accounts.isEmpty()) {
            Logger.w(this.getClass(), "#fulfill - No online account");
            return false;
        }

        Account account = null;
        for (Account cur : accounts) {
            if (cur.id == accountId) {
                account = cur;
                break;
            }
        }

        if (null == account) {
            Logger.w(this.getClass(), "#fulfill - Can NOT find online account : " + accountId);
            return false;
        }

        if (account.taskRunning) {
            Logger.w(this.getClass(), "#fulfill - Account \"" + accountId + "\" is running");
            return false;
        }

        // 设置参数
        mission.setParameter(parameter);

        // 检查
        mission.checkMission();

        if (!mission.isTaskReady()) {
            Logger.i(this.getClass(), "The task is NOT ready");
            return false;
        }

        // 查询计划表
        Schedule schedule = this.roboengine.querySchedule(account.id, mission.getTask().id);
        if (null == schedule) {
            schedule = this.roboengine.newSchedule(mission.getTask().id, account.id, System.currentTimeMillis());
            if (null == schedule) {
                Logger.w(this.getClass(), "New schedule failed - task: " + mission.getTask().id
                        + " , account: " + account.id);
                return false;
            }
        }

        // 上传任务脚本文件
        if (!mission.uploadScriptFiles()) {
            Logger.w(this.getClass(), "Upload script file failed");
            return false;
        }

        // 推送
        return this.roboengine.pushSchedule(schedule);
    }

    /**
     * 取消任务计划表。
     *
     * @param accountId
     * @param missionName
     * @return
     */
    public Schedule cancel(long accountId, String missionName) {
        AbstractMission mission = this.createMission(missionName);
        if (null == mission) {
            return null;
        }

        // 检查
        mission.checkMission();
        if (!mission.isTaskReady()) {
            Logger.i(this.getClass(), "The task is NOT ready");
            return null;
        }

        // 查询计划表
        Schedule schedule = this.roboengine.querySchedule(accountId, mission.getTask().id);
        if (null == schedule) {
            Logger.w(this.getClass(), "Can NOT find schedule - task: " + mission.getTask().id
                    + " , account: " + accountId);
            return null;
        }

        return (this.roboengine.cancelSchedule(schedule) ? schedule : null);
    }

    /**
     * 下载报告文件。
     *
     * @param filename
     * @param context
     * @return
     */
    public boolean downloadReportFile(String filename, TalkContext context) {
        PrimitiveOutputStream pos = this.cellet.speakStream(context, filename);
        boolean result = this.roboengine.downloadReportFile(filename, pos);
        if (!result) {
            try {
                pos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected void processUploadFile(PrimitiveInputStream stream) {
        File file = new File(AbstractMission.sWorkingPath.toFile(), stream.getName());

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int length = 0;
            while ((length = stream.read(buf)) > 0) {
                fos.write(buf, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
            }

            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    private void checkMissions() {
        if (!this.roboengine.isServerOnline()) {
            Logger.w(this.getClass(), "Roboengine server is NOT online");
            return;
        }

        AbstractMission mission = new DouYinAccountData(this.roboengine);
        mission.checkMission();

        mission = new DouYinDailyOperation(this.roboengine);
        mission.checkMission();

        mission = new WeiXinMessageList(this.roboengine);
        mission.checkMission();

        Logger.i(this.getClass(), "All missions have been checked");
    }
}
