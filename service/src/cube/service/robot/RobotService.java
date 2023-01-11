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

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.robot.Report;
import cube.robot.RobotAction;
import cube.service.client.ClientManager;
import cube.service.client.ServerClient;
import cube.service.robot.mission.AbstractMission;
import cube.service.robot.mission.ReportDouYinAccountData;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.IOException;
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

    /**
     * 已立即执行方式执行任务。
     * 指定任务将随机选择机器人进行任务执行。
     *
     * @param mission
     * @return
     */
    public boolean fulfill(AbstractMission mission) {
        
        return true;
    }

    private void checkMissions() {
        if (!this.roboengine.isServerOnline()) {
            Logger.w(this.getClass(), "Roboengine server is NOT online");
            return;
        }

        ReportDouYinAccountData mission = new ReportDouYinAccountData(this.roboengine);
        mission.checkMission();
    }
}
