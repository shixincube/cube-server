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

package cube.service;

import cell.api.Nucleus;
import cell.carpet.CellListener;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.core.Kernel;
import cube.plugin.PluginSystem;
import cube.report.ReportService;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;

/**
 * Cell 容器监听器。
 */
public class ServiceCarpet implements CellListener {

    private Kernel kernel;

    private Daemon daemon;

    private Timer timer;

    public ServiceCarpet() {
        File path = new File("storage/");
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        this.kernel = new Kernel(nucleus);
        nucleus.setParameter("kernel", this.kernel);

        this.daemon = new Daemon(this.kernel, nucleus);
        LogManager.getInstance().addHandle(this.daemon);
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        this.setupKernel();

        PluginSystem.load();

        this.timer = new Timer();
        this.timer.schedule(daemon, 30L * 1000L, 10L * 1000L);

        this.initManagement(nucleus);
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        PluginSystem.unlaod();

        this.teardownKernel();
    }

    private void setupKernel() {
        JSONObject tokenCache = new JSONObject();
        try {
            String filepath= "config/token-pool.properties";
            File file = new File(filepath);
            if (!file.exists()) {
                filepath = "token-pool.properties";
            }
            tokenCache.put("type", SharedMemoryCache.TYPE);
            tokenCache.put("configFile", filepath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.kernel.installCache("TokenPool", tokenCache);

        JSONObject config = new JSONObject();
        try {
            config.put("type", SharedMemoryCache.TYPE);
            config.put("configFile", "config/general-cache.properties");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.kernel.installCache("General", config);

        this.kernel.startup();

        // 启动密码机
        CipherMachine.getInstance().start(this.kernel.getCache("General"));
    }

    private void teardownKernel() {
        // 关闭密码机
        CipherMachine.getInstance().stop();

        this.kernel.uninstallCache("General");

        this.kernel.uninstallCache("TokenPool");

        this.kernel.shutdown();
    }

    private void initManagement(Nucleus nucleus) {
        // 配置控制台
        try {
            Properties properties = ConfigUtils.readProperties("config/console-follower-service.properties");

            // 设置接收报告的服务器
            ReportService.getInstance().addHost(properties.getProperty("console.host"),
                    Integer.parseInt(properties.getProperty("console.port", "7080")));

            String defaultName = ConfigUtils.makeUniqueStringWithMAC() + "#service#" +
                    nucleus.getTalkService().getServers().get(0).getPort();
            this.kernel.setNodeName(properties.getProperty("name", defaultName));
            Logger.i(this.getClass(), "Node name: " + this.kernel.getNodeName());
        } catch (IOException e) {
            Logger.e(this.getClass(), "Read console follower config failed", e);
        }
    }
}
