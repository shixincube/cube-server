/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.app.server.Manager;
import cube.app.server.account.AccountManager;
import cube.app.server.applet.WeChatApplet;
import cube.app.server.container.file.ListSharingTagHandler;
import cube.app.server.container.file.ListSharingTraceHandler;
import cube.app.server.container.file.SharingTagHandler;
import cube.app.server.container.file.TraverseVisitTraceHandler;
import cube.app.server.notice.NoticeManager;
import cube.app.server.version.VersionManager;
import cube.util.ConfigUtils;
import cube.util.HttpConfig;
import cube.util.HttpServer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * 容器管理器。
 */
public class ContainerManager {

    private HttpServer server;

    private JSONObject cubeConfig;

    private HttpConfig httpConfig;

    public ContainerManager() {
    }

    public void launch() {
        AccountManager.getInstance().start();
        NoticeManager.getInstance().start();
        VersionManager.getInstance().start();
        WeChatApplet.getInstance().start();

        Properties config = this.loadConfig();

        this.loadHttpConfig(config);
        this.loadCubeConfig(config);

        this.loadOtherConfig(config);

        // 启动管理器
        (new Thread() {
            @Override
            public void run() {
                if (!Manager.getInstance().start(config)) {
                    Logger.e(ContainerManager.class, "Cube client start failed");
                }
                else {
                    Logger.i(ContainerManager.class, "Cube client started");
                }
            }
        }).start();

        this.server = new HttpServer();
        try {
            this.server.setKeystorePath(this.httpConfig.keystore);
        } catch (FileNotFoundException e) {
            Logger.w(this.getClass(), "#launch", e);
            return;
        }
        this.server.setKeystorePassword(this.httpConfig.storePassword, this.httpConfig.managerPassword);
        this.server.setPort(this.httpConfig.httpPort, this.httpConfig.httpsPort);

        // 设置句柄
        this.server.setHandler(createHandlerList(config));

        Logger.i(ContainerManager.class, "Start cube app server # "
                + this.httpConfig.httpPort + "/" + this.httpConfig.httpsPort);

        System.out.println("" +
                "                                             \n" +
                "  ,----..                                    \n" +
                " /   /   \\                ,---,              \n" +
                "|   :     :         ,--,,---.'|              \n" +
                ".   |  ;. /       ,'_ /||   | :              \n" +
                ".   ; /--`   .--. |  | ::   : :      ,---.   \n" +
                ";   | ;    ,'_ /| :  . |:     |,-.  /     \\  \n" +
                "|   : |    |  ' | |  . .|   : '  | /    /  | \n" +
                ".   | '___ |  | ' |  | ||   |  / :.    ' / | \n" +
                "'   ; : .'|:  | : ;  ; |'   : |: |'   ;   /| \n" +
                "'   | '/  :'  :  `--'   \\   | '/ :'   |  / | \n" +
                "|   :    / :  ,      .-./   :    ||   :    | \n" +
                " \\   \\ .'   `--`----'   /    \\  /  \\   \\  /  \n" +
                "  `---`                 `-'----'    `----'   \n" +
                "                                             ");

        System.out.println();

        // 加入主线程
        this.server.start(true);
    }

    public void destroy() {
        Manager.getInstance().stop();

        WeChatApplet.getInstance().destroy();
        VersionManager.getInstance().destroy();
        NoticeManager.getInstance().destroy();
        AccountManager.getInstance().destroy();
    }

    public JSONObject getCubeConfig() {
        return this.cubeConfig;
    }

    private HttpConfig loadHttpConfig(Properties properties) {
        this.httpConfig = new HttpConfig();
        this.httpConfig.httpPort = Integer.parseInt(properties.getProperty("http.port", "7777"));
        this.httpConfig.httpsPort = Integer.parseInt(properties.getProperty("https.port", "8140"));
        this.httpConfig.keystore = properties.getProperty("keystore");
        this.httpConfig.storePassword = properties.getProperty("storePassword");
        this.httpConfig.managerPassword = properties.getProperty("managerPassword");
        return this.httpConfig;
    }

    private void loadCubeConfig(Properties properties) {
        this.cubeConfig = new JSONObject();
        this.cubeConfig.put("address", properties.getProperty("cube.address"));
        this.cubeConfig.put("domain", properties.getProperty("cube.domain"));
        this.cubeConfig.put("appKey", properties.getProperty("cube.appKey"));
    }

    private void loadOtherConfig(Properties properties) {
        if (properties.containsKey("log.level")) {
            String level = properties.getProperty("log.level");
            if (level.equalsIgnoreCase("DEBUG")) {
                LogManager.getInstance().setLevel(LogLevel.DEBUG);
            }
            else if (level.equalsIgnoreCase("INFO")) {
                LogManager.getInstance().setLevel(LogLevel.INFO);
            }
            else if (level.equalsIgnoreCase("WARNING")) {
                LogManager.getInstance().setLevel(LogLevel.WARNING);
            }
            else if (level.equalsIgnoreCase("ERROR")) {
                LogManager.getInstance().setLevel(LogLevel.ERROR);
            }
            else {
                LogManager.getInstance().setLevel(LogLevel.INFO);
            }
        }

        if (properties.containsKey("url.account")) {
            AccountManager.getInstance().setAccountExternalURL(properties.getProperty("url.account"));
        }
        if (properties.containsKey("url.upgrade")) {
            AccountManager.getInstance().setUpgradeExternalURL(properties.getProperty("url.upgrade"));
        }
    }

    private HandlerList createHandlerList(Properties properties) {
        String httpAllowOrigin = null;
        String httpsAllowOrigin = null;
        if (null != properties) {
            httpAllowOrigin = properties.getProperty("http.allowOrigin", null);
            httpsAllowOrigin = properties.getProperty("https.allowOrigin", null);
        }

        HandlerList handlers = new HandlerList();

        handlers.setHandlers(new Handler[] {
                new CubeConfigHandler(httpAllowOrigin, httpsAllowOrigin, this.cubeConfig),
                new TraceHandler(httpAllowOrigin, httpsAllowOrigin, this.cubeConfig),

                new LoginHandler(httpAllowOrigin, httpsAllowOrigin),
                new LogoutHandler(httpAllowOrigin, httpsAllowOrigin),
                new BindHandler(httpAllowOrigin, httpsAllowOrigin),
                new AccountInfoHandler(httpAllowOrigin, httpsAllowOrigin),
                new RegisterHandler(httpAllowOrigin, httpsAllowOrigin),
                new CheckPhoneAvailableHandler(httpAllowOrigin, httpsAllowOrigin),
                new SearchAccountHandler(httpAllowOrigin, httpsAllowOrigin),

                // 账号升级
                new UpgradeHandler(httpAllowOrigin, httpsAllowOrigin),
                // 账号管理
                new ManagementHandler(httpAllowOrigin, httpsAllowOrigin),

                new DomainInfoHandler(httpAllowOrigin, httpsAllowOrigin),

                new HeartbeatHandler(httpAllowOrigin, httpsAllowOrigin),
                new AccountBuildinHandler(httpAllowOrigin, httpsAllowOrigin),

                new NoticeHandler(httpAllowOrigin, httpsAllowOrigin),
                new VersionHandler(httpAllowOrigin, httpsAllowOrigin),
                new CaptchaHandler(httpAllowOrigin, httpsAllowOrigin),

                new ListSharingTagHandler(httpAllowOrigin, httpsAllowOrigin),
                new ListSharingTraceHandler(httpAllowOrigin, httpsAllowOrigin),
                new SharingTagHandler(httpAllowOrigin, httpsAllowOrigin),
                new TraverseVisitTraceHandler(httpAllowOrigin, httpsAllowOrigin),

                new StopHandler(this.server, this),
                new DefaultHandler()});

        return handlers;
    }

    private Properties loadConfig() {
        String[] configFiles = new String[] {
                "server_dev.properties",
                "server.properties"
        };

        String configFile = null;
        for (String filename : configFiles) {
            File file = new File(filename);
            if (file.exists()) {
                configFile = filename;
                break;
            }
        }

        if (null != configFile) {
            try {
                return ConfigUtils.readProperties(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
