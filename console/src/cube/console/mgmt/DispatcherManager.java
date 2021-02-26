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

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.console.storage.DispatcherStorage;
import cube.console.tool.DeployTool;
import cube.util.ConfigUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * 调度机管理器。
 */
public class DispatcherManager {

    private String tag;

    private DispatcherStorage storage;

    private Path deploySourcePath;

    public DispatcherManager(String tag) {
        this.tag = tag;
    }

    public void start() {
        String filepath = "console.properties";
        File file = new File(filepath);
        if (!file.exists()) {
            filepath = "config/console.properties";
            file = new File(filepath);
            if (!file.exists()) {
                return;
            }
        }

        try {
            Properties properties = ConfigUtils.readProperties(filepath);
            this.storage = new DispatcherStorage(properties);
            this.storage.open();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#start", e);
        }

        this.deploySourcePath = DeployTool.searchDeploySource();
        if (null != this.deploySourcePath) {
            Logger.i(this.getClass(), "Deploy source path: " + this.deploySourcePath.toString());

            // 检查当前库里是否有默认部署信息
            DispatcherServer server = this.storage.readServer(this.tag, this.deploySourcePath.toString());
            if (null == server) {
                // 插入本地部署
                this.storage.writeServer(this.tag, this.getDefaultDeployPath(),
                        this.getDefaultCellConfigFile(), this.getDefaultPropertiesFile());
            }
        }
        else {
            Logger.e(this.getClass(), "Can NOT find deploy source path");
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }
    }

    public String getDefaultDeployPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        return this.deploySourcePath.toString();
    }

    public String getDefaultCellConfigFile() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path file = Paths.get(this.deploySourcePath.toString(), "config/dispatcher.xml");
        return file.toString();
    }

    public String getDefaultPropertiesFile() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path file = Paths.get(this.deploySourcePath.toString(), "config/dispatcher.properties");
        return file.toString();
    }

    public List<DispatcherServer> listDispatcherServers() {
        if (null == this.deploySourcePath) {
            return null;
        }

        List<DispatcherServer> list = this.storage.listServers();
        if (null == list) {
            return null;
        }

        for (DispatcherServer server : list) {
            if (this.tag.equals(server.tag)) {
                server.refresh();
            }
            else {
                // TODO
            }
        }

        return list;
    }

    public DispatcherServer getDispatcherServer(String tag, String deployPath) {
        DispatcherServer server = this.storage.readServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        if (this.tag.equals(server.tag)) {
            server.refresh();
        }
        else {
           // TODO
        }

        return server;
    }

    /**
     * 启动服务器。
     *
     * @param tag
     * @param deployPath
     * @param password
     * @return
     */
    public DispatcherServer startDispatcher(String tag, String deployPath, String password) {
        DispatcherServer server = getDispatcherServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        // 判断运行状态
        if (!server.isRunning()) {
            if (this.tag.equals(server.tag)) {
                (new Thread() {
                    @Override
                    public void run() {
                        int status = 0;
                        ProcessBuilder pb = new ProcessBuilder(deployPath + "/start-dispatcher.sh");
                        pb.directory(new File(deployPath));
                        try {
                            String line = null;
                            Process process = pb.start();
                            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                            while ((line = stdInput.readLine()) != null) {
                                if (line.length() > 0) {
                                    Logger.i(DispatcherManager.class, "#startDispatcher - " + line);
                                }
                            }
                            while ((line = stdError.readLine()) != null) {
                                if (line.length() > 0) {
                                    Logger.w(DispatcherManager.class, "#startDispatcher - " + line);
                                }
                            }

                            try {
                                status = process.waitFor();
                            } catch (InterruptedException e) {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Logger.i(DispatcherManager.class, "Start dispatcher '" + deployPath + "' - " + status);
                    }
                }).start();
            }
            else {
                // TODO
            }
        }

        return server;
    }

    /**
     * 关停服务器。
     *
     * @param tag
     * @param deployPath
     * @param password
     * @return
     */
    public DispatcherServer stopDispatcher(String tag, String deployPath, String password) {
        DispatcherServer server = getDispatcherServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        // 判断运行状态
        if (server.isRunning()) {
            if (this.tag.equals(server.tag)) {
                (new Thread() {
                    @Override
                    public void run() {
                        int status = 0;
                        ProcessBuilder pb = new ProcessBuilder(deployPath + "/stop-dispatcher.sh");
                        pb.directory(new File(deployPath));
                        try {
                            String line = null;
                            Process process = pb.start();
                            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                            while ((line = stdInput.readLine()) != null) {
                                if (line.length() > 0) {
                                    Logger.i(DispatcherManager.class, "#stopDispatcher - " + line);
                                }
                            }
                            while ((line = stdError.readLine()) != null) {
                                if (line.length() > 0) {
                                    Logger.w(DispatcherManager.class, "#stopDispatcher - " + line);
                                }
                            }

                            try {
                                status = process.waitFor();
                            } catch (InterruptedException e) {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Logger.i(DispatcherManager.class, "Stop dispatcher '" + deployPath + "' - " + status);
                    }
                }).start();
            }
            else {
                // TODO
            }
        }

        return server;
    }
}
