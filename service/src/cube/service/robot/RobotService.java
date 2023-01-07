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

import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.robot.Task;
import cube.service.robot.mission.AbstractMission;
import cube.service.robot.mission.ReportDouYinAccountData;
import cube.util.ConfigUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * 机器人服务。
 */
public class RobotService extends AbstractModule {

    public final static String NAME = "Robot";

    private RoboengineImpl roboengine;

    public RobotService() {
    }

    @Override
    public void start() {
        try {
            Properties config = ConfigUtils.readProperties("config/robot.properties");
            String apiHost = config.getProperty("api.host", "127.0.0.1");
            int apiPort = Integer.parseInt(config.getProperty("api.port", "2280"));
            String apiToken = config.getProperty("api.token", "");

            this.roboengine = new RoboengineImpl();
            this.roboengine.start(apiHost, apiPort, apiToken);

            (new Thread() {
                @Override
                public void run() {
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

    public boolean fulfill() {

        return true;
    }

    private void checkMissions() {
        ReportDouYinAccountData mission = new ReportDouYinAccountData(this.roboengine);
        mission.checkMission();
    }
}
