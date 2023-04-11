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

package cube.service;

import cell.api.Nucleus;
import cell.api.Servable;
import cell.core.cellet.Cellet;
import cell.util.Utils;
import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.license.LicenseConfig;
import cube.license.LicenseTool;
import cube.report.*;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.*;

/**
 * 服务器的守护任务。
 */
public class Daemon extends TimerTask implements LogHandle {

    private Nucleus nucleus;

    private Kernel kernel;

    private long startTime;

    private boolean stopped = false;

    /**
     * 日志报告间隔。
     */
    private long logReportInterval = 10 * 1000;

    /**
     * 上一次提交日志的时间戳。
     */
    private long lastLogReport = 0;

    /**
     * 报告发送间隔。
     */
    private long reportInterval = 60 * 1000;

    /**
     * 最近一次报告时间戳。
     */
    private long lastReportTime = 0;

    /**
     * 日志记录。
     */
    private List<LogLine> logRecords;

    /**
     * 验证授权间隔。
     */
    private long verifyLicenceInterval = 60 * 60 * 1000;

    /**
     * 最近一次验证时间。
     */
    private long lastVerifyLicenceTime = 0;

    /**
     * 授权文件目录。
     */
    private String licensePath;

    public Daemon(Kernel kernel, Nucleus nucleus, String licensePath) {
        super();
        this.startTime = System.currentTimeMillis();
        this.kernel = kernel;
        this.nucleus = nucleus;
        this.licensePath = licensePath;
        this.logRecords = new ArrayList<>();
        this.lastVerifyLicenceTime = this.startTime;
    }

    public List<LogLine> getLogRecords(int limit) {
        synchronized (this.logRecords) {
            if (limit <= 0) {
                return new ArrayList<>(this.logRecords);
            }
            else {
                List<LogLine> list = new ArrayList<>(limit);

                for (int i = this.logRecords.size() - 1; i >= 0; --i) {
                    list.add(this.logRecords.get(i));
                    if (list.size() >= limit) {
                        break;
                    }
                }

                Collections.reverse(list);

                return list;
            }
        }
    }

    @Override
    public void run() {
        if (this.stopped) {
            Logger.d(this.getClass(), "Daemon has stopped");
            return;
        }

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
            this.submitJVMReport(now);
            this.submitPerformanceReport(now);
            this.lastReportTime = now;
        }

        // 是否验证授权证书
        if (now - this.lastVerifyLicenceTime > this.verifyLicenceInterval) {
            this.lastVerifyLicenceTime = now;

            if (!this.verifyLicence()) {
                Logger.e(this.getClass(), "Not install certificate correctly or certificate has expired!");

                // 停止运行
                this.stopped = true;

                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        kernel.shutdown();
                    }
                })).start();
            }
        }
    }

    private void submitJVMReport(long timestamp) {
        JVMReport report = new JVMReport(this.kernel.getNodeName(), timestamp);
        report.setSystemStartTime(this.startTime);
        ReportService.getInstance().submitReport(report);
    }

    private void submitPerformanceReport(long timestamp) {
        PerformanceReport report = new PerformanceReport(this.kernel.getNodeName(), timestamp);
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

        // 填写联系人性能
        JSONObject contactPerf = new JSONObject();
        contactPerf.put("onlineNum", ContactManager.getInstance().numOnlineContacts());
        contactPerf.put("maxNum", ContactManager.getInstance().getMaxContactNum());
        report.appendItem(ContactManager.NAME, contactPerf);

        ReportService.getInstance().submitReport(report);
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
