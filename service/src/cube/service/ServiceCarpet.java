/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service;

import cell.api.Nucleus;
import cell.carpet.CellListener;
import cell.util.Utils;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.license.LicenseConfig;
import cube.license.LicenseTool;
import cube.plugin.PluginSystem;
import cube.report.ReportService;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;

/**
 * Cell 容器监听器。
 */
public class ServiceCarpet implements CellListener {

    private Kernel kernel;

    private Daemon daemon;

    private Timer timer;

    private String licensePath = "license/";

    public ServiceCarpet() {
        Logger.i(this.getClass(), "--------------------------------");
        Logger.i(this.getClass(), "Version " + Version.toVersionString());
        Logger.i(this.getClass(), "--------------------------------");

        File path = new File("storage/");
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        this.kernel = new Kernel(nucleus);
        nucleus.setParameter("kernel", this.kernel);

        this.daemon = new Daemon(this.kernel, nucleus, this.licensePath);
        nucleus.setParameter("daemon", this.daemon);

        LogManager.getInstance().addHandle(this.daemon);
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        StringBuilder buf = new StringBuilder();
        buf.append("Service STARTED - ");
        buf.append(Utils.gsDateFormat.format(new Date(System.currentTimeMillis())));
        buf.append("\n");

        if (!this.verifyLicence()) {
            // 授权无效，不进行初始化
            buf.append("Not install certificate correctly or certificate has expired!\n");
            buf.append("Cube service is not available!\n");
            Logger.i(this.getClass(), buf.toString());
            return;
        }

        this.setupKernel();

        PluginSystem.load();

        this.timer = new Timer();
        this.timer.schedule(this.daemon, 30 * 1000, 10 * 1000);

        this.initManagement(nucleus);

        buf.append("--------------------------------------------------------------------------------------\n");
        buf.append("   ______            __                  _____                         _\n");
        buf.append("  / ____/  __  __   / /_   ___          / ___/  ___    _____ _   __   (_)  _____  ___\n");
        buf.append(" / /      / / / /  / __ \\ / _ \\         \\__ \\  / _ \\  / ___/| | / /  / /  / ___/ / _ \\\n");
        buf.append("/ /___   / /_/ /  / /_/ //  __/        ___/ / /  __/ / /    | |/ /  / /  / /__  /  __/\n");
        buf.append("\\____/   \\__,_/  /_.___/ \\___/        /____/  \\___/ /_/     |___/  /_/   \\___/  \\___/\n");
        buf.append("--------------------------------------------------------------------------------------\n");
        Logger.i(this.getClass(), buf.toString());
    }

    @Override
    public void cellPredestroy(Nucleus nucleus) {
        this.kernel.dispose();
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


    private boolean verifyLicence() {
        Logger.i(this.getClass(), "License path: " + (new File(this.licensePath)).getAbsolutePath());

        Date expiration = LicenseTool.getExpiration(this.licensePath);
        if (System.currentTimeMillis() > expiration.getTime()) {
            Logger.e(this.getClass(), "Certificate expiration: " + Utils.gsDateFormat.format(expiration));
            return false;
        }

        PublicKey publicKey = LicenseTool.getPublicKeyFromCer(this.licensePath);
        if (null == publicKey) {
            Logger.e(this.getClass(), "Read certificate file error");
            return false;
        }

        LicenseConfig config = LicenseTool.getLicenseConfig(new File(this.licensePath, "cube.license"));
        if (null == config) {
            Logger.e(this.getClass(), "Read license file error");
            return false;
        }

        Logger.i(this.getClass(), "License expiration: " + config.expiration);

        String signContent = config.extractSignContent();

        byte[] data = signContent.getBytes(StandardCharsets.UTF_8);
        try {
            return LicenseTool.verify(data, config.signature, publicKey);
        } catch (Exception e) {
            Logger.e(this.getClass(), "Verify license error", e);
        }

        return false;
    }


    private void setupKernel() {
        // Read configs
        Properties properties = null;
        try {
            properties = ConfigUtils.readProperties("config/service.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
        AbstractCellet.initialize(properties);

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

        AbstractCellet.halt();
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
