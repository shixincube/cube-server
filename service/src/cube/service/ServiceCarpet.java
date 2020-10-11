/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.core.Kernel;
import cube.plugin.PluginSystem;
import cube.report.ReportService;
import cube.service.auth.AuthService;
import cube.util.ConfigUtils;

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
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        this.kernel = new Kernel();
        nucleus.setParameter("kernel", this.kernel);

        this.daemon = new Daemon(this.kernel, nucleus);
        LogManager.getInstance().addHandle(this.daemon);

        this.initManagement();
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        this.setupKernel();

        PluginSystem.load();

        this.timer = new Timer();
        this.timer.schedule(daemon, 10L * 1000L, 10L * 1000L);
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

        this.kernel.installModule(AuthService.NAME, new AuthService());

        this.kernel.startup();
    }

    private void teardownKernel() {
        this.kernel.shutdown();

        this.kernel.uninstallCache("TokenPool");
    }

    private void initManagement() {
        // 配置控制台
        try {
            Properties properties = ConfigUtils.readConsoleFollower();
            ReportService.getInstance().addHost(properties.getProperty("host"),
                    Integer.parseInt(properties.getProperty("port", "7080")));
        } catch (IOException e) {
            Logger.e(this.getClass(), "Read console follower config failed", e);
        }
    }
}
