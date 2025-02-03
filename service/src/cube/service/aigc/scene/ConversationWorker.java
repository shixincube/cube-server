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
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.composition.*;
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

    private String unitName = ModelConfig.INFINITE_UNIT;

    private AIGCService service;

    public ConversationWorker(AIGCService service) {
        this.service = service;
    }

    public AIGCStateCode work(String token, String channelCode, List<ReportRelation> reportRelationList,
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

        String prompt = PsychologyScene.getInstance().buildPrompt(reportRelationList, query);

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

    public AIGCStateCode work(String token, AIGCChannel channel, GeneratingRecord context, GenerateTextListener listener) {
        // 获取对话上下文
        ConversationContext convCtx = SceneManager.getInstance().getConversationContext(channel.getCode());
        if (null == convCtx) {
            convCtx = new ConversationContext();
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

        if (ConversationSubtask.PredictPainting == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - PredictPainting: " + token + "/" + channel.getCode());

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
                    Logger.d(this.getClass(), "#work - No file: " + token + "/" + channel.getCode());
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = ANSWER_NO_FILE;
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                        }
                    });
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
                    Logger.d(this.getClass(), "#work - No attribute: " + token + "/" + channel.getCode());

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
                    return AIGCStateCode.Ok;
                }
                else if (attribute.age == 0) {
                    // 没有提供年龄
                    Logger.d(this.getClass(), "#work - No attribute age: " + token + "/" + channel.getCode());

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
                    return AIGCStateCode.Ok;
                }
                else if (attribute.gender.length() == 0) {
                    // 没有提供性别
                    Logger.d(this.getClass(), "#work - No attribute gender: " + token + "/" + channel.getCode());

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
                    return AIGCStateCode.Ok;
                }
                else {
                    // 有属性
                    convCtx.setCurrentAttribute(attribute);
                }
            }

            final ConversationContext constConvCtx = convCtx;
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
                            GeneratingRecord record = constConvCtx.getRecent();
                            if (null != record.context) {
                                record.context.setInferring(false);
                            }
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
                            record.answer = makeMarkdown(report);
                            constConvCtx.clearCurrent();
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
                return AIGCStateCode.Ok;
            }
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
                return AIGCStateCode.UnitError;
            }
        }

        String prompt = PsychologyScene.getInstance().buildPrompt(convCtx, query);
        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            return AIGCStateCode.NoData;
        }

        this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                null, null, false, true, listener);

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

    private String makeMarkdown(PaintingReport report) {
        StringBuilder buf = new StringBuilder();
        if (report.isNull()) {
            buf.append("根据您提供的绘画文件，我没有发现有效的心理投射内容，建议您检查一下图片文件内容。");
            return buf.toString();
        }

        EvaluationReport evalReport = report.getEvaluationReport();
        buf.append("根据您提供的绘画图片，");
        if (evalReport.isHesitating()) {
            buf.append("我发现画面内容并不容易被识别。");
        }
        else {
            buf.append("我已经按照心理学绘画投射理论进行了解读。");
        }
        buf.append("这位**").append(report.getAttribute().getAgeText()).append("**的**")
                .append(report.getAttribute().getGenderText()).append("性**")
                .append("被测人，");
        buf.append("在这幅绘画中投射出了").append(evalReport.numRepresentations()).append("个心理表征，");
        buf.append("相关的评测报告内容如下：\n\n");

        buf.append("# 概述\n\n");
        buf.append(report.getSummary()).append("\n\n");

        if (evalReport.numEvaluationScores() > 0) {
            buf.append("# 指标数据\n\n");
            for (EvaluationScore score : evalReport.getEvaluationScores()) {
                ReportSection section = report.getReportSection(score.indicator);
                if (null == section) {
                    continue;
                }

                buf.append("## ").append(section.title).append("\n\n");
                buf.append("* **评级** ：").append(score.rate.value).append("级 （").append(score.rate.displayName).append("）\n\n");
                buf.append("**【描述】**\n\n").append(section.report).append("\n\n");
                buf.append("**【建议】**\n\n").append(section.suggestion).append("\n\n");
            }
            buf.append("\n");
        }

        PersonalityAccelerator personality = evalReport.getPersonalityAccelerator();
        if (null != personality) {
            BigFivePersonality bigFivePersonality = personality.getBigFivePersonality();
            buf.append("# 大五人格\n\n");
            buf.append("**【人格画像】** ：**").append(bigFivePersonality.getDisplayName()).append("**。\n\n");
            buf.append("**【人格描述】** ：\n\n").append(bigFivePersonality.getDescription()).append("\n\n");
            buf.append("**【维度描述】** ：\n\n");
            buf.append("* **宜人性**（").append(bigFivePersonality.getObligingness()).append("）\n");
            buf.append(bigFivePersonality.getObligingnessContent()).append("\n\n");
            buf.append("* **尽责性**（").append(bigFivePersonality.getConscientiousness()).append("）\n");
            buf.append(bigFivePersonality.getConscientiousnessContent()).append("\n\n");
            buf.append("* **外向性**（").append(bigFivePersonality.getExtraversion()).append("）\n");
            buf.append(bigFivePersonality.getExtraversionContent()).append("\n\n");
            buf.append("* **进取性**（").append(bigFivePersonality.getAchievement()).append("）\n");
            buf.append(bigFivePersonality.getAchievementContent()).append("\n\n");
            buf.append("* **情绪性**（").append(bigFivePersonality.getNeuroticism()).append("）\n");
            buf.append(bigFivePersonality.getNeuroticismContent()).append("\n\n");
        }

        buf.append("综上所述，通过各项评测描述可以帮助被测人对自身有一个清晰、客观、全面的认识，从而进行科学、有效的管理。通过对自身的心理状态、人格特质等方面的了解，认识到更多的可能性，从而对生活和工作方向提供参考。");
        if (evalReport.getAttention().level <= Attention.GeneralAttention.level && !evalReport.isHesitating()) {
            buf.append("被测人目前的心理状态尚可，应当积极保持良好的作息和积极的生活、工作习惯。遇到困难积极应对。");
        }
        else {
            buf.append("被测人应当关注自己最近的心理状态变化，如果有需要应当积极需求帮助。");
        }
        buf.append("\n\n");

        buf.append("需要注意的是：\n\n");
        buf.append("1. **不要将测试结果当作永久的“标签”。**测试的结果仅仅是根据最近一周或者近期的感觉，其结果也只是表明短期内的心理健康状态，是可以调整变化的，不必产生心理负担。\n\n");
        buf.append("2. **报告结果没有“好”与“坏”之分。**报告结果与个人道德品质无关，只反映你目前的心理状态，但不同的特点对于不同的工作、生活状态会存在“合适”和“不合适”的区别，从而表现出具体条件的优势和劣势。\n\n");
        buf.append("3. **以整体的观点来看待测试结果。**很多测验都包含多个分测验，对于这类测验来说，不应该孤立地理解单个分测验的成绩。在评定一个人的特征时，一方面需要理解每一个分测验分数的意义，但更重要的是综合所有信息全面分析。\n\n");

        return buf.toString();
    }
}
