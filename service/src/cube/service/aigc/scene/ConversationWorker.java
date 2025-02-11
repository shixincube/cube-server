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
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Theme;
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
import java.util.List;

public class ConversationWorker {

    private final static String ANSWER_NO_FILE = "没有找到需要操作的文件，可以尝试重新上传一下文件。";

    private final static String ANSWER_NEED_TO_PROVIDE_GENDER_AND_AGE =
            "进行评测需要知道被测人的性别和年龄，请告诉我画这幅画的被测人的**性别**和**年龄**。";

    private final static String ANSWER_NEED_TO_PROVIDE_GENDER = "我已经知道了这幅画的被测人的年龄，还需要您告知这位被测人的**性别**。";

    private final static String ANSWER_NEED_TO_PROVIDE_AGE = "我已经知道了这幅画的被测人的性别，还需要您告知这位被测人的**年龄**。";

    private final static String ANSWER_FAILED = "我遇到了一些问题，请稍候再试。";

    private final static String FORMAT_ANSWER_GENERATING = "正在为被测人生成报告，该被测人为%s性，年龄是%s。报告生成需要数分钟，请稍候……";

    private final static String ANSWER_CAN_NOT_FIND_REPORTS = "我没有找到您的报告信息。如果您想进行绘画测评，可以将您的绘画发给我。";

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
                    record.answer = ANSWER_NO_FILE;
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
                    record.answer = ANSWER_NEED_TO_PROVIDE_GENDER_AND_AGE;

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
                    record.answer = ANSWER_NEED_TO_PROVIDE_AGE;

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
                    record.answer = ANSWER_NEED_TO_PROVIDE_GENDER;

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
                            record.answer = ANSWER_FAILED;
                            constConvCtx.clearCurrent();
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
                            record.answer = PsychologyHelper.makeMarkdown(report);
                            constConvCtx.clearCurrent();
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
                            record.answer = ANSWER_FAILED;
                            constConvCtx.clearCurrent();
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
                        record.answer = String.format(FORMAT_ANSWER_GENERATING,
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
                        record.answer = ANSWER_FAILED;
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
            if (list.isEmpty()) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = ANSWER_CAN_NOT_FIND_REPORTS;
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
                        String answer = PsychologyHelper.makeReportListMarkdown(list);
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer;
                        constConvCtx.record(record);
                        listener.onGenerated(channel, record);

                        channel.setProcessing(false);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }
        else if (ConversationSubtask.SelectReport == subtask) {

        }
        else if (ConversationSubtask.ExplainPainting == subtask) {

        }
        else {
            Logger.d(this.getClass(), "#work - General conversation");
            convCtx.clearCurrent();
        }

        // 获取单元
        AIGCUnit unit = this.service.selectUnitByName(this.unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find unit \"" + this.unitName + "\"");

            unit = this.service.selectUnitByName(PsychologyScene.getInstance().getUnitName());
            if (null == unit) {
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
                if (word.equals("男") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }
        else {
            for (String word : words) {
                if (word.equals("男") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }

        return new Attribute(gender, age, false);
    }
}
