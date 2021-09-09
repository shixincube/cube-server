/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.app.server.container;

import cell.util.log.Logger;
import cube.app.server.account.AccountManager;
import cube.util.ConfigUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * 容器管理器。
 */
public class ContainerManager {

    private Server server;

    private JSONObject cubeConfig;

    public ContainerManager() {
    }

    public void launch(int port) {
        AccountManager.getInstance().start();

        Properties config = this.loadConfig();

        this.loadCubeConfig(config);

        this.server = new Server(port);

        this.server.setHandler(createHandlerList(config));

        Logger.i(ContainerManager.class, "Start cube app server # " + port);

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

        try {
            this.server.start();
            this.server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        AccountManager.getInstance().destroy();
    }

    public JSONObject getCubeConfig() {
        return this.cubeConfig;
    }

    private void loadCubeConfig(Properties properties) {
        this.cubeConfig = new JSONObject();
        this.cubeConfig.put("address", properties.getProperty("cube.address"));
        this.cubeConfig.put("domain", properties.getProperty("cube.domain"));
        this.cubeConfig.put("appKey", properties.getProperty("cube.appKey"));
    }

    private HandlerList createHandlerList(Properties properties) {
        String httpAllowOrigin = null;
        String httpsAllowOrigin = null;
        if (null != properties) {
            httpAllowOrigin = properties.getProperty("httpAllowOrigin");
            httpsAllowOrigin = properties.getProperty("httpsAllowOrigin");
        }

        HandlerList handlers = new HandlerList();

        handlers.setHandlers(new Handler[] {
                new CubeConfigHandler(httpAllowOrigin, httpsAllowOrigin, this.cubeConfig),

                new LoginHandler(httpAllowOrigin, httpsAllowOrigin),
                new LogoutHandler(httpAllowOrigin, httpsAllowOrigin),
                new AccountInfoHandler(httpAllowOrigin, httpsAllowOrigin),
                new RegisterHandler(httpAllowOrigin, httpsAllowOrigin),

                new HeartbeatHandler(httpAllowOrigin, httpsAllowOrigin),
                new AccountBuildinHandler(httpAllowOrigin, httpsAllowOrigin),

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
