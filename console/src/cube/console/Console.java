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

package cube.console;

import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.console.mgmt.DispatcherManager;
import cube.console.mgmt.ServiceManager;
import cube.console.mgmt.UserManager;
import cube.report.JVMReport;
import cube.report.LogLine;
import cube.report.LogReport;
import cube.util.ConfigUtils;
import org.json.JSONArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 控制台数据管理类。
 */
public final class Console implements Runnable {

    private String consoleTag;

    private Properties servers;

    /**
     * 日志记录。
     */
    private ConcurrentHashMap<String, List<LogLine>> serverLogMap;

    /**
     * 记录每个服务器的最大日志行数。
     */
    private int maxLogLines = 200;

    /**
     * JVM 信息记录。
     */
    private ConcurrentHashMap<String, List<JVMReport>> serverJVMMap;

    private int maxReportNum = 30;

    private ScheduledExecutorService timer;

    private ConsoleLogHandler logHandler;

    private UserManager userManager;

    private DispatcherManager dispatcherManager;

    private ServiceManager serviceManager;

    public Console() {
        this.serverLogMap = new ConcurrentHashMap<>();
        this.serverJVMMap = new ConcurrentHashMap<>();
        this.logHandler = new ConsoleLogHandler();
    }

    public String getTag() {
        return this.consoleTag;
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public DispatcherManager getDispatcherManager() {
        return this.dispatcherManager;
    }

    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    public void launch() {
        LogManager.getInstance().addHandle(this.logHandler);

        // 生成服务器基于 MAC 地址信息的识别标识
        this.consoleTag = ConfigUtils.makeUniqueStringWithMAC();

        this.userManager = new UserManager();
        this.dispatcherManager = new DispatcherManager(this.consoleTag);
        this.serviceManager = new ServiceManager(this.consoleTag);

        this.userManager.start();
        this.dispatcherManager.start();
        this.serviceManager.start();

        this.timer = Executors.newScheduledThreadPool(2);
        this.timer.scheduleWithFixedDelay(this, 10L, 10L, TimeUnit.SECONDS);

        Logger.i(this.getClass(), "#launch - tag: " + this.consoleTag);
    }

    public void destroy() {
        this.serviceManager.stop();
        this.dispatcherManager.stop();
        this.userManager.stop();

        this.timer.shutdown();
    }

    @Deprecated
    public JSONArray getDispatcherServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("dispatcher.num"));
        for (int i = 1; i <= num; ++i) {
            String name = this.servers.getProperty("dispatcher." + i + ".name");
            String listening = this.servers.getProperty("dispatcher." + i + ".listening");
            String address = this.servers.getProperty("dispatcher." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("dispatcher." + i + ".port"));

            Follower follower = new Follower(name, listening, address, port);
            array.put(follower.toJSON());
        }

        return array;
    }

    @Deprecated
    public JSONArray getServiceServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("service.num"));
        for (int i = 1; i <= num; ++i) {
            String name = this.servers.getProperty("service." + i + ".name");
            String listening = this.servers.getProperty("service." + i + ".listening");
            String address = this.servers.getProperty("service." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("service." + i + ".port"));

            Follower follower = new Follower(name, listening, address, port);
            array.put(follower.toJSON());
        }

        return array;
    }

    public void appendLogReport(LogReport report) {
        List<LogLine> list = this.serverLogMap.get(report.getReporter());
        if (null == list) {
            list = new Vector<>();
            this.serverLogMap.put(report.getReporter().toString(), list);
        }

        list.addAll(report.getLogs());

        int d = list.size() - this.maxLogLines;
        while (d > 0) {
            list.remove(0);
            --d;
        }
    }

    public List<LogLine> queryLogs(String serverName, long startTimestamp, int maxLength) {
        ArrayList<LogLine> result = new ArrayList<>();
        List<LogLine> list = this.serverLogMap.get(serverName);
        if (null != list) {
            for (int i = 0, size = list.size(); i < size; ++i) {
                LogLine line = list.get(i);
                if (line.time > startTimestamp) {
                    result.add(line);
                    if (result.size() >= maxLength) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<LogLine> queryConsoleLogs(long startTimestamp, int maxLength) {
        ArrayList<LogLine> list = new ArrayList<>();
        synchronized (this.logHandler.logLines) {
            for (int i = 0, size = this.logHandler.logLines.size(); i < size; ++i) {
                LogLine line = this.logHandler.logLines.get(i);
                if (line.time > startTimestamp) {
                    list.add(line);
                    if (list.size() >= maxLength) {
                        break;
                    }
                }
            }
        }
        return list;
    }

    public void appendJVMReport(JVMReport report) {
        Logger.d(this.getClass(), "Received report from " + report.getReporter() + " (" + report.getName() + ")");

        List<JVMReport> list = this.serverJVMMap.get(report.getReporter());
        if (null == list) {
            list = new Vector<>();
            this.serverJVMMap.put(report.getReporter().toString(), list);
        }

        report.scaleValue(1048576);
        list.add(report);
        if (list.size() > this.maxReportNum) {
            list.remove(0);
        }
    }

    public List<JVMReport> queryJVMReport(String reporter, int num, long time) {
        List<JVMReport> result = new ArrayList<>(num);
        List<JVMReport> list = this.serverJVMMap.get(reporter);
        if (null == list) {
            long reportTime = time;
            for (int i = 0; i < num; ++i) {
                JVMReport empty = new JVMReport(reporter, reportTime);
                reportTime -= 60000L;
                result.add(empty);
            }
            Collections.reverse(result);
            return result;
        }

        long scope = 30000L;
        int index = 0;

        // 找到最近的记录
        for (index = list.size() - 1; index >= 0; --index) {
            JVMReport report = list.get(index);
            if (Math.abs(time - report.getTimestamp()) < scope) {
                break;
            }
        }

        while (index >= 0) {
            JVMReport report = list.get(index);
            result.add(report);
            if (result.size() == num) {
                break;
            }
            --index;
        }

        int d = num - result.size();
        if (d > 0) {
            long reportTime = result.isEmpty() ? time : result.get(result.size() - 1).getTimestamp();
            for (int i = 0; i < d; ++i) {
                reportTime -= 60000L;
                JVMReport empty = new JVMReport(reporter, reportTime);
                result.add(empty);
            }
        }

        Collections.reverse(result);

        return result;
    }

    @Override
    public void run() {

    }

    protected class ConsoleLogHandler implements LogHandle {

        protected List<LogLine> logLines;

        public ConsoleLogHandler() {
            this.logLines = new ArrayList<>();
        }

        @Override
        public String getName() {
            return "ConsoleLog";
        }

        @Override
        public void logDebug(String tag, String text) {
            this.recordLog(LogLevel.DEBUG, tag, text);
        }

        @Override
        public void logInfo(String tag, String text) {
            this.recordLog(LogLevel.INFO, tag, text);
        }

        @Override
        public void logWarning(String tag, String text) {
            this.recordLog(LogLevel.WARNING, tag, text);
        }

        @Override
        public void logError(String tag, String text) {
            this.recordLog(LogLevel.ERROR, tag, text);
        }

        private void recordLog(LogLevel level, String tag, String text) {
            LogLine log = new LogLine(level.getCode(), tag, text, System.currentTimeMillis());
            synchronized (this.logLines) {
                this.logLines.add(log);

                if (this.logLines.size() > maxLogLines) {
                    this.logLines.remove(0);
                }
            }
        }
    }
}
