/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.attachment.Attachment;
import cube.aigc.attachment.ReportAttachment;
import cube.aigc.psychology.*;
import cube.aigc.psychology.app.PointTable;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.ContentTools;
import cube.service.aigc.scene.PaintingReportListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.SceneManager;
import cube.service.contact.ContactManager;

public class PredictPaintingSubtask extends ConversationSubtask {

    public PredictPaintingSubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                                  ConversationRelation relation, ConversationContext convCtx,
                                  GenerateTextListener listener) {
        super(Subtask.PredictPainting, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
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
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = fastPolish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_FILE_FOR_PREDICT"));
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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

                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = Resource.getInstance().getCorpus(CORPUS,
                                    "ANSWER_RE_UPLOAD_PAINTING_FILE");
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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
                    convCtx.setCurrentSubtask(Subtask.PredictPainting);

                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ASK_INVALID_FILE"));
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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
            currentAttribute = new Attribute(gender, age);
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
                    convCtx.setCurrentSubtask(Subtask.PredictPainting);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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
                    convCtx.setCurrentSubtask(Subtask.PredictPainting);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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
                    convCtx.setCurrentSubtask(Subtask.PredictPainting);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
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
                    convCtx.setCurrentSubtask(Subtask.PredictPainting);
                    convCtx.record(record);
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
            }
        }

        PaintingReport report = PsychologyScene.getInstance().generatePredictingReport(channel, convCtx.getCurrentAttribute(),
                convCtx.getCurrentFile(), Theme.Generic, 10, new PaintingReportListener() {
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
                        GeneratingRecord record = convCtx.getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                        convCtx.cancelCurrentPredict();
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }

                    @Override
                    public void onReportEvaluating(PaintingReport report) {
                        Logger.d(this.getClass(), "#onReportEvaluating");
                    }

                    @Override
                    public void onReportEvaluateCompleted(PaintingReport report) {
                        Logger.d(this.getClass(), "#onReportEvaluateCompleted: " + channel.getCode());
                        GeneratingRecord record = convCtx.getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = ContentTools.makeContent(report,
                                true, 0, false);
                        record.answer += ContentTools.makePageLink(channel.getHttpsEndpoint(),
                                channel.getAuthToken().getCode(), report, true, true);
                        // clear subtask
                        convCtx.cancelCurrentPredict();
                        // 将生成的报告设置为当前报告
                        convCtx.setCurrentReport(report);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }

                    @Override
                    public void onReportEvaluateFailed(PaintingReport report) {
                        Logger.d(this.getClass(), "#onReportEvaluateFailed - Clear current subtask");
                        GeneratingRecord record = convCtx.getRecent();
                        if (null != record.context) {
                            record.context.setInferring(false);
                        }
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                        convCtx.cancelCurrentPredict();
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }
                });

        if (null != report) {
            // 判断积分
            int points = ContactManager.getInstance().getPointSystem().total(channel.getAuthToken());
            ReportPermission permission = null;
            if (points >= Math.abs(PointTable.EvaluationPoints)) {
                // 全部权限
                permission = ReportPermission.createAllPermissions(report.contactId, report.sn);
                // 扣除积分
                ContactManager.getInstance().getPointSystem().insert(
                        PointTable.createEvaluationPoint(channel.getAuthToken()));
            }
            else {
                // 最小权限
                permission = new ReportPermission(report.contactId, report.sn);
            }
            // 设置权限
            report.setPermission(permission);

            // 开始生成报告
            final GeneratingRecord record = new GeneratingRecord(query, convCtx.getCurrentFile());

            Attachment attachment = new ReportAttachment(report.sn, convCtx.getCurrentFile());
            AttachmentResource resource = new AttachmentResource(attachment);

            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Heavyweight);
            complexContext.setInferring(true);
            complexContext.addResource(resource);
            complexContext.setSubtask(Subtask.PredictPainting);

            record.context = complexContext;
            // 记录
            convCtx.record(record);

            // 启动生成流程，不记录历史，不更新频道状态
            this.service.getExecutor().execute(new Runnable() {
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
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query, convCtx.getCurrentFile());
                    record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED"));
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
    }
}
