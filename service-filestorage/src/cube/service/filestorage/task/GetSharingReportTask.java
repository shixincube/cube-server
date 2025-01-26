/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.SharingReport;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.service.filestorage.SharingReportor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * 获取分享报告任务。
 */
public class GetSharingReportTask extends ServiceTask {

    public GetSharingReportTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                                Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        ArrayList<String> reportNames = new ArrayList<>();

        if (packet.data.has("name")) {
            reportNames.add(packet.data.getString("name"));
        }
        else if (packet.data.has("names")) {
            JSONArray array = packet.data.getJSONArray("names");
            for (int i = 0; i < array.length(); ++i) {
                reportNames.add(array.getString(i));
            }
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        // 创建报告机
        SharingReportor reportor = service.getSharingManager().createSharingReportor();

        ArrayList<SharingReport> reportList = new ArrayList<>();
        for (String reportName : reportNames) {
            if (reportName.equalsIgnoreCase(SharingReport.CountRecord)) {
                // 生成报告
                SharingReport report = reportor.reportCountRecord(contact);
                reportList.add(report);
            }
            else if (reportName.equalsIgnoreCase(SharingReport.TopCountRecord)) {
                // 生成报告
                SharingReport report = reportor.reportTopNRecords(contact, 10);
                reportList.add(report);
            }
            else if (reportName.equalsIgnoreCase(SharingReport.HistoryEventRecord)) {
                int duration = 7;
                int unit = Calendar.DATE;

                if (packet.data.has("option")) {
                    JSONObject option = packet.data.getJSONObject("option");
                    duration = option.getInt("duration");
                    unit = option.getInt("unit");
                }

                // 生成报告
                SharingReport report = reportor.reportHistoryTotal(contact, duration, unit);
                reportList.add(report);
            }
            else if (reportName.equalsIgnoreCase(SharingReport.VisitorRecord)) {
                int duration = 7;
                int unit = Calendar.DATE;

                if (packet.data.has("option")) {
                    JSONObject option = packet.data.getJSONObject("option");
                    duration = option.getInt("duration");
                    unit = option.getInt("unit");
                }

                // 生成报告
                SharingReport report = reportor.reportVisitorEventTimeline(contact, duration, unit);
                reportList.add(report);
            }
            else if (reportName.equalsIgnoreCase(SharingReport.FileTypeTotalRecord)) {
                // 生成报告
                SharingReport report = reportor.reportFileTypeTotals(contact);
                reportList.add(report);
            }
        }

        if (reportList.isEmpty()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
        }
        else {
            SharingReport report = reportList.get(0);
            for (int i = 1; i < reportList.size(); ++i) {
                report.merge(reportList.get(i));
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, report.toJSON()));
            markResponseTime();
        }
    }
}
