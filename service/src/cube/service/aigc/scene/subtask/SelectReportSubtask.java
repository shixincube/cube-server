/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.attachment.ReportAttachment;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AttachmentResource;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.ReportHelper;
import cube.service.aigc.scene.SceneManager;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SelectReportSubtask extends ConversationSubtask {

    public SelectReportSubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                              ConversationRelation relation, ConversationContext convCtx,
                              GenerateTextListener listener) {
        super(Subtask.SelectReport, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        if (null == convCtx.getReportList()) {
            List<PaintingReport> list = SceneManager.getInstance().queryReports(convCtx.getAuthToken().getContactId(),
                    0);
            convCtx.setReportList(list);
        }

        if (convCtx.getReportList().isEmpty()) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_NO_REPORTS_DATA"));
                    convCtx.record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);
                }
            });
            return AIGCStateCode.Ok;
        }

        final int year = TextUtils.extractYear(query);
        final int month = TextUtils.extractMonth(query);
        final int day = TextUtils.extractDay(query);
        if (year == 0 && month == 0 && day == 0) {
            // 没有找到日期信息
            List<String> sentences = this.service.segmentation(query);
            int location = TextUtils.extractLocation(sentences);
            if (0 == location) {
                // 没有找到位置
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = polish(String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_PLEASE_INPUT_REPORT_DESC"),
                                convCtx.getReportList().size()));
                        convCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                });
            }
            else {
                // 找到位置
                List<PaintingReport> list = convCtx.getReportList();
                if (location <= list.size()) {
                    int index = location - 1;
                    final PaintingReport report = list.get(index);
                    // 设置当前选中报告
                    convCtx.setCurrentReport(report);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext context = new ComplexContext(ComplexContext.Type.Simplex);
                            ReportAttachment attachment = new ReportAttachment(report.sn, report.getFileLabel());
                            context.addResource(new AttachmentResource(attachment));

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.context = context;
//                            record.answer = polish(String.format(
//                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_REPORT_CONTENT"),
//                                    ReportHelper.makeReportTitleMarkdown(report),
//                                    ReportHelper.makeContentMarkdown(report, 5)));
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_REPORT_CONTENT"),
                                    ReportHelper.makeReportTitleMarkdown(report),
                                    ReportHelper.makeContentSummaryMarkdown(channel, report));
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    });
                }
                else {
                    // 位置越界
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_SELECT_REPORT_LOCATION_OVERFLOW"),
                                    location, list.size());
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    });
                }
            }
        }
        else {
            // 根据日期信息匹配
            List<PaintingReport> reports = this.matchReports(convCtx.getReportList(), year, month, day);
            if (reports.isEmpty()) {
                // 对应日期没有报告
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (0 != year && 0 != month && 0 != day) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YMD"),
                                    convCtx.getReportList().size(), year, month, day);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else if (0 != year && 0 != month) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YM"),
                                    convCtx.getReportList().size(), year, month);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else if (0 != month && 0 != day) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_MD"),
                                    convCtx.getReportList().size(), month, day);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else if (0 != day) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_DAY"),
                                    convCtx.getReportList().size(), day);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else if (0 != month) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_MONTH"),
                                    convCtx.getReportList().size(), month);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else if (0 != year) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YEAR"),
                                    convCtx.getReportList().size(), year);
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                        else {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    }
                });
            }
            else {
                if (reports.size() == 1) {
                    // 设置当前选中报告
                    convCtx.setCurrentReport(reports.get(0));
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext context = new ComplexContext(ComplexContext.Type.Simplex);
                            ReportAttachment attachment = new ReportAttachment(reports.get(0).sn,
                                    reports.get(0).getFileLabel());
                            context.addResource(new AttachmentResource(attachment));

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.context = context;
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_REPORT_CONTENT"),
                                    ReportHelper.makeReportTitleMarkdown(reports.get(0)),
                                    ReportHelper.makeContentMarkdown(reports.get(0), true, 0, false));
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    });
                }
                else {
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            String answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_FOUND_MULTIPLE_REPORTS"),
                                    convCtx.getReportList().size(), reports.size(),
                                    ReportHelper.makeReportListMarkdown(channel, reports));
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = answer;
                            convCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    });
                }
            }
        }
        return AIGCStateCode.Ok;
    }

    private List<PaintingReport> matchReports(List<PaintingReport> reports, int year, int month, int day) {
        List<PaintingReport> result = new ArrayList<>();
        for (PaintingReport report : reports) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(report.timestamp);
            int reportYear = calendar.get(Calendar.YEAR);
            int reportMonth = calendar.get(Calendar.MONTH) + 1;
            int reportDay = calendar.get(Calendar.DATE);

            if (year != 0 && month != 0 && day != 0) {
                if (year == reportYear && month == reportMonth && day == reportDay) {
                    result.add(report);
                }
            }
            else if (year != 0 && month != 0) {
                if (year == reportYear && month == reportMonth) {
                    result.add(report);
                }
            }
            else if (month != 0 && day != 0) {
                if (month == reportMonth && day == reportDay) {
                    result.add(report);
                }
            }
            else if (day != 0) {
                if (day == reportDay) {
                    result.add(report);
                }
            }
            else if (month != 0) {
                if (month == reportMonth) {
                    result.add(report);
                }
            }
            else if (year != 0) {
                if (year == reportYear) {
                    result.add(report);
                }
            }
        }
        return result;
    }
}
