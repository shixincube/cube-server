/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.aigc.attachment.Attachment;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.ConversationSubtask;
import cube.aigc.psychology.composition.ReportAttachment;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.util.FileType;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ConversationWorker {

    private final static String CORPUS = "conversation";

    private String unitName = ModelConfig.INFINITE_UNIT;

    private AIGCService service;

    public ConversationWorker(AIGCService service) {
        this.service = service;
    }

    public AIGCStateCode work(String token, String channelCode, List<ConversationRelation> conversationRelationList,
                              String query, GenerateTextListener listener) {
        // 获取频道
        AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            channel = this.service.createChannel(token, channelCode, channelCode);
        }

        // 获取单元
        AIGCUnit unit = this.service.selectUnitByName(this.unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find unit \"" + this.unitName + "\"");

            unit = this.service.selectUnitByName(PsychologyScene.getInstance().getUnitName());
            if (null == unit) {
                return AIGCStateCode.UnitError;
            }
        }

        String prompt = PsychologyScene.getInstance().buildPrompt(conversationRelationList, query);

        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            return AIGCStateCode.NoData;
        }

        /** FIXME 2024-08-09 放弃使用历史记录方式
        if (channel.getHistories().isEmpty()) {
            prompt = PsychologyScene.getInstance().buildPrompt(reportRelationList, query);
            if (null == prompt) {
                return AIGCStateCode.NoData;
            }
        }
        else {
            // 非空历史
            histories = new ArrayList<>();
            GenerativeRecord trick = PsychologyScene.getInstance().buildHistory(reportRelationList, query);
            if (null == trick) {
                return AIGCStateCode.NoData;
            }

            histories.add(trick);

            for (GenerativeRecord history : channel.getHistories()) {
                histories.add(history);
                if (histories.size() >= maxHistories) {
                    break;
                }
            }
        }*/

        // 使用指定模型生成结果
        this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                null, null, false, true, listener);

        return AIGCStateCode.Ok;
    }

    public AIGCStateCode work(AIGCChannel channel, GeneratingRecord context, ConversationRelation relation,
                              GenerateTextListener listener) {
        if (channel.isProcessing()) {
            // 频道正在工作
            return AIGCStateCode.Busy;
        }

        // 标记频道正在工作
        channel.setProcessing(true);

        // 获取对话上下文
        ConversationContext convCtx = SceneManager.getInstance().getConversationContext(channel.getCode());
        if (null == convCtx) {
            convCtx = new ConversationContext(relation, channel.getAuthToken());
            SceneManager.getInstance().putConversationContext(channel.getCode(), convCtx);
        }
        // 获取子任务
        ConversationSubtask subtask = convCtx.getCurrentSubtask();
        // Query
        final String query = context.query;
        if (null == subtask) {
            // 匹配子任务
            subtask = this.matchSubtask(query);
        }

        final ConversationContext constConvCtx = convCtx;

        if (ConversationSubtask.PredictPainting == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - PredictPainting: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行预测
            if (null == convCtx.getCurrentFile()) {
                // 获取文件
                if (null != context.queryFileLabels) {
                    // 判断文件是否存在
                    List<FileLabel> fileList = this.checkFileLabels(context.queryFileLabels);
                    if (null != fileList) {
                        convCtx.setCurrentFile(fileList.get(0));
                    }
                }

                if (null == convCtx.getCurrentFile()) {
                    Logger.d(this.getClass(), "#work - No file: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_FILE");
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                        }
                    });
                    channel.setProcessing(false);
                    return AIGCStateCode.Ok;
                }
            }
            else {
                // 更新文件
                if (null != context.queryFileLabels) {
                    // 判断文件是否存在
                    List<FileLabel> fileList = this.checkFileLabels(context.queryFileLabels);
                    if (null != fileList) {
                        convCtx.setCurrentFile(fileList.get(0));
                    }
                }
            }

            if (null == convCtx.getCurrentAttribute()) {
                // 当前文件
                FileLabel fileLabel = convCtx.getCurrentFile();
                // 提取属性
                Attribute attribute = this.extractAttribute(query);
                if (attribute.age == 0 && attribute.gender.length() == 0) {
                    // 没有提供年龄和性别
                    Logger.d(this.getClass(), "#work - No attribute: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NEED_TO_PROVIDE_GENDER_AND_AGE");

                    // 进入子任务
                    convCtx.setCurrentSubtask(subtask);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                        }
                    });
                    channel.setProcessing(false);
                    return AIGCStateCode.Ok;
                }
                else if (attribute.age == 0) {
                    // 没有提供年龄
                    Logger.d(this.getClass(), "#work - No attribute age: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NEED_TO_PROVIDE_AGE");

                    // 进入子任务
                    convCtx.setCurrentSubtask(subtask);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                        }
                    });
                    channel.setProcessing(false);
                    return AIGCStateCode.Ok;
                }
                else if (attribute.gender.length() == 0) {
                    // 没有提供性别
                    Logger.d(this.getClass(), "#work - No attribute gender: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NEED_TO_PROVIDE_GENDER");

                    // 进入子任务
                    convCtx.setCurrentSubtask(subtask);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                        }
                    });
                    channel.setProcessing(false);
                    return AIGCStateCode.Ok;
                }
                else {
                    // 有属性
                    convCtx.setCurrentAttribute(attribute);
                }
            }

            PaintingReport report = PsychologyScene.getInstance().generatePredictingReport(channel, convCtx.getCurrentAttribute(),
                    convCtx.getCurrentFile(), Theme.Generic, 5, new PaintingReportListener() {
                        @Override
                        public void onPaintingPredicting(PaintingReport report, FileLabel file) {
                            Logger.d(this.getClass(), "#onPaintingPredicting");
                        }

                        @Override
                        public void onPaintingPredictCompleted(PaintingReport report, FileLabel file, Painting painting) {
                            Logger.d(this.getClass(), "#onPaintingPredictCompleted");
                        }

                        @Override
                        public void onPaintingPredictFailed(PaintingReport report) {
                            Logger.d(this.getClass(), "#onPaintingPredictFailed: " + channel.getCode());
                            GeneratingRecord record = constConvCtx.getRecent();
                            if (null != record.context) {
                                record.context.setInferring(false);
                            }
                            record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                            constConvCtx.clearCurrentPredict();
                            channel.setProcessing(false);
                        }

                        @Override
                        public void onReportEvaluating(PaintingReport report) {
                            Logger.d(this.getClass(), "#onReportEvaluating");
                        }

                        @Override
                        public void onReportEvaluateCompleted(PaintingReport report) {
                            Logger.d(this.getClass(), "#onReportEvaluateCompleted - Clear current subtask");
                            GeneratingRecord record = constConvCtx.getRecent();
                            if (null != record.context) {
                                record.context.setInferring(false);
                            }
                            record.answer = PsychologyHelper.makeContentMarkdown(report);
                            constConvCtx.clearCurrentPredict();
                            channel.setProcessing(false);

                            SceneManager.getInstance().writeRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    constConvCtx, record);
                        }

                        @Override
                        public void onReportEvaluateFailed(PaintingReport report) {
                            Logger.d(this.getClass(), "#onReportEvaluateFailed - Clear current subtask");
                            GeneratingRecord record = constConvCtx.getRecent();
                            if (null != record.context) {
                                record.context.setInferring(false);
                            }
                            record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                            constConvCtx.clearCurrentPredict();
                            channel.setProcessing(false);
                        }
                    });

            if (null != report) {
                // 开始成功报告
                final GeneratingRecord record = new GeneratingRecord(query, convCtx.getCurrentFile());

                Attachment attachment = new ReportAttachment(report.sn);
                AttachmentResource resource = new AttachmentResource(attachment);

                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Complex);
                complexContext.setInferring(true);
                complexContext.addResource(resource);

                record.context = complexContext;
                // 记录
                convCtx.record(record);

                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        record.answer = String.format(Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_GENERATING"),
                                constConvCtx.getCurrentAttribute().getGenderText(),
                                constConvCtx.getCurrentAttribute().getAgeText());
                        listener.onGenerated(channel, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                // 生成报告发生错误
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query, constConvCtx.getCurrentFile());
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                        listener.onGenerated(channel, record);
                    }
                });
                channel.setProcessing(false);
                return AIGCStateCode.Ok;
            }
        }
        else if (ConversationSubtask.QueryReport == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - QueryReport: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            final List<PaintingReport> list = SceneManager.getInstance().queryReports(constConvCtx.getRelationId(),
                    constConvCtx.getAuthToken().getDomain());
            constConvCtx.setReportList(list);
            if (list.isEmpty()) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_REPORTS_DATA");
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        String answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_QUERY_REPORT_RESULT"),
                                list.size(), PsychologyHelper.makeReportListMarkdown(list));
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer;
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().writeRecord(channel.getCode(), ModelConfig.AIXINLI,
                                constConvCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }
        else if (ConversationSubtask.SelectReport == subtask) {
            if (null == constConvCtx.getReportList()) {
                List<PaintingReport> list = SceneManager.getInstance().queryReports(constConvCtx.getRelationId(),
                        constConvCtx.getAuthToken().getDomain());
                constConvCtx.setReportList(list);
            }

            if (constConvCtx.getReportList().isEmpty()) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_REPORTS_DATA");
                        constConvCtx.record(record);
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
                            record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_PLEASE_INPUT_REPORT_DESC"),
                                    constConvCtx.getReportList().size());
                            constConvCtx.record(record);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);
                        }
                    });
                }
                else {
                    // 找到位置
                    List<PaintingReport> list = constConvCtx.getReportList();
                    if (location <= list.size()) {
                        int index = location - 1;
                        final PaintingReport report = list.get(index);
                        // 设置当前选中报告
                        constConvCtx.setCurrentReport(report);
                        this.service.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(
                                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_REPORT_CONTENT"),
                                        PsychologyHelper.makeReportTitleMarkdown(report),
                                        PsychologyHelper.makeContentMarkdown(report));
                                constConvCtx.record(record);
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
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                        });
                    }
                }
            }
            else {
                // 根据日期信息匹配
                List<PaintingReport> reports = this.matchReports(constConvCtx.getReportList(), year, month, day);
                if (reports.isEmpty()) {
                    // 对应日期没有报告
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (0 != year && 0 != month && 0 != day) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YMD"),
                                        constConvCtx.getReportList().size(), year, month, day);
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                            else if (0 != year && 0 != month) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YM"),
                                        constConvCtx.getReportList().size(), year, month);
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                            else if (0 != month && 0 != day) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_MD"),
                                        constConvCtx.getReportList().size(), month, day);
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                            else if (0 != day) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_DAY"),
                                        constConvCtx.getReportList().size(), day);
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                            else if (0 != month) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_MONTH"),
                                        constConvCtx.getReportList().size(), month);
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                            else if (0 != year) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                        "FORMAT_ANSWER_NO_REPORT_WAS_FOUND_FOR_YEAR"),
                                        constConvCtx.getReportList().size(), year);
                                constConvCtx.record(record);
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
                        constConvCtx.setCurrentReport(reports.get(0));
                        this.service.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = String.format(
                                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_REPORT_CONTENT"),
                                        PsychologyHelper.makeReportTitleMarkdown(reports.get(0)),
                                        PsychologyHelper.makeContentMarkdown(reports.get(0)));
                                constConvCtx.record(record);
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
                                        constConvCtx.getReportList().size(), reports.size(),
                                        PsychologyHelper.makeReportListMarkdown(reports));
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = answer;
                                constConvCtx.record(record);
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);
                            }
                        });
                    }
                }
            }
            return AIGCStateCode.Ok;
        }
        else if (ConversationSubtask.UnselectReport == subtask) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    if (null != constConvCtx.getCurrentReport()) {
                        PaintingReport report = constConvCtx.getCurrentReport();
                        constConvCtx.setCurrentReport(null);
                        String answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_UNSELECT_REPORT_OK"),
                                PsychologyHelper.makeReportTitleMarkdown(report));
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer;
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                    else {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_UNSELECT_REPORT_WARNING");
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                }
            });
            return AIGCStateCode.Ok;
        }
        else if (ConversationSubtask.ShowCoT == subtask) {

        }
        else {
            Logger.d(this.getClass(), "#work - General conversation");
            convCtx.clearCurrentPredict();
        }

        // 获取单元
        AIGCUnit unit = this.service.selectUnitByName(this.unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find unit \"" + this.unitName + "\"");

            unit = this.service.selectUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
            if (null == unit) {
                Logger.w(this.getClass(), "#work - Can NOT find unit \"" + ModelConfig.BAIZE_NEXT_UNIT + "\"");
                channel.setProcessing(false);
                return AIGCStateCode.UnitError;
            }
        }

        String prompt = PsychologyScene.getInstance().buildPrompt(constConvCtx, query);
        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            channel.setProcessing(false);
            return AIGCStateCode.NoData;
        }

        this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                null, null, false, true, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        listener.onFailed(channel, stateCode);
                    }
                });

        return AIGCStateCode.Ok;
    }

    private List<FileLabel> checkFileLabels(List<FileLabel> fileLabels) {
        if (null == fileLabels || fileLabels.isEmpty()) {
            return null;
        }

        List<FileLabel> result = new ArrayList<>();
        for (FileLabel fileLabel : fileLabels) {
            FileLabel local = this.service.getFile(fileLabel.getDomain().getName(), fileLabel.getFileCode());
            if (null != local) {
                // 判断文件类型
                if (local.getFileType() == FileType.JPEG ||
                        local.getFileType() == FileType.PNG ||
                        local.getFileType() == FileType.BMP) {
                    result.add(local);
                }
            }
        }

        return (result.isEmpty()) ? null : result;
    }

    private ConversationSubtask matchSubtask(String query) {
        final List<QuestionAnswer> list = new ArrayList<>();

        // 尝试匹配子任务
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                list.addAll(questionAnswers);
                synchronized (list) {
                    list.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (list) {
                    list.notify();
                }
            }
        });

        if (success) {
            synchronized (list) {
                try {
                    list.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (QuestionAnswer questionAnswer : list) {
            if (questionAnswer.getScore() < 0.8) {
                // 跳过得分低的问答
                continue;
            }
            List<String> answers = questionAnswer.getAnswers();
            for (String answer : answers) {
                ConversationSubtask subtask = ConversationSubtask.extract(answer);
                if (ConversationSubtask.Unknown != subtask) {
                    return subtask;
                }
            }
        }

        return ConversationSubtask.Unknown;
    }

    private Attribute extractAttribute(String query) {
        int age = 0;
        String gender = "";

        List<String> words = this.service.segmentation(query);
        int ageIndex = -1;
        int genderIndex = -1;
        for (int i = 0; i < words.size(); ++i) {
            String word = words.get(i);
            if (Consts.contains(word, Consts.AGE_SYNONYMS)) {
                ageIndex = i;
            }
            else if (Consts.contains(word, Consts.GENDER_SYNONYMS)) {
                genderIndex = i;
            }
        }

        if (ageIndex >= 0) {
            int start = Math.max(0, ageIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        age = Integer.parseInt(word);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            for (int i = words.size() - 1; i >= 0; --i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        int value = Integer.parseInt(word);
                        if (value >= Attribute.MIN_AGE && value <= Attribute.MAX_AGE) {
                            age = value;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (genderIndex >= 0) {
            int start = Math.max(0, genderIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }
        else {
            for (String word : words) {
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }

        return new Attribute(gender, age, false);
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
