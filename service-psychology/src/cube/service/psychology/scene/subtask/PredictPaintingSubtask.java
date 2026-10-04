/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.service.psychology.scene.ReportRenderer;
import cube.aigc.spi.AIGCHost;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.RetentionPolicy;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.attachment.ReportAttachment;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.listener.PaintingReportListener;
import cube.service.psychology.scene.PsychologyScene;
import cube.service.psychology.scene.SceneManager;

import java.util.List;

public class PredictPaintingSubtask extends ConversationSubtask {

    /**
     * 绘画推理的消耗量：剩余用量低于该值时不再预测。
     */
    private final static int POWER_OF_PREDICT_PAINTING = 100;

    private static final String[] sAS_KEYWORDS = new String[] {
            "依恋", "依恋类型"
    };

    public PredictPaintingSubtask(AIGCHost host, AIGCChannel channel, String query, ComplexContext context,
                                  ConversationRelation relation, ConversationContext convCtx,
                                  GenerateTextListener listener) {
        super(Subtask.PredictPainting, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        User user = PredictPaintingSubtask.this.host.getUser(channel.getAuthToken().getCode());
        Membership membership = this.host.getMembership(
                channel.getAuthToken().getDomain(), channel.getAuthToken().getContactId(), Membership.STATE_NORMAL);

        // 判断是否剩余用量
        int remaining = this.host.getRemainingUsages(user, membership);
        if (remaining < POWER_OF_PREDICT_PAINTING) {
            // 剩余用量不够执行预测绘画
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    if (null == membership) {
                        record.answer = Utils.randomUnsigned() % 2 == 0 ?
                                fastPolish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_USAGE_JOIN_MEMBER")) :
                                Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_USAGE_JOIN_MEMBER");
                    }
                    else {
                        record.answer = Utils.randomUnsigned() % 2 == 0 ?
                                fastPolish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_USAGE_UPGRADE_MEMBER")) :
                                Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_USAGE_UPGRADE_MEMBER");
                    }
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 文件是否变更
        boolean fileChanged = false;

        // 执行预测
        if (null == convCtx.getCurrentFile()) {
            // 获取文件
            if (null != context.getFileResource()) {
                // 判断文件是否存在
                FileLabel file = this.checkFileLabel(context.getFileResource().getFileLabel());
                if (null != file) {
                    convCtx.setCurrentFile(file);
                }
            }

            if (null == convCtx.getCurrentFile()) {
                Logger.d(this.getClass(), "#work - No file: " +
                        channel.getAuthToken().getCode() + "/" + channel.getCode());
                this.host.schedule("psychology-subtask", 0, new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = fastPolish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_FILE_FOR_PREDICT"));
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }
        else {
            // 更新文件
            if (null != context.getFileResource()) {
                // 判断文件是否存在
                FileLabel newFile = this.checkFileLabel(context.getFileResource().getFileLabel());
                if (null != newFile) {
                    FileLabel curFile = convCtx.getCurrentFile();
                    if (!curFile.getFileCode().equals(newFile.getFileCode())) {
                        // 文件发生变更
                        fileChanged = true;
                    }
                    convCtx.setCurrentFile(newFile);
                }
            }
        }

        // 校验图片是否合规
        if (null != convCtx.getCurrentFile() && !convCtx.getCurrentPaintingValidity()) {
            // 如果是回答 YES 任务，则忽略检测
            boolean ignore = false;

            if (!fileChanged) {
                // 文件没有变更，对用户回答的问题进行推理
                // 是否在回答
                if (roundSubtask == Subtask.Yes) {
                    // 用户坚持使用该文件
                    convCtx.setCurrentPaintingValidity(true);
                    // 忽略检测
                    ignore = true;
                }
                else if (roundSubtask == Subtask.No) {
                    // 回答不是，则表示终止预测
                    convCtx.cancelAll();

                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = Resource.getInstance().getCorpus(CORPUS,
                                    "ANSWER_RE_UPLOAD_PAINTING_FILE");
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
            }

            if (!ignore) {
                boolean valid = PsychologyScene.getInstance().checkPsychologyPainting(channel.getAuthToken(),
                        convCtx.getCurrentFile().getFileCode());
                if (!valid) {
                    // 进入子任务
                    convCtx.activateSubtask(Subtask.PredictPainting);

                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ASK_INVALID_FILE"));
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
                else {
                    convCtx.setCurrentPaintingValidity(true);
                }
            }
        }

        if (null == convCtx.getCurrentAttribute() || !convCtx.getCurrentAttribute().isValid()) {
            // 当前文件
            FileLabel fileLabel = convCtx.getCurrentFile();
            // 提取属性
            Attribute extractedAttribute = this.extractAttribute(query);
            // 当前属性
            Attribute currentAttribute = convCtx.getCurrentAttribute();
            int age = (null != currentAttribute) ? currentAttribute.age : 0;
            String gender = (null != currentAttribute) ? currentAttribute.gender : "";
            if (extractedAttribute.age > 0) {
                age = extractedAttribute.age;
            }
            if (extractedAttribute.gender.length() > 0) {
                gender = extractedAttribute.gender;
            }
            currentAttribute = new Attribute(gender, age, "");
            convCtx.setCurrentAttribute(currentAttribute);

            if (!currentAttribute.isValid()) {
                // 对有效数据部分进行判断
                Attribute attribute = currentAttribute;
                if (attribute.age == 0 && attribute.gender.length() == 0) {
                    // 没有提供年龄和性别
                    Logger.d(this.getClass(), "#work - No attribute: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = this.filterFirstPerson(this.polish(
                            Resource.getInstance().getCorpus(CORPUS, "ANSWER_NEED_TO_PROVIDE_GENDER_AND_AGE")));

                    // 进入子任务
                    convCtx.activateSubtask(Subtask.PredictPainting);
                    convCtx.getSubtaskMemory().record(record);
                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
                else if (attribute.age == 0) {
                    // 没有提供年龄
                    Logger.d(this.getClass(), "#work - No attribute age: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = this.polish(Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_NEED_TO_PROVIDE_AGE"));

                    // 进入子任务
                    convCtx.activateSubtask(Subtask.PredictPainting);
                    convCtx.getSubtaskMemory().record(record);
                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
                else if (attribute.age < Attribute.MIN_AGE || attribute.age > Attribute.MAX_AGE) {
                    // 受测人年龄超出限制
                    Logger.d(this.getClass(), "#work - Age out of limit: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = this.polish(Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_AGE_OUT_OF_LIMIT"));

                    // 进入子任务
                    convCtx.activateSubtask(Subtask.PredictPainting);
                    convCtx.getSubtaskMemory().record(record);
                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
                else if (attribute.gender.length() == 0) {
                    // 没有提供性别
                    Logger.d(this.getClass(), "#work - No attribute gender: " +
                            channel.getAuthToken().getCode() + "/" + channel.getCode());

                    GeneratingRecord record = new GeneratingRecord(query, fileLabel);
                    record.answer = this.polish(Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_NEED_TO_PROVIDE_GENDER"));

                    // 进入子任务
                    convCtx.activateSubtask(Subtask.PredictPainting);
                    convCtx.getSubtaskMemory().record(record);
                    this.host.schedule("psychology-subtask", 0, new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
            }
        }

        // 由会员属性确定留存天数
        int retention = RetentionPolicy.NON_MEMBER_RETENTION_DAYS;
        int numIndicators = user.isRegistered() ? 10 : 5;
        if (null != membership) {
            // 会员
            retention = RetentionPolicy.MEMBER_RETENTION_DAYS;
            numIndicators = 36;
        }

        // 判断是什么评测主题
        Theme theme = Theme.Generic;
        List<String> words = this.host.segmentWords(query);
        for (String word : sAS_KEYWORDS) {
            if (words.contains(word.toLowerCase())) {
                theme = Theme.AttachmentStyle;
                break;
            }
        }

        PaintingReport report = PsychologyScene.getInstance().generatePaintingReport(channel, convCtx.getCurrentAttribute(),
                convCtx.getCurrentFile(), theme, numIndicators, true, retention, null, new PaintingReportListener() {
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
                        GeneratingRecord record = convCtx.getSubtaskMemory().getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                        convCtx.cancelCurrentPredict();
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }

                    @Override
                    public void onReportEvaluating(PaintingReport report) {
                        Logger.d(this.getClass(), "#onReportEvaluating");
                    }

                    @Override
                    public void onReportEvaluateCompleted(PaintingReport report, AIGCUnit unit) {
                        Logger.d(this.getClass(), "#onReportEvaluateCompleted: " + channel.getCode());
                        GeneratingRecord record = convCtx.getSubtaskMemory().getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = ReportRenderer.makeContent(report,
                                true, 0, false);
                        record.answer += ReportRenderer.makePageLink(channel.getHttpsEndpoint(),
                                channel.getAuthToken().getCode(), report, true, true);
                        // clear subtask
                        convCtx.cancelCurrentPredict();
                        // 将生成的报告设置为当前报告
                        convCtx.setCurrentReport(report);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }

                    @Override
                    public void onReportEvaluateFailed(PaintingReport report) {
                        Logger.d(this.getClass(), "#onReportEvaluateFailed - Clear current subtask");
                        GeneratingRecord record = convCtx.getSubtaskMemory().getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                        convCtx.cancelCurrentPredict();
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });

        if (null != report) {
            // 生成权限
            ReportPermission permission = this.host.allowPredictPainting(channel.getAuthToken().getDomain(),
                    user, report.sn);
            // 设置权限
            report.setPermission(permission);

            // 开始生成报告
            final GeneratingRecord record = new GeneratingRecord(query, convCtx.getCurrentFile());

            Attachment attachment = new ReportAttachment(report.sn, convCtx.getCurrentFile());
            AttachmentResource resource = new AttachmentResource(attachment);

            ComplexContext complexContext = new ComplexContext(false);
            complexContext.setInferring(true);
            complexContext.addResource(resource);
            complexContext.setSubtask(Subtask.PredictPainting);

            record.context = complexContext;
            // 记录
            convCtx.getSubtaskMemory().record(record);

            // 启动生成流程，不记录历史，不更新频道状态
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    record.answer = polish(String.format(Resource.getInstance().getCorpus(CORPUS,
                            "FORMAT_ANSWER_GENERATING"),
                            convCtx.getCurrentAttribute().getGenderText(),
                            convCtx.getCurrentAttribute().getAgeText()));
                    listener.onGenerated(channel, record);
                }
            });
            return AIGCStateCode.Ok;
        }
        else {
            // 生成报告发生错误
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query, convCtx.getCurrentFile());
                    record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED"));
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
    }
}
