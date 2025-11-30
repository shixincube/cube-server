/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher;

import cell.api.Servable;
import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AuthAction;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.core.AbstractCellet;
import cube.dispatcher.contact.ContactCellet;
import cube.report.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * 守护任务。
 */
public class Daemon extends TimerTask implements LogHandle {

    private long startTime = 0;

    private long contactTimeout = 30 * 1000;

    private long transmissionTimeout = 10 * 1000;

    private Performer performer;

    /**
     * 报告发送间隔。
     */
    private long reportInterval = 60 * 1000;

    /**
     * 最近一次报告时间。
     */
    private long lastReportTime = 0;

    private long latencyInterval = 5 * 60 * 1000;

    private long lastLatency = 0;

    /**
     * 日志记录。
     */
    private List<LogLine> logRecords;

    public Daemon(Performer performer) {
        this.startTime = System.currentTimeMillis();
        this.performer = performer;
        this.logRecords = new ArrayList<>();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        try {
            this.performer.onTick(now);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 检查 Talk context 失效
        Iterator<Map.Entry<String, Contact>> contactIter = this.performer.getOnlineContacts().entrySet().iterator();
        while (contactIter.hasNext()) {
            Map.Entry<String, Contact> e = contactIter.next();
            Contact contact = e.getValue();
            List<Device> devices = contact.getDeviceList();
            for (Device device : devices) {
                TalkContext context = device.getTalkContext();
                if (null == context) {
                    Logger.w(this.getClass(), "Error device, contact id: " + contact.getId() + " - " + device.getName());
                    continue;
                }
                if (!context.isValid()) {
                    // 上下文已失效
                    long time = context.getFailureTime();
                    if (time > 0 && now - time >= this.contactTimeout) {
                        contact.removeDevice(device);

                        ActionDialect actionDialect = createDeviceTimeout(contact, device, time, now - time);
                        this.performer.transmit(context, ContactCellet.NAME, actionDialect);

                        // 删除超时的上下文
                        this.performer.removeTalkContext(context);
                    }
                }
            }
        }

        // 检查 Transmission
        Iterator<Map.Entry<Long, Performer.Transmission>> traniter = this.performer.transmissionMap.entrySet().iterator();
        while (traniter.hasNext()) {
            Map.Entry<Long, Performer.Transmission> e = traniter.next();
            Performer.Transmission transmission = e.getValue();
            if (now - transmission.timestamp > this.transmissionTimeout) {
                traniter.remove();
            }
        }

        // 检查路由记录
        Iterator<Map.Entry<TalkContext, Director>> tditer = this.performer.talkDirectorMap.entrySet().iterator();
        while (tditer.hasNext()) {
            Map.Entry<TalkContext, Director> e = tditer.next();
            TalkContext talkContext = e.getKey();
            if (!talkContext.isValid()) {
                tditer.remove();
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "Remove timeout talk context: " + e.getValue().endpoint.toString());
                }
            }
        }

//        if (now - this.lastReportTime >= this.reportInterval) {
//            // 提交 JVM 报告
//            this.submitJVMReport(now);
//            // 提交性能报告
//            this.submitPerformanceReport(now);
//            // 更新时间戳
//            this.lastReportTime = now;
//        }

        // 提交日志报告
//        this.submitLogReport();

        if (now - this.lastLatency >= this.latencyInterval) {
            this.lastLatency = now;
            latency();
        }
    }

    private void latency() {
        for (Director director : this.performer.directorList) {
            JSONObject payload = new JSONObject();
            payload.put("launch", System.currentTimeMillis());
            Packet packet = new Packet(AuthAction.Latency.name, payload);

            ActionDialect response = this.performer.syncTransmit(director, "Auth", packet.toDialect(), 10 * 1000);
            if (null != response) {
                Packet responsePacket = new Packet(response);
                long remoteTime = responsePacket.data.getJSONObject("data").getLong("time");
                Logger.i(this.getClass(), "Latency - " + director.endpoint.toString() + " : "
                        + (System.currentTimeMillis() - remoteTime) + " ms");
            }
            else {
                Logger.w(this.getClass(), "Latency - Service is not available : " + director.endpoint.toString());
                // 重连
                (new Thread() {
                    @Override
                    public void run() {
                        performer.restart();
                    }
                }).start();
            }
        }
    }

    private ActionDialect createDeviceTimeout(Contact contact, Device device, long failureTime, long timeout) {
        JSONObject data = new JSONObject();
        try {
            data.put("id", contact.getId().longValue());
            data.put("domain", contact.getDomain().getName());
            data.put("device", device.toJSON());
            data.put("failureTime", failureTime);
            data.put("timeout", timeout);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        Packet packet = new Packet(ContactAction.DeviceTimeout.name, data);
        ActionDialect actionDialect = packet.toDialect();
        actionDialect.addParam("token", device.getToken());
        return actionDialect;
    }

    private void submitLogReport() {
        synchronized (this.logRecords) {
            if (this.logRecords.isEmpty()) {
                return;
            }

            LogReport report = new LogReport(this.performer.getNodeName());
            report.addLogs(this.logRecords);

            ReportService.getInstance().submitReport(report);

            this.logRecords.clear();
        }
    }

    private void submitJVMReport(long timestamp) {
        JVMReport report = new JVMReport(this.performer.getNodeName(), timestamp);
        report.setSystemStartTime(this.startTime);
        ReportService.getInstance().submitReport(report);
    }

    private void submitPerformanceReport(long timestamp) {
        PerformanceReport report = new PerformanceReport(this.performer.getNodeName(), timestamp);
        report.setSystemStartTime(this.startTime);

        for (Cellet cellet : this.performer.celletService.getCellets()) {
            if (cellet instanceof AbstractCellet) {
                AbstractCellet absCellet = (AbstractCellet) cellet;
                report.gather(absCellet);
            }
        }

        for (Servable server : this.performer.talkService.getServers()) {
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
