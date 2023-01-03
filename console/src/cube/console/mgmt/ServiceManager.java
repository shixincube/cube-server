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

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.console.storage.ServiceStorage;
import cube.console.tool.DeployTool;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务单元管理器。
 */
public class ServiceManager {

    private String tag;

    private ServiceStorage storage;

    private Path deploySourcePath;

    private Map<String, ServiceServer> serverMap;

    private long timeout = 5L * 60L * 1000L;

    public ServiceManager(String tag) {
        this.tag = tag;
        this.serverMap = new ConcurrentHashMap<>();
    }

    public void start() {
        String filepath = null;
        for (String path : DeployTool.CONSOLE_PROP_FILES) {
            File file = new File(path);
            if (file.exists()) {
                filepath = path;
                break;
            }
        }

        try {
            Properties properties = ConfigUtils.readProperties(filepath);
            this.storage = new ServiceStorage(properties);
            this.storage.open();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#start", e);
        }

        this.deploySourcePath = DeployTool.searchDeploySource();
        if (null != this.deploySourcePath) {
            Logger.i(this.getClass(), "Deploy source path: " + this.deploySourcePath.toString());

            // 检查当前库里是否有默认部署信息
            ServiceServer server = this.storage.readServer(this.tag, this.deploySourcePath.toString());
            if (null == server) {
                // 插入本地部署
                this.storage.writeServer(this.tag, this.getDefaultDeployPath(),
                        this.getDefaultConfigPath(), this.getDefaultCelletsPath());
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

    public void tick(long now) {
        // 按照指定超时时间删除内存里的服务器数据
        Iterator<ServiceServer> iter = this.serverMap.values().iterator();
        while (iter.hasNext()) {
            ServiceServer server = iter.next();
            if (now - server.timestamp > this.timeout) {
                iter.remove();
            }
        }
    }

    public String getDefaultDeployPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        return this.deploySourcePath.toString();
    }

    public String getDefaultConfigPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path path = Paths.get(this.deploySourcePath.toString(), "config");
        return path.toString();
    }

    public String getDefaultCelletsPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path path = Paths.get(this.deploySourcePath.toString(), "cellets");
        return path.toString();
    }

    public List<ServiceServer> listServiceServers() {
        if (null == this.deploySourcePath) {
            return null;
        }

        List<ServiceServer> list = this.storage.listServers();
        if (null == list) {
            return null;
        }

        for (int i = 0; i < list.size(); ++i) {
            ServiceServer server = list.get(i);

            if (this.tag.equals(server.tag)) {
                if (!this.serverMap.containsKey(server.deployPath)) {
                    server.refresh();

                    if (server.isLocal()) {
                        this.serverMap.put(server.deployPath, server);
                    }
                }
                else {
                    list.set(i, this.serverMap.get(server.deployPath));
                }
            }
            else {
                // TODO
            }
        }

        return new ArrayList<>(this.serverMap.values());
    }

    public ServiceServer getServiceServer(String tag, String deployPath) {
        ServiceServer server = null;

        if (this.tag.equals(tag)) {
            server = this.serverMap.get(deployPath);
            if (null != server) {
                return server;
            }
        }

        server = this.storage.readServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        if (this.tag.equals(server.tag)) {
            server.refresh();

            if (server.isLocal()) {
                this.serverMap.put(deployPath, server);
            }
        }
        else {
            // TODO
        }

        return server;
    }

    public ServiceServer updateServiceServer(JSONObject data)
            throws JSONException {
        String tag = data.getString("tag");
        String deployPath = data.getString("deployPath");

        return null;
    }

    /**
     * 启动服务器。
     *
     * @param tag
     * @param deployPath
     * @param password
     * @return
     */
    public ServiceServer startService(final String tag, final String deployPath, final String password) {
        ServiceServer server = getServiceServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        // 判断运行状态
        if (!server.isRunning()) {
            if (this.tag.equals(server.tag)) {
                (new Thread() {
                    @Override
                    public void run() {
                        int status = 1;
                        ProcessBuilder pb = new ProcessBuilder(deployPath + "/start-service.sh");
                        pb.directory(new File(deployPath));
                        try {
                            // 执行脚本
                            Process process = pb.start();
                            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                            (new Thread() {
                                @Override
                                public void run() {
                                    String line = null;
                                    try {
                                        while ((line = stdInput.readLine()) != null) {
                                            if (line.length() > 0) {
                                                Logger.i(DispatcherManager.class, "#startService - " + line);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (null != stdInput) {
                                            try {
                                                stdInput.close();
                                            } catch (IOException e) {
                                            }
                                        }
                                    }
                                }
                            }).start();

                            (new Thread() {
                                @Override
                                public void run() {
                                    String line = null;
                                    try {
                                        while ((line = stdError.readLine()) != null) {
                                            if (line.length() > 0) {
                                                Logger.w(DispatcherManager.class, "#startService - " + line);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (null != stdError) {
                                            try {
                                                stdError.close();
                                            } catch (IOException e) {
                                            }
                                        }
                                    }
                                }
                            }).start();

                            try {
                                status = process.waitFor();
                            } catch (InterruptedException e) {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Logger.i(DispatcherManager.class, "Start service '" + deployPath + "' - " + status);

                        // 5秒后刷新状态
                        AtomicInteger count = new AtomicInteger(8);
                        Timer timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                ServiceServer server = getServiceServer(tag, deployPath);
                                // 刷新数据
                                server.refresh();

                                if (server.isRunning()) {
                                    timer.cancel();
                                    return;
                                }

                                if (count.decrementAndGet() == 0) {
                                    timer.cancel();
                                }
                            }
                        };
                        timer.schedule(task, 5000, 3000);
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
    public ServiceServer stopService(String tag, String deployPath, String password) {
        ServiceServer server = getServiceServer(tag, deployPath);
        if (null == server) {
            return null;
        }

        // 判断运行状态
        if (server.isRunning()) {
            if (this.tag.equals(server.tag)) {
                (new Thread() {
                    @Override
                    public void run() {
                        int status = 1;
                        ProcessBuilder pb = new ProcessBuilder(deployPath + "/stop-service.sh");
                        pb.directory(new File(deployPath));
                        try {
                            // 执行脚本
                            Process process = pb.start();
                            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                            (new Thread() {
                                @Override
                                public void run() {
                                    String line = null;
                                    try {
                                        while ((line = stdInput.readLine()) != null) {
                                            if (line.length() > 0) {
                                                Logger.i(DispatcherManager.class, "#stopService - " + line);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (null != stdInput) {
                                            try {
                                                stdInput.close();
                                            } catch (IOException e) {
                                            }
                                        }
                                    }
                                }
                            }).start();

                            (new Thread() {
                                @Override
                                public void run() {
                                    String line = null;
                                    try {
                                        while ((line = stdError.readLine()) != null) {
                                            if (line.length() > 0) {
                                                Logger.w(DispatcherManager.class, "#stopService - " + line);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (null != stdError) {
                                            try {
                                                stdError.close();
                                            } catch (IOException e) {
                                            }
                                        }
                                    }
                                }
                            }).start();

                            try {
                                status = process.waitFor();
                            } catch (InterruptedException e) {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Logger.i(DispatcherManager.class, "Stop service '" + deployPath + "' - " + status);

                        // 5秒后刷新状态
                        AtomicInteger count = new AtomicInteger(8);
                        Timer timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                ServiceServer server = getServiceServer(tag, deployPath);
                                // 刷新数据
                                server.refresh();

                                if (!server.isRunning()) {
                                    timer.cancel();
                                    return;
                                }

                                if (count.decrementAndGet() == 0) {
                                    timer.cancel();
                                }
                            }
                        };
                        timer.schedule(task, 5000, 2000);
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
