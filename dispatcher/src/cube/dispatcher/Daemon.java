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

package cube.dispatcher;

import cell.core.talk.TalkContext;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.ContactActions;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.dispatcher.contact.ContactCellet;
import cube.report.LogLine;
import cube.report.LogReport;
import cube.report.ReportService;

import java.util.*;

/**
 * 守护任务。
 */
public class Daemon extends TimerTask implements LogHandle {

    private long contactTimeout = 30 * 1000L;

    private long transmissionTimeout = 10 * 1000L;

    private Performer performer;

    /**
     * 报告发送间隔。
     */
    private long reportInterval = 60L * 1000L;

    /**
     * 日志记录。
     */
    private List<LogLine> logRecords;

    public Daemon(Performer performer) {
        this.performer = performer;
        this.logRecords = new ArrayList<>();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

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
                    if (now - time >= this.contactTimeout) {
                        contact.removeDevice(device);

                        Packet packet = createDeviceTimeout(contact, device, time, now - time);
                        this.performer.transmit(context, ContactCellet.NAME, packet.toDialect());

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

        // 提交日志报告
        this.submitLogReport();
    }

    private Packet createDeviceTimeout(Contact contact, Device device, long failureTime, long timeout) {
        JSONObject data = new JSONObject();
        try {
            data.put("id", contact.getId().longValue());
            data.put("device", device.toJSON());
            data.put("failureTime", failureTime);
            data.put("timeout", timeout);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        Packet packet = new Packet(ContactActions.DeviceTimeout.name, data);
        return packet;
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
