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

package cube.dispatcher;

import cell.api.Nucleus;
import cell.api.Servable;
import cell.carpet.CellListener;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.report.ReportService;
import cube.util.ConfigUtils;
import cube.util.HttpConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

/**
 * 网关容器的监听器。
 */
public class DispatcherListener implements CellListener {

    private Timer timer;

    private Daemon daemon;

    private List<String> cellets;

    public DispatcherListener() {
        Logger.i(this.getClass(), "--------------------------------");
        Logger.i(this.getClass(), "Version " + Version.toVersionString());
        Logger.i(this.getClass(), "--------------------------------");
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        Performer performer = new Performer(nucleus);
        nucleus.setParameter("performer", performer);

        this.daemon = new Daemon(performer);
        LogManager.getInstance().addHandle(this.daemon);

        // 从配置文件加载配置数据
        this.config(performer);
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.start(this.cellets);
        this.cellets.clear();
        this.cellets = null;

        this.timer = new Timer();
        this.timer.schedule(this.daemon, 30 * 1000, 10 * 1000);

        // 配置管理信息
        this.initManagement(performer, nucleus);
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.stop();
    }

    private void config(Performer performer) {
        File file = new File("config/dispatcher.properties");
        if (!file.exists()) {
            file = new File("dispatcher.properties");
            if (!file.exists()) {
                Logger.w(this.getClass(), "Can NOT find config file");
                return;
            }
        }

        Properties properties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        // 读取管理的 Cellet 名称
        String celletsConfig = properties.getProperty("cellets");
        String[] celletNames = celletsConfig.split(",");
        this.cellets = new ArrayList<>(celletNames.length);
        // 默认都允许访问 Client
        this.cellets.add("Client");
        for (String cellet : celletNames) {
            this.cellets.add(cellet.trim());
        }

        // 读取路由配置
        String prefix = "director.";
        for (int i = 1; i <= 10; ++i) {
            String keyAddress = prefix + (i) + ".address";
            if (!properties.containsKey(keyAddress)) {
                continue;
            }

            String keyPort = prefix + (i) + ".port";
            String keyCellets = prefix + (i) + ".cellets";
            String keyWeight = prefix + (i) + ".weight";

            String address = properties.getProperty(keyAddress);
            int port = Integer.parseInt(properties.getProperty(keyPort, "6000"));

            String celletString = properties.getProperty(keyCellets);
            String[] cellets = celletString.split(",");
            int weight = Integer.parseInt(properties.getProperty(keyWeight, "5"));

            // 设定范围
            Scope scope = new Scope();
            for (String cellet : cellets) {
                scope.cellets.add(cellet.trim());
            }
            scope.weight = weight;
            // 添加导演机节点
            performer.addDirector(address, port, scope);

            Logger.i(this.getClass(), "Add director point " + address + ":" + port + " #" + weight);
        }

        // 加载 HTTP 配置
        HttpConfig httpConfig = this.loadHttpConfig(properties);
        performer.configHttpServer(httpConfig);

        // 配置 App
        if (properties.containsKey("app.login")) {
            Performer.APP_LOGIN_URL = properties.getProperty("app.login").trim();
        }

        // 配置 Robot
        if (properties.containsKey("robot.api")) {
            Performer.ROBOT_API_URL = properties.getProperty("robot.api").trim();
        }
        if (properties.containsKey("robot.callback")) {
            Performer.ROBOT_CALLBACK_URL = properties.getProperty("robot.callback").trim();
        }
    }

    private HttpConfig loadHttpConfig(Properties properties) {
        HttpConfig config = new HttpConfig();

        config.httpPort = Integer.parseInt(properties.getProperty("http.port", "7010"));
        config.httpsPort = Integer.parseInt(properties.getProperty("https.port", "0"));
        config.keystore = properties.getProperty("keystore", "");
        config.storePassword = properties.getProperty("storePassword", "");
        config.managerPassword = properties.getProperty("managerPassword", "");

        return config;
    }

    private void initManagement(Performer performer, Nucleus nucleus) {
        // 配置控制台
        try {
            Properties properties = ConfigUtils.readProperties("config/console-follower-dispatcher.properties");

            // 设置接收报告的服务器
            ReportService.getInstance().addHost(properties.getProperty("console.host"),
                    Integer.parseInt(properties.getProperty("console.port", "7080")));

            // 设置节点名
            String defaultName = ConfigUtils.makeUniqueStringWithMAC();
            for (Servable server : nucleus.getTalkService().getServers()) {
                if (server.getClass().getName().equals("cell.core.talk.Server")) {
                    defaultName += "#dispatcher#" + server.getPort();
                    break;
                }
            }
            performer.setNodeName(properties.getProperty("name", defaultName));
            Logger.i(this.getClass(), "Node name: " + performer.getNodeName());
        } catch (IOException e) {
            Logger.e(this.getClass(), "Read console follower config failed", e);
        }
    }
}
