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

package cube.console;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cube.report.LogLine;
import cube.report.LogReport;
import cube.util.ConfigUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 控制台数据管理类。
 */
public final class Console implements Runnable {

    private Properties servers;

    private ConcurrentHashMap<String, List<LogLine>> serverLogMap;

    /**
     * 记录每个服务器的最大日志行数。
     */
    private int maxLogLines = 5000;

    private ScheduledExecutorService timer;

    private ConsoleLogHandler logHandler;

    public Console() {
        this.serverLogMap = new ConcurrentHashMap<>();
        this.logHandler = new ConsoleLogHandler();
    }

    public void launch() {
        try {
            this.servers = ConfigUtils.readConsoleServers();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LogManager.getInstance().addHandle(this.logHandler);

        this.timer = Executors.newScheduledThreadPool(2);
        this.timer.scheduleWithFixedDelay(this, 10L, 10L, TimeUnit.SECONDS);
    }

    public void destroy() {
        this.timer.shutdown();
    }

    public JSONArray getDispatcherServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("dispatcher.num"));
        for (int i = 1; i <= num; ++i) {
            String address = this.servers.getProperty("dispatcher." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("dispatcher." + i + ".port"));
            JSONObject json = new JSONObject();
            try {
                json.put("address", address);
                json.put("port", port);
                array.put(json);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return array;
    }

    public JSONArray getServiceServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("service.num"));
        for (int i = 1; i <= num; ++i) {
            String address = this.servers.getProperty("service." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("service." + i + ".port"));
            JSONObject json = new JSONObject();
            try {
                json.put("address", address);
                json.put("port", port);
                array.put(json);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
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

    public List<LogLine> queryLogs(long startTimestamp) {
        ArrayList<LogLine> list = new ArrayList<>();
        synchronized (this.logHandler.logLines) {
            for (int i = 0, size = this.logHandler.logLines.size(); i < size; ++i) {
                
            }
        }
        return list;
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
