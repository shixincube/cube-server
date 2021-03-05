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
import cell.api.Servable;
import cell.core.cellet.Cellet;
import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.report.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * 服务器的守护任务。
 */
public class Daemon extends TimerTask implements LogHandle {

    private Nucleus nucleus;

    private Kernel kernel;

    private long startTime;

    /**
     * 日志报告间隔。
     */
    private long logReportInterval = 10L * 1000L;

    /**
     * 上一次提交日志的时间戳。
     */
    private long lastLogReport = 0;

    /**
     * 报告发送间隔。
     */
    private long reportInterval = 60L * 1000L;

    /**
     * 最近一次报告时间戳。
     */
    private long lastReportTime = 0;

    /**
     * 日志记录。
     */
    private List<LogLine> logRecords;

    public Daemon(Kernel kernel, Nucleus nucleus) {
        super();
        this.startTime = System.currentTimeMillis();
        this.kernel = kernel;
        this.nucleus = nucleus;
        this.logRecords = new ArrayList<>();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        if (now - this.lastLogReport >= this.logReportInterval) {
            LogReport report = null;

            synchronized (this.logRecords) {
                if (!this.logRecords.isEmpty()) {
                    String reporter = this.kernel.getNodeName();
                    report = new LogReport(reporter);
                    report.addLogs(this.logRecords);
                    this.logRecords.clear();
                }
            }

            if (null != report) {
                ReportService.getInstance().submitReport(report);
            }

            this.lastLogReport = now;
        }

        if (now - this.lastReportTime > this.reportInterval) {
            this.submitJVMReport();
            this.submitPerformanceReport();
            this.lastReportTime = now;
        }
    }

    private void submitJVMReport() {
        JVMReport report = new JVMReport(this.kernel.getNodeName());
        report.setSystemStartTime(this.startTime);
        ReportService.getInstance().submitReport(report);
    }

    private void submitPerformanceReport() {
        PerformanceReport report = new PerformanceReport(this.kernel.getNodeName());
        report.setSystemStartTime(this.startTime);

        for (Cellet cellet : this.nucleus.getCelletService().getCellets()) {
            if (cellet instanceof AbstractCellet) {
                AbstractCellet absCellet = (AbstractCellet) cellet;
                report.gather(absCellet);
            }
        }

        for (Servable server : this.nucleus.getTalkService().getServers()) {
            report.reportConnection(server.getPort(), server.numTalkContexts(), server.getMaxConnections());
        }

        ReportService.getInstance().submitReport(report);
    }

    @Override
    public String getName() {
        return "Daemon";
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
        synchronized (this.logRecords) {
            this.logRecords.add(log);
        }
    }
}
