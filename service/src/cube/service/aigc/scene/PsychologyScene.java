/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.StrategyFlow;
import cube.aigc.StrategyNode;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.app.UserProfile;
import cube.aigc.psychology.composition.*;
import cube.aigc.psychology.material.Label;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.node.DetectTeenagerQueryStrategyNode;
import cube.service.aigc.scene.node.TeenagerProblemClassificationNode;
import cube.service.aigc.scene.node.TeenagerQueryNode;
import cube.service.contact.ContactManager;
import cube.service.cv.CVService;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 心理学场景。
 */
public class PsychologyScene {

    private final static PsychologyScene instance = new PsychologyScene();

    private AIGCService service;

    private PsychologyStorage storage;

    private long lastConfigModified;

    private Queue<ReportTask> taskQueue;

    private int maxQueueLength = 30;

    private Queue<ReportTask> runningTaskQueue;

    private AtomicInteger numRunningTasks;

    private Queue<ScaleReportTask> scaleTaskQueue;

    private AtomicInteger numRunningScaleTasks;

    /**
     * Key：报告序列号。
     */
    private Map<Long, Report> reportMap;

    /**
     * Key：报告序列号。
     */
    private Map<Long, Painting> paintingMap;

    private PsychologyScene() {
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.runningTaskQueue = new ConcurrentLinkedQueue<>();
        this.numRunningTasks = new AtomicInteger(0);
        this.scaleTaskQueue = new ConcurrentLinkedQueue<>();
        this.numRunningScaleTasks = new AtomicInteger(0);
        this.reportMap = new ConcurrentHashMap<>();
        this.paintingMap = new ConcurrentHashMap<>();
    }

    public static PsychologyScene getInstance() {
        return PsychologyScene.instance;
    }

    public void start(AIGCService service) {
        this.service = service;

        try {
            JSONObject config = ConfigUtils.readJsonFile("psychology.json");

            // 读取存储配置
            JSONObject storage = config.getJSONObject("storage");
            if (storage.getString("type").equalsIgnoreCase("SQLite")) {
                this.storage = new PsychologyStorage(StorageType.SQLite, storage);
            }
            else {
                this.storage = new PsychologyStorage(StorageType.MySQL, storage);
            }

            // 读取性能配置
            JSONObject preference = config.getJSONObject("preference");
            this.maxQueueLength = preference.getInt("maxQueueLength");
            Logger.i(this.getClass(), "#start - max queue length: " + this.maxQueueLength);

            this.storage.open(this.service.getTokenizer());
            this.storage.execSelfChecking(null);

            this.lastConfigModified = System.currentTimeMillis();

            // 检查资源文件
            Resource.getInstance().checkFiles();

            // 数据管理器设置
            SceneManager.getInstance().setService(service);

            // 激活数据集
            String r = ContentTools.extract("白泽京智", this.service.getTokenizer());
            Logger.i(this.getClass(), "#start - Active dataset: " + r);

            String corpus = Resource.getInstance().getCorpus("baize", "MIND_ECHO");
            Logger.i(this.getClass(), "#start - Active corpus: " + corpus);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.w(this.getClass(), "#start", e);
        }

        this.numRunningTasks.set(0);
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }

        // 阻止新任务进入
        this.maxQueueLength = 0;
        this.numRunningTasks.set(Integer.MAX_VALUE);
    }

    private void loadConfig() {
        try {
            File file = new File("config/psychology.json");
            if (file.exists() && file.lastModified() <= this.lastConfigModified) {
                return;
            }

            JSONObject config = ConfigUtils.readJsonFile("psychology.json");
            if (null != config) {
                this.lastConfigModified = file.lastModified();
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#loadConfig", e);
        }
    }

    public boolean recordUsage(Usage usage) {
        return this.storage.writeUsage(usage.cid, usage.token, usage.timestamp, usage.remoteHost, usage.query,
                usage.queryType, usage.queryTokens, usage.completionTokens, usage.completionSN);
    }

    public boolean checkPsychologyPainting(AuthToken authToken, String fileCode) {
        FileLabel fileLabel = this.service.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - File error: " + fileCode);
            return false;
        }

        boolean more = false;
        CVService cvService = (CVService) this.service.getKernel().getModule(CVService.NAME);
        ObjectInfo info = cvService.detectObject(authToken, fileCode, false);
        if (null != info) {
            more = this.hasMoreObjects(info);
        }

        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - No psychology unit: " + fileCode);
            return false;
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        Packet request = new Packet(AIGCAction.CheckPsychologyPainting.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - Predict image unit error");
            return false;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - Predict image response state: " +
                    Packet.extractCode(response));
            return false;
        }

        try {
            JSONObject responseData = Packet.extractDataPayload(response);
            // 结果
            boolean result = responseData.getBoolean("result");
            // 1. 通过像素校验是绘画
            // 2. 物体检测没有其他元素
            // 依此上述两条判断是否是绘画
            return (result || !more);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#checkPsychologyPainting", e);
            return false;
        }
    }

    private boolean hasMoreObjects(ObjectInfo info) {
        int count = 0;
        for (Material object : info.getObjects()) {
            if (object.label.startsWith("人")) {
                continue;
            }
            ++count;
        }
        return (count > 0);
    }

    public PaintingReport getPaintingReport(long sn) {
        Report current = this.reportMap.get(sn);
        if (current instanceof PaintingReport) {
            return (PaintingReport) current;
        }

        PaintingReport report = this.storage.readPsychologyReport(sn);
        if (null == report) {
            return null;
        }

        FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
        if (null == fileLabel) {
            return null;
        }

        report.setFileLabel(fileLabel);
        return report;
    }

    public int numPsychologyReports(long contactId) {
        return this.storage.countPsychologyReports(contactId);
    }

    public int numPsychologyReports(long contactId, int state) {
        return this.storage.countPsychologyReports(contactId, state);
    }

    public int numPsychologyReports(long contactId, int state, boolean permissible) {
        return this.storage.countPsychologyReports(contactId, state, permissible);
    }

    public int numPsychologyReports(long contactId, int state, boolean permissible, long starTime, long endTime) {
        return this.storage.countPsychologyReports(contactId, state, permissible, starTime, endTime);
    }

    public List<PaintingReport> getPsychologyReports(long contactId, int state, int limit) {
        List<PaintingReport> list = this.storage.readPsychologyReportsByContact(contactId, state, limit);
        Iterator<PaintingReport> iter = list.iterator();
        while (iter.hasNext()) {
            PaintingReport report = iter.next();
            FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
                report.painting = this.storage.readPainting(report.sn);
            }
            else {
                Logger.w(this.getClass(), "#getPsychologyReports - NOT find file: " + report.sn);
                iter.remove();
            }
        }
        return list;
    }

    public List<PaintingReport> getPsychologyReports(long contactId, int pageIndex, int pageSize, boolean descending) {
        List<PaintingReport> list = this.storage.readPsychologyReports(contactId, pageIndex, pageSize, descending);
        Iterator<PaintingReport> iter = list.iterator();
        while (iter.hasNext()) {
            PaintingReport report = iter.next();
            FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
                report.painting = this.storage.readPainting(report.sn);
            }
            else {
                iter.remove();
            }
        }
        return list;
    }

    public List<PaintingReport> getPsychologyReportsWithState(long contactId, int pageIndex, int pageSize,
                                                              boolean descending, int state) {
        List<PaintingReport> list = this.storage.readPsychologyReports(contactId, pageIndex, pageSize, descending, state);
        Iterator<PaintingReport> iter = list.iterator();
        while (iter.hasNext()) {
            PaintingReport report = iter.next();
            FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
                report.painting = this.storage.readPainting(report.sn);
            }
            else {
                iter.remove();
            }
        }
        return list;
    }

    /**
     * 修改报告备注。
     *
     * @param reportSn
     * @param remark
     * @return
     */
    public PaintingReport modifyPsychologyReportRemark(long reportSn, String remark) {
        if (this.storage.updatePsychologyReportRemark(reportSn, remark)) {
            PaintingReport report = this.storage.readPsychologyReport(reportSn);
            report.setRemark(remark);
            return report;
        }
        return null;
    }

    /**
     * 根据主题生成评测报告。
     *
     * @param channel
     * @param attribute
     * @param fileLabel
     * @param theme
     * @param maxIndicators
     * @param adjust
     * @param retention
     * @param listener
     * @return
     */
    public synchronized PaintingReport generatePsychologyReport(AIGCChannel channel, Attribute attribute,
                                                                FileLabel fileLabel, Theme theme,
                                                                int maxIndicators, boolean adjust,
                                                                int retention,
                                                                PaintingReportListener listener) {
        if (null == channel) {
            Logger.e(this.getClass(), "#generatePsychologyReport - Channel is null");
            return null;
        }

        if (this.taskQueue.size() >= this.maxQueueLength) {
            Logger.w(this.getClass(), "#generatePsychologyReport - The queue length has reached the maximum limit: "
                    + this.taskQueue.size() + "/" + this.maxQueueLength);
            return null;
        }

        // 判断属性限制
        if (attribute.age < Attribute.MIN_AGE || attribute.age > Attribute.MAX_AGE) {
            Logger.w(this.getClass(), "#generatePsychologyReport - Age param overflow: " +
                    attribute.age);
            return null;
        }

        // 并发数量
        int concurrency = this.service.numUnitsByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (0 == concurrency) {
            concurrency = this.service.numUnitsByName(ModelConfig.BAIZE_X_UNIT);
            if (0 == concurrency) {
                Logger.e(this.getClass(), "#generatePsychologyReport - No baize unit");
                return null;
            }
        }
        if (concurrency > 2) {
            concurrency -= 1;
        }

        if (!this.service.hasUnit(ModelConfig.PSYCHOLOGY_UNIT)) {
            Logger.e(this.getClass(), "#generatePsychologyReport - No psychology unit");
            return null;
        }

        PaintingReport report = new PaintingReport(channel.getAuthToken().getContactId(),
                attribute, fileLabel, theme);

        ReportTask task = new ReportTask(channel, attribute, fileLabel, theme, maxIndicators, adjust, listener, report);

        this.taskQueue.offer(task);

        Logger.d(this.getClass(), "#generatePsychologyReport - Number of concurrency (C/Q/R): " + concurrency
                + "/" + this.taskQueue.size() + "/" + this.numRunningTasks.get());

        this.reportMap.put(report.sn, report);

        // 判断并发数量
        if (this.numRunningTasks.get() >= concurrency) {
            // 并发数量大于等于单元数量，在队列中等待
            return report;
        }

        this.numRunningTasks.incrementAndGet();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.i(PsychologyScene.class, "Generating thread START ("
                        + numRunningTasks.get() + "/" + taskQueue.size() + ") - "
                        + Thread.currentThread().getName()
                        + " - " + Utils.gsDateFormat.format(new Date(System.currentTimeMillis())));

                try {
                    while (!taskQueue.isEmpty()) {
                        // 新任务
                        ReportTask reportTask = taskQueue.poll();
                        if (null == reportTask) {
                            // 队列空，结束
                            break;
                        }

                        long start = System.currentTimeMillis();
                        Logger.i(getClass(), "Starts generating report: " + reportTask.report.sn);

                        runningTaskQueue.offer(reportTask);

                        try {
                            // 设置为正在操作
                            reportTask.channel.setProcessing(true);

                            // 获取单元
                            AIGCUnit unit = service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                            if (null == unit) {
                                // 没有可用单元
                                reportTask.report.setState(AIGCStateCode.UnitError);
                                reportTask.report.setFinished(true);
                                reportTask.listener.onPaintingPredictFailed(reportTask.report);
                                continue;
                            }

                            // 更新单元状态
                            unit.setRunning(true);

                            // 加载配置
                            loadConfig();

                            // 预测
                            reportTask.listener.onPaintingPredicting(reportTask.report, reportTask.fileLabel);

                            Painting painting = processPainting(unit, reportTask.fileLabel, theme, reportTask.adjust, false);
                            if (null == painting) {
                                // 预测绘图失败
                                Logger.w(PsychologyScene.class, "#generatePsychologyReport - onPaintingPredictFailed: " +
                                        reportTask.fileLabel.getFileCode());
                                // 记录故障
                                unit.markFailure(AIGCStateCode.FileError.code, System.currentTimeMillis(),
                                        reportTask.channel.getAuthToken().getContactId());
                                // 更新单元状态
                                unit.setRunning(false);
                                reportTask.report.setState(AIGCStateCode.FileError);
                                reportTask.report.setFinished(true);
                                reportTask.listener.onPaintingPredictFailed(reportTask.report);
                                continue;
                            }

                            // 更新单元状态
                            unit.setRunning(false);

                            // 设置绘画属性
                            painting.setAttribute(reportTask.attribute);
                            painting.fileLabel = reportTask.fileLabel;
                            paintingMap.put(reportTask.report.sn, painting);

                            // 关联绘画
                            reportTask.report.painting = painting;

                            // 绘图预测完成
                            reportTask.listener.onPaintingPredictCompleted(reportTask.report, reportTask.fileLabel, painting);

                            // 开始进行评估
                            reportTask.listener.onReportEvaluating(reportTask.report);

                            // 根据图像推理报告
                            EvaluationWorker evaluationWorker = processReport(reportTask.channel, painting,
                                    reportTask.theme, unit);

                            // 将特征集数据填写到报告，这里仅仅是方便客户端获取特征描述文本
                            reportTask.report.paintingFeatureSet = evaluationWorker.getPaintingFeatureSet();
                            // 设置评估数据
                            reportTask.report.setEvaluationReport(evaluationWorker.getEvaluationReport());

                            // 修改状态
                            reportTask.report.setState(AIGCStateCode.Inferencing);

                            // 执行工作流，制作报告数据
                            evaluationWorker = evaluationWorker.make(reportTask.channel, reportTask.theme, reportTask.maxIndicators);
                            if (null == evaluationWorker) {
                                // 推理生成报告失败
                                Logger.w(PsychologyScene.class, "#generatePsychologyReport - onReportEvaluateFailed (IllegalOperation): " +
                                        reportTask.fileLabel.getFileCode());
                                reportTask.report.setState(AIGCStateCode.IllegalOperation);
                                reportTask.report.setFinished(true);
                                reportTask.listener.onReportEvaluateFailed(reportTask.report);
                                continue;
                            }

                            // 填写数据
                            evaluationWorker.fillReport(reportTask.report);

                            if (evaluationWorker.isUnknown()) {
                                // 未能处理的图片
                                Logger.w(PsychologyScene.class, "#generatePsychologyReport - onReportEvaluateCompleted (InvalidData): " +
                                        reportTask.fileLabel.getFileCode());
                                reportTask.report.setState(AIGCStateCode.InvalidData);
                                reportTask.report.setFinished(true);

                                // 存储
                                storage.writePsychologyReport(reportTask.report);
                                storage.writePainting(reportTask.report.sn, reportTask.fileLabel.getFileCode(), painting);
                                if (null != evaluationWorker.getPaintingFeatureSet()) {
                                    PaintingFeatureSet paintingFeatureSet = evaluationWorker.getPaintingFeatureSet();
                                    storage.writePaintingFeatureSet(paintingFeatureSet);
                                }

                                // 按照正常状态返回
                                reportTask.listener.onReportEvaluateCompleted(reportTask.report);
                                // reportTask.listener.onReportEvaluateFailed(reportTask.report);
                                continue;
                            }

                            // 生成 Markdown 调试信息
                            reportTask.report.makeMarkdown();

                            // 设置状态
                            if (reportTask.report.isNull()) {
                                reportTask.report.setState(AIGCStateCode.Failure);
                            } else {
                                reportTask.report.setState(AIGCStateCode.Ok);
                            }

                            // 修改结束状态
                            reportTask.report.setFinished(true);
                            reportTask.listener.onReportEvaluateCompleted(reportTask.report);

                            // 填写数据
                            evaluationWorker.fillReport(reportTask.report);
                            // 生成 Markdown 调试信息
                            reportTask.report.makeMarkdown();

                            // 存储
                            storage.writePsychologyReport(reportTask.report, retention);
                            storage.writePainting(reportTask.report.sn, reportTask.fileLabel.getFileCode(), painting);
                            if (null != evaluationWorker.getPaintingFeatureSet()) {
                                PaintingFeatureSet paintingFeatureSet = evaluationWorker.getPaintingFeatureSet();
                                storage.writePaintingFeatureSet(paintingFeatureSet);
                            }

                            // 使用数据管理器生成关联数据
                            //                        SceneManager.getInstance().writeReportChart(reportTask.report);

                            Logger.i(getClass(), "End generating report: " + reportTask.report.sn + " - elapsed: " +
                                    Math.round((System.currentTimeMillis() - start) / 1000.0) + "s");
                        } catch (Exception e) {
                            Logger.e(PsychologyScene.class, "#run", e);
                        } finally {
                            // 从正在执行队列移除
                            runningTaskQueue.remove(reportTask);
                            // 频道状态恢复
                            reportTask.channel.setProcessing(false);
                        }
                    } // while
                } catch (Exception e) {
                    Logger.e(PsychologyScene.class, "#run", e);
                } finally {
                    // 更新运行计数
                    numRunningTasks.decrementAndGet();

                    Logger.i(PsychologyScene.class, "Generating thread END ("
                            + numRunningTasks.get() + "/" + runningTaskQueue.size() + ") - "
                            + Thread.currentThread().getName()
                            + " - " + Utils.gsDateFormat.format(new Date(System.currentTimeMillis())));
                }
            }
        });
        thread.setName("GeneratePredictingReport-" + System.currentTimeMillis());
        thread.start();

        return report;
    }

    public PaintingReport stopGenerating(long sn) {
        Report current = this.reportMap.get(sn);
        if (!(current instanceof PaintingReport)) {
            // 没有找到报告
            Logger.i(this.getClass(), "#stopGenerating - Can NOT find report: " + sn);
            return null;
        }

        PaintingReport report = (PaintingReport) current;

        if (report.getState() == AIGCStateCode.Ok) {
            // 已经生成的报告不能停止
            Logger.i(this.getClass(), "#stopGenerating - Report is ok: " + sn);
            return null;
        }

        Iterator<ReportTask> iter = this.taskQueue.iterator();
        while (iter.hasNext()) {
            ReportTask task = iter.next();
            if (task.report.sn == sn) {
                iter.remove();
                break;
            }
        }

        // 设置状态
        report.setState(AIGCStateCode.Stopped);

        return report;
    }

    public Queue<ReportTask> getRunningTaskQueue() {
        return this.runningTaskQueue;
    }

    public int getGeneratingQueuePosition(long sn) {
        int position = 0;
        for (ReportTask task : this.taskQueue) {
            ++position;
            if (task.report.sn == sn) {
                return position;
            }
        }
        return -1;
    }

    /**
     * 重置报告的关注等级数据。
     *
     * @param reportSn
     * @param newAttention
     * @return
     */
    public PaintingReport resetReportAttention(long reportSn, Attention newAttention) {
        PaintingReport report = this.getPaintingReport(reportSn);
        if (null == report) {
            Logger.w(this.getClass(), "#resetReportAttention - Can NOT find report: " + reportSn);
            return null;
        }

        EvaluationReport evaluationReport = report.getEvaluationReport();
        Attention current = evaluationReport.getAttention();
        if (null == newAttention) {
            evaluationReport.rollAttentionSuggestion();
        }
        else {
            evaluationReport.overlayAttentionSuggestion(newAttention);
        }

        Logger.i(this.getClass(), "#resetReportAttention - Reset attention: " +
                current.level + " -> " + evaluationReport.getAttention().level);

        if (!this.storage.updatePsychologyReport(report)) {
            Logger.w(this.getClass(), "#resetReportAttention - Update report data error: " + reportSn);
            return null;
        }

        return report;
    }

    public List<Scale> listScales(long contactId) {
        return Resource.getInstance().listScales(contactId);
    }

    public Scale getScale(long sn) {
        return this.storage.readScale(sn);
    }

    public Scale generateScale(long contactId, String scaleName, Attribute attribute) {
        Scale scale = Resource.getInstance().loadScaleByName(scaleName, contactId);
        if (null == scale) {
            Logger.w(this.getClass(), "#generateScale - Can NOT find scale: " + scaleName);
            return null;
        }

        scale.setAttribute(attribute);
        this.storage.writeScale(scale);
        return scale;
    }

    public ScaleResult submitAnswerSheet(AnswerSheet answerSheet) {
        Scale scale = this.storage.readScale(answerSheet.scaleSn);
        if (null == scale) {
            Logger.w(this.getClass(), "#submitAnswerSheet - Can NOT find scale: " + answerSheet.scaleSn);
            return null;
        }

        scale.submitAnswer(answerSheet);
        this.storage.writeScale(scale);
        this.storage.writeAnswerSheet(answerSheet);

        if (!answerSheet.isValid()) {
            Logger.w(this.getClass(), "#submitAnswerSheet - The answer is NOT valid: " + scale.getSN());
        }

        if (!scale.isComplete()) {
            Logger.d(this.getClass(), "#submitAnswerSheet - Scale complete: false");
            return new ScaleResult(scale);
        }

        try {
            // 尝试推理答案评级
            this.inferScaleAnswer(scale);

            ScaleResult scaleResult = scale.scoring(Resource.getInstance().getQuestionnairesPath());
            if (null != scaleResult) {
                this.storage.writeScale(scale);
            }
            return scaleResult;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#submitAnswerSheet", e);
            return null;
        }
    }

    private ScaleResult submitScale(Scale scale) {
        try {
            // 尝试推理答案评级
            this.inferScaleAnswer(scale);

            ScaleResult scaleResult = scale.scoring(Resource.getInstance().getQuestionnairesPath());
            if (null != scaleResult) {
                if (!this.storage.writeScale(scale)) {
                    Logger.e(this.getClass(), "#submitScale - Write scale to DB failed: " + scale.getSN());
                }
            }
            return scaleResult;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#submitScale", e);
            return null;
        }
    }

    private void inferScaleAnswer(Scale scale) {
        for (Question question : scale.getQuestions()) {
            this.inferScaleAnswer(scale, question.sn);
        }
    }

    public void inferScaleAnswer(Scale scale, int questionSn) {
        Question question = scale.getQuestion(questionSn);
        if (question.isDescriptive()) {
            // 检查是否已经有答案
            if (question.hasChosen()) {
                return;
            }

            Logger.i(this.getClass(), "#inferScaleAnswer - scale: " + scale.getSN() + " - " + questionSn);

            GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                    question.makeInferencePrompt(), null, null, null);
            if (null == result) {
                result = this.service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                        question.makeInferencePrompt(), null, null, null);
            }
            if (null == result) {
                Logger.e(this.getClass(), "#inferScaleAnswer - Inference answer error: " + scale.getSN());
                // 发生错误，选择第一个
                question.chooseAnswer(question.answers.get(0).code);
                question.setInferenceResult(question.answers.get(0).content);
                return;
            }

            TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
            // 所有答案的关键字
            List<List<String>> answerKeywordList = new ArrayList<>();
            for (Answer answer : question.answers) {
                List<String> keywords = analyzer.analyzeOnlyWords(answer.content, 3);
                answerKeywordList.add(keywords);
            }

            Answer hit = null;
            int index = 0;
            for (List<String> answerKeyword : answerKeywordList) {
                for (String word : answerKeyword) {
                    if (TextUtils.isNumeric(word)) {
                        // 跳过数字
                        continue;
                    }

                    if (result.answer.contains(word)) {
                        hit = question.answers.get(index);
                        break;
                    }
                }
                ++index;
                if (null != hit) {
                    break;
                }
            }

            if (null == hit) {
                Logger.e(this.getClass(), "#inferScaleAnswer - Can NOT find answer keyword: " + scale.getSN());
                hit = question.answers.get(0);
            }

            Logger.i(this.getClass(), "#inferScaleAnswer - scale: " +  scale.getSN() +
                    " - hit: " + hit.code + " - " + hit.content);
            question.chooseAnswer(hit.code);
            question.setInferenceResult(result.answer);
        }
    }

    /**
     * 根据报告内容推荐量表。
     *
     * @param reportSn
     * @return
     */
    public Scale recommendScale(long reportSn) {
        PaintingReport report = this.getPaintingReport(reportSn);
        if (null == report) {
            Logger.w(this.getClass(), "#recommendScale - Can NOT find report: " + reportSn);
            return null;
        }

        ScaleEvaluation evaluation = new ScaleEvaluation();
        return evaluation.recommendScale(report.getEvaluationReport());
    }

    /**
     * 生成量表报告。
     *
     * @param channel
     * @param scale
     * @param language
     * @param listener
     * @return
     */
    public ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, Language language,
                                           ScaleReportListener listener) {
        if (!scale.isComplete()) {
            Logger.e(this.getClass(), "#generateScaleReport - The scale is NOT complete: " + scale.getSN());
            return null;
        }

        if (this.reportMap.containsKey(scale.getSN())) {
            Logger.e(this.getClass(), "#generateScaleReport - Submits data repeatedly: " + scale.getSN());
            return null;
        }

        if (null == scale.getResult()) {
            Logger.d(this.getClass(), "#generateScaleReport - Recalculates scale score: " + scale.getSN());
            this.submitScale(scale);
        }

        if (null == scale.getResult() || null == scale.getResult().prompt) {
            Logger.w(this.getClass(), "#generateScaleReport - Scale prompt data is null: " + scale.getSN());
            return null;
        }

        // 并发数量
        int numUnit = this.service.numUnitsByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (0 == numUnit) {
            Logger.e(this.getClass(), "#generateScaleReport - No unit");
            return null;
        }

        ScaleReport report = new ScaleReport(channel.getAuthToken().getContactId(), scale);

        ScaleReportTask task = new ScaleReportTask(channel, scale, report, listener);

        // 缓存到内存
        this.reportMap.put(report.sn, report);

        this.scaleTaskQueue.offer(task);

        if (this.numRunningScaleTasks.get() >= numUnit) {
            // 执行任务数大于单元数
            return report;
        }

        this.numRunningScaleTasks.incrementAndGet();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.i(PsychologyScene.class, "#generateScaleReport - Generating thread start");

                ScaleReportTask scaleReportTask = scaleTaskQueue.poll();
                while (null != scaleReportTask) {
                    // 回调
                    scaleReportTask.listener.onReportEvaluating(scaleReportTask.scaleReport);

                    // 生成报告
                    AIGCStateCode state = processScaleReport(scaleReportTask);
                    if (state == AIGCStateCode.Ok) {
                        // 写入数据库
                        if (!storage.writeScaleReport(scaleReportTask.scaleReport)) {
                            Logger.e(this.getClass(), "#generateScaleReport - #writeScaleReport error: "
                                    + scaleReportTask.scaleReport.sn);
                        }

                        // 变更状态
                        scaleReportTask.scaleReport.setFinished(true);
                        scaleReportTask.scaleReport.setState(AIGCStateCode.Ok);

                        // 回调
                        scaleReportTask.listener.onReportEvaluateCompleted(scaleReportTask.scaleReport);
                    }
                    else {
                        Logger.e(this.getClass(), "#generateScaleReport - #processScaleReport state: " + state.code);

                        // 变更状态
                        scaleReportTask.scaleReport.setFinished(true);
                        scaleReportTask.scaleReport.setState(state);

                        // 回调
                        scaleReportTask.listener.onReportEvaluateFailed(scaleReportTask.scaleReport);
                    }

                    scaleReportTask = scaleTaskQueue.poll();
                }

                numRunningScaleTasks.decrementAndGet();
            }
        });
        thread.setName("PsychologyScene#generateScaleReport");
        thread.start();

        return report;
    }

    public int numScaleReports(long contactId) {
        return this.storage.countScaleReports(contactId);
    }

    public int numScaleReports(long contactId, int state) {
        return this.storage.countScaleReports(contactId, state);
    }

    public int numScaleReports(long contactId, int state, boolean permissible, long startTime, long endTime) {
        return this.storage.countPsychologyReports(contactId, state, permissible, startTime, endTime);
    }

    public ScaleReport getScaleReport(long sn) {
        Report current = this.reportMap.get(sn);
        if (current instanceof ScaleReport) {
            return (ScaleReport) current;
        }

        ScaleReport report = this.storage.readScaleReport(sn);
        if (null == report) {
            Logger.w(this.getClass(), "#getScaleReport - Can NOT find scale report: " + sn);
            return null;
        }

        return report;
    }

    public List<ScaleReport> getScaleReports(long contactId, boolean descending) {
        return this.storage.readScaleReports(contactId, descending);
    }

    public List<ScaleReport> getScaleReports(long contactId, int state, boolean descending) {
        return this.storage.readScaleReports(contactId, state, descending);
    }

    public String buildPrompt(List<ConversationRelation> relations, String query) {
        StringBuilder result = new StringBuilder();

        if (relations.size() == 1) {
            ConversationRelation relation = relations.get(0);
            PaintingReport paintingReport = this.getPaintingReport(relation.reportSn);
            if (null != paintingReport) {
                QueryRevolver queryRevolver = new QueryRevolver(this.service, this.storage);
                result.append(queryRevolver.generatePrompt(relation, paintingReport, query));
            }
            else {
                ScaleReport scaleReport = this.getScaleReport(relation.reportSn);
                if (null != scaleReport) {
                    QueryRevolver queryRevolver = new QueryRevolver(this.service, this.storage);
                    result.append(queryRevolver.generatePrompt(relation, scaleReport, query));
                }
                else {
                    Logger.w(this.getClass(), "#buildPrompt - Can NOT find report: " + relation.reportSn);
                    return null;
                }
            }
        }
        else {
            List<ConversationRelation> relationList = new ArrayList<>();
            List<Report> reportList = new ArrayList<>();

            for (ConversationRelation relation : relations) {
                Report report;
                PaintingReport paintingReport = this.getPaintingReport(relation.reportSn);
                if (null == paintingReport) {
                    ScaleReport scaleReport = this.getScaleReport(relation.reportSn);
                    report = scaleReport;
                }
                else {
                    report = paintingReport;
                }
                if (null == report) {
                    Logger.w(this.getClass(), "#buildPrompt - Can NOT find report: " + relation.reportSn);
                    continue;
                }

                relationList.add(relation);
                reportList.add(report);
            }

            if (relationList.isEmpty() || reportList.isEmpty()) {
                Logger.w(this.getClass(), "#buildPrompt - No data for building prompt");
                return null;
            }

            QueryRevolver queryRevolver = new QueryRevolver(this.service, this.storage);
            result.append(queryRevolver.generatePrompt(relationList, reportList, query));
        }

        return result.toString();
    }

    /*public GeneratingRecord buildHistory(List<ConversationRelation> relations, String currentQuery) {
        ConversationRelation relation = relations.get(0);

        Report report = this.getPaintingReport(relation.reportSn);
        if (null == report) {
            report = this.getScaleReport(relation.reportSn);

            if (null == report) {
                Logger.w(this.getClass(), "#buildHistory - Can NOT find report: " + relation.reportSn);
                return null;
            }
        }

        QueryRevolver revolver = new QueryRevolver(this.service, this.storage);
        return revolver.generateSupplement(relation, report, currentQuery);
    }*/

    /**
     * 构建基于上下文数据的提示词。
     *
     * @param context
     * @param query
     * @param language
     * @return
     */
    public PromptRevolver revolve(ConversationContext context, String query, Language language) {
        QueryRevolver revolver = new QueryRevolver(this.service, this.storage);

        // 尝试情景推理
        if (null != context.getCurrentReport() && !context.getCurrentReport().isNull()) {
            PaintingReport report = context.getCurrentReport();
            if (report.getAttribute().age < 18) {
                Logger.d(this.getClass(), "#revolve - The age is less then 18: " + report.sn);

                // 添加节点
                StrategyNode detectQuery = new DetectTeenagerQueryStrategyNode(query, language);
                StrategyNode problemClassification = new TeenagerProblemClassificationNode(revolver, context.getCurrentReport());
                StrategyNode queryNode = new TeenagerQueryNode(query, revolver, context.getCurrentReport());
                // 连接节点
                detectQuery.link(problemClassification).link(queryNode);

                // 创建流
                StrategyFlow flow = new StrategyFlow(detectQuery);

                // 执行
                GeneratingRecord result = flow.generate(this.service);
                if (null != result) {
                    PromptRevolver prompt = new PromptRevolver(result.query);
                    prompt.result = result;
                    return prompt;
                }
            }
        }

        return revolver.generatePrompt(context, query);
    }

    public Painting getPainting(long reportSn) {
        Painting painting = this.paintingMap.get(reportSn);
        if (null != painting) {
            return painting;
        }
        return this.storage.readPainting(reportSn);
    }

    public PaintingFeatureSet getPaintingFeatureSet(long reportSn) {
        PaintingFeatureSet featureSet = this.storage.readPaintingFeatureSet(reportSn);
        if (null != featureSet) {
            return featureSet;
        }

        Report report = this.reportMap.get(reportSn);
        if (report instanceof PaintingReport) {
            PaintingReport paintingReport = (PaintingReport) report;
            return paintingReport.paintingFeatureSet;
        }

        return null;
    }

    public Painting getPredictedPainting(AuthToken authToken, String fileCode) {
        FileLabel fileLabel = this.service.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#getPredictedPainting - Can NOT find file: " + fileCode);
            return null;
        }

        /*
        File file = this.service.loadFile(authToken.getDomain(), fileLabel.getFileCode());
        if (null == file) {
            Logger.e(this.getClass(), "#getPredictedPainting - Can NOT find painting file: " +
                    fileCode);
            return null;
        }

        // 将文件调整为指定规格：横幅，宽1280
        File outputFile = new File(this.service.getWorkingPath(),
                FileUtils.extractFileName(file.getName()) + "_processed.jpg");
        try {
            BufferedImage image = ImageIO.read(file);
            image = ImageUtils.rotateToLandscape(image);
            image = ImageUtils.adjustAspectRatio(image);
            image = ImageUtils.resizeToDefault(image);
            ImageIO.write(image, "jpeg", outputFile);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#getPredictedPainting - Process image file error: " +
                    fileLabel.getFileCode());
            return null;
        }

        String filename = fileLabel.getFileCode() + "_predict.jpg";
        String newFileCode = FileUtils.makeFileCode(fileCode, authToken.getDomain(), filename);
        FileLabel resultFile = this.service.saveFile(authToken, newFileCode, outputFile, filename, true);
        */

        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.e(this.getClass(), "#getPredictedPainting - No unit: " + fileCode);
            return null;
        }
        // 进行绘画元素预测
        Painting resultPainting = this.processPainting(unit, fileLabel, Theme.Generic, true, true);
        if (null == resultPainting) {
            Logger.e(this.getClass(), "#getPredictedPainting - Predict painting failed: " + fileCode);
            return null;
        }
        resultPainting.fileLabel = resultPainting.processedFileLabel;
        return resultPainting;
    }

    public FileLabel getPredictedPainting(AuthToken authToken, long reportSn, boolean bbox, boolean  vparam,
                                          double probability) {
        Painting painting = this.getPainting(reportSn);
        if (null == painting) {
            Logger.w(this.getClass(), "#getPredictedPainting - Can NOT find painting: " + reportSn);
            return null;
        }

        if (null == painting.fileLabel) {
            PaintingReport report = this.getPaintingReport(reportSn);
            if (null == report) {
                Logger.w(this.getClass(), "#getPredictedPainting - Can NOT find painting: " + reportSn);
                return null;
            }
            // 设置文件
            painting.fileLabel = report.getFileLabel();
        }

        /*
        File file = this.service.loadFile(authToken.getDomain(), painting.fileLabel.getFileCode());
        if (null == file) {
            Logger.e(this.getClass(), "#getPredictedPainting - Can NOT find painting file: " +
                    painting.fileLabel.getFileCode());
            return null;
        }

        // 将文件调整为指定规格：横幅，宽1280
        File rawFile = new File(this.service.getWorkingPath(),
                FileUtils.extractFileName(file.getName()) + "_processed.jpg");
        try {
            BufferedImage image = ImageIO.read(file);
            image = ImageUtils.rotateToLandscape(image);
            image = ImageUtils.adjustAspectRatio(image);
            image = ImageUtils.resizeToDefault(image);
            ImageIO.write(image, "jpeg", rawFile);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#getPredictedPainting - Process image file error: " +
                    painting.fileLabel.getFileCode());
            return null;
        }

        String filename = painting.fileLabel.getFileCode() + "_predict.jpg";
        String tmpFileCode = FileUtils.makeFileCode(reportSn, authToken.getDomain(), filename);
        FileLabel rawFileLabel = this.service.saveFile(authToken, tmpFileCode, rawFile, filename, false);
        */

        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.e(this.getClass(), "#getPredictedPainting - No unit");
            return null;
        }
        // 进行绘画元素预测
        Painting resultPainting = this.processPainting(unit, painting.fileLabel, Theme.Generic, true, true);
        if (null == resultPainting) {
            Logger.e(this.getClass(), "#getPredictedPainting - Process painting failed");
            return null;
        }
        resultPainting.fileLabel = resultPainting.processedFileLabel;

        List<Material> materials = new ArrayList<>();
        for (Material material : resultPainting.getMaterials()) {
            if (material.prob >= probability) {
                materials.add(material);
            }
        }

        File rawFile = this.service.loadFile(authToken.getDomain(), resultPainting.fileLabel.getFileCode());

        // 绘制预测数据
        File outputFile = new File(this.service.getWorkingPath(),
                resultPainting.fileLabel.getFileCode() + "_result.jpg");
        PaintingUtils.drawMaterial(rawFile, materials, bbox, vparam, outputFile);

        if (!outputFile.exists()) {
            Logger.e(this.getClass(), "#getPredictedPainting - Drawing picture material box failed: " + reportSn);
            return null;
        }

        String filename = reportSn + "_predict.jpg";
        String tmpFileCode = FileUtils.makeFileCode(reportSn, authToken.getDomain(), filename);
        FileLabel result = this.service.saveFile(authToken, tmpFileCode, outputFile, filename, true);
        return result;
    }

    public JSONObject getPaintingInferenceData(AuthToken authToken, long reportSn) {
        PaintingReport report = this.getPaintingReport(reportSn);
        if (null == report) {
            Logger.d(this.getClass(), "#getPaintingInferenceData - Can NOT find report: " + reportSn);
            return null;
        }

        if (report.getState() == AIGCStateCode.Ok || report.getState() == AIGCStateCode.Inferencing) {
            JSONObject result = new JSONObject();
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();

            List<String> nodesNames = new ArrayList<>();
            List<String> houseCompNodeNames = new ArrayList<>();
            List<String> treeCompNodeNames = new ArrayList<>();
            List<String> personCompNodeNames = new ArrayList<>();
            List<String> otherCompNodeNames = new ArrayList<>();
            List<String> otherNodeNames = new ArrayList<>();

            EvaluationReport evaluationReport = report.getEvaluationReport();
            if (null == evaluationReport) {
                Logger.w(this.getClass(), "#getPaintingInferenceData - Evaluation data is null: " + reportSn);
                return null;
            }
            List<Representation> list = evaluationReport.getRepresentationList();
            for (Representation rep : list) {
                JSONObject node = new JSONObject();
                if (!nodesNames.contains(rep.knowledgeStrategy.getTerm().word)) {
                    nodesNames.add(rep.knowledgeStrategy.getTerm().word);
                    node.put("name", rep.knowledgeStrategy.getTerm().word);
                    nodes.put(node);
                }

                for (PerceptronThing thing : rep.things) {
                    Label label = thing.getLabel();
                    String name = thing.getName();

                    if (houseCompNodeNames.contains(name) ||
                            treeCompNodeNames.contains(name) ||
                            personCompNodeNames.contains(name) ||
                            otherCompNodeNames.contains(name) ||
                            otherNodeNames.contains(name)) {
                        continue;
                    }

                    if (Label.isHouseComponent(label)) {
                        houseCompNodeNames.add(name);
                    }
                    else if (Label.isTreeComponent(label)) {
                        treeCompNodeNames.add(name);
                    }
                    else if (Label.isPersonComponent(label)) {
                        personCompNodeNames.add(name);
                    }
                    else if (Label.isOther(label)) {
                        otherCompNodeNames.add(name);
                    }
                    else {
                        otherNodeNames.add(name);
                    }

                    if (!nodesNames.contains(name)) {
                        nodesNames.add(name);
                        node = new JSONObject();
                        node.put("name", name);
                        nodes.put(node);
                    }
                }
            }

            for (Representation rep : list) {
                int value = rep.positiveCorrelation - rep.negativeCorrelation;
                value = (value <= 0) ? 1 : value + 1;
                for (PerceptronThing thing : rep.things) {
                    String name = thing.getName();
                    JSONObject link = new JSONObject();
                    link.put("source", name);
                    link.put("target", rep.knowledgeStrategy.getTerm().word);
                    link.put("value", value);
                    links.put(link);
                }
            }

            // 组件关系
            for (Representation rep : list) {
                for (PerceptronThing thing : rep.things) {
                    Label label = thing.getLabel();
                    String name = thing.getName();
                    if (Label.isHouse(label)) {
                        for (String comp : houseCompNodeNames) {
                            JSONObject link = new JSONObject();
                            link.put("source", comp);
                            link.put("target", name);
                            link.put("value", 3);
                            links.put(link);
                        }
                    }
                    else if (Label.isTree(label)) {
                        for (String comp : treeCompNodeNames) {
                            JSONObject link = new JSONObject();
                            link.put("source", comp);
                            link.put("target", name);
                            link.put("value", 4);
                            links.put(link);
                        }
                    }
                    else if (Label.isPerson(label)) {
                        for (String comp : personCompNodeNames) {
                            JSONObject link = new JSONObject();
                            link.put("source", comp);
                            link.put("target", name);
                            link.put("value", 5);
                            links.put(link);
                        }
                    }
                }
            }

            // 子集关系
            for (Representation rep : list) {
                for (PerceptronThing thing : rep.things) {
                    if (thing.hasChildren()) {
                        for (PerceptronThing child : thing.getChildren()) {
                            JSONObject link = new JSONObject();
                            link.put("source", child.getName());
                            link.put("target", thing.getName());
                            link.put("value", thing.getValue());
                            links.put(link);
                        }
                    }
                }
            }

            result.put("nodes", nodes);
            result.put("links", links);
            return result;
        }

        return null;
    }

    public List<PaintingLabel> getPaintingLabels(long sn) {
        return this.storage.readPaintingLabels(sn);
    }

    public boolean writePaintingLabels(long sn, List<PaintingLabel> labels) {
        this.storage.deletePaintingLabel(sn);

        if (labels.isEmpty()) {
            return true;
        }
        return this.storage.writePaintingLabels(labels);
    }

    public boolean writePaintingReportState(long sn, int state) {
        return this.storage.writePaintingManagementState(sn, state);
    }

    /**
     *
     * @param authToken
     * @param streamName
     * @return
     */
    public boolean analyseStreamCounselingStrategy(AuthToken authToken, String streamName) {

        return true;
    }

    public UserProfile getAppUserProfile(AuthToken authToken) {
        UserProfile profile = new UserProfile();

        Contact contact = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        profile.totalPoints = ContactManager.getInstance().getPointSystem().total(contact);
        profile.pointList = ContactManager.getInstance().getPointSystem().listPoints(contact);

        Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(contact, Membership.STATE_NORMAL);
        if (null != membership) {
            // 是会员
            profile.membership = membership;
            if (membership.type.equals(Membership.TYPE_ORDINARY)) {
                // 本月用量
                profile.usageOfThisMonth = UserProfiles.getUsageOfThisMonth(contact.getId(), membership);
                // 每月限制
                profile.limitPerMonth = UserProfiles.gsOrdinaryMemberTimesPerMonth;
            }
            else {
                // 本月用量
                profile.usageOfThisMonth = UserProfiles.getUsageOfThisMonth(contact.getId(), membership);
                // 每月限制
                profile.limitPerMonth = UserProfiles.gsPremiumMemberTimesPerMonth;
            }
        }
        else {
            // 本月用量，非会员标记为 -1
            profile.usageOfThisMonth = -1;
        }

        // 总报告数
        profile.permissibleReports = this.numPsychologyReports(authToken.getContactId(),
                AIGCStateCode.Ok.code, true);

        List<PaintingReport> reports = this.getPsychologyReports(authToken.getContactId(), AIGCStateCode.Ok.code, 1);
        if (!reports.isEmpty()) {
            try {
                profile.hexagonScore = reports.get(0).getDimensionScore();
                profile.personality = reports.get(0).getEvaluationReport().getPersonalityAccelerator().getBigFivePersonality();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#getAppUserProfile", e);
            }
        }

        return profile;
    }

    private Painting processPainting(AIGCUnit unit, FileLabel fileLabel, Theme theme, boolean adjust, boolean upload) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        data.put("adjust", adjust);
        data.put("upload", upload);
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 5 * 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(), "#processPainting - Predict image unit error");
            return null;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#processPainting - Predict image response state: " +
                    Packet.extractCode(response));
            return null;
        }

        try {
            JSONObject responseData = Packet.extractDataPayload(response);
            // 绘画识别结果
            return new Painting(responseData.getJSONArray("result").getJSONObject(0));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#processPainting", e);
            return null;
        }
    }

    private EvaluationWorker processReport(AIGCChannel channel, Painting painting, Theme theme, AIGCUnit unit) {
        Evaluation evaluation = null;
        switch (theme) {
            case HouseTreePerson:
                evaluation = (null == painting) ?
                        new HTPEvaluation(channel.getAuthToken().getContactId(),
                                new Attribute("female", 28, channel.getLanguage(), false)) :
                        new HTPEvaluation(channel.getAuthToken().getContactId(), painting);
                break;
            case AttachmentStyle:
                evaluation = new AttachmentStyleEvaluation(channel.getAuthToken().getContactId(), painting);
                break;
            case PersonInRain:
                evaluation = new PersonInRainEvaluation(channel.getAuthToken().getContactId(), painting,
                        this.service.getTokenizer());
                break;
            case SocialIcebreakerGame:
                evaluation = new SocialIcebreakerGameEvaluation(channel.getAuthToken().getContactId(),
                        painting, this.service.getTokenizer());
                break;
            default:
                evaluation = (null == painting) ?
                        new HTPEvaluation(channel.getAuthToken().getContactId(),
                                new Attribute("male", 28, channel.getLanguage(), false)) :
                        new HTPEvaluation(channel.getAuthToken().getContactId(), painting);
                break;
        }

        // 生成评估报告
        EvaluationReport report = evaluation.makeEvaluationReport();

        EvaluationWorker evaluationWorker = new EvaluationWorker(report, this.service);
        if (report.isEmpty()) {
            Logger.w(this.getClass(), "#processReport - No things in painting: " + channel.getAuthToken().getContactId());
            return evaluationWorker;
        }

        // 设置绘画特征集
        evaluationWorker.setPaintingFeatureSet(evaluation.getPaintingFeatureSet());

        if (theme == Theme.Generic || theme == Theme.HouseTreePerson) {
            // 进行指标因子推理
            JSONObject data = new JSONObject();
            JSONArray indicators = new JSONArray();
            for (EvaluationScore es : report.getFullEvaluationScores()) {
                indicators.put(es.calcScore());
            }
            data.put("attribute", report.getAttribute().calcFactorToArray());
            data.put("indicators", indicators);
            Packet request = new Packet(AIGCAction.PredictPsychologyFactors.name, data);
            ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
            if (null != dialect) {
                Packet response = new Packet(dialect);
                if (Packet.extractCode(response) == AIGCStateCode.Ok.code) {
                    JSONObject responseData = Packet.extractDataPayload(response);
                    FactorSet factorSet = new FactorSet(responseData.getJSONObject("result"));
                    // 合并因子集合
                    evaluationWorker.mergeFactorSet(factorSet);
                }
                else {
                    Logger.w(this.getClass(), "#processReport - Predict factor response state: " +
                            Packet.extractCode(response));
                }
            }
            else {
                Logger.w(this.getClass(), "#processReport - Predict factor unit error");
            }
        }

        return evaluationWorker;
    }

    private AIGCStateCode processScaleReport(ScaleReportTask task) {
        if (task.scaleReport.getResult().prompt.isEmpty()) {
            // 没有可用的提示词
            return AIGCStateCode.Ok;
        }

        EvaluationWorker evaluationWorker = new EvaluationWorker(this.service, task.scaleReport.getAttribute());

        for (ScaleFactor factor : task.scaleReport.getFactors()) {
            ScalePrompt.Factor prompt = task.scale.getResult().prompt.getFactor(factor.name);
            if (null == prompt) {
                Logger.w(this.getClass(), "#processScaleReport - Can NOT find prompt: " + factor.name);
                return AIGCStateCode.IllegalOperation;
            }

            String description = null;
            if (evaluationWorker.fast) {
                Logger.d(this.getClass(), "processScaleReport - factor prompt: " +
                        prompt.name + " - " + prompt.description);
                description = ContentTools.extract(prompt.description, this.service.getTokenizer());
            }
            if (null == description) {
                GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                        prompt.description, new GeneratingOption(), null, null);
                if (null != result) {
                    description = result.answer;
                }
            }

            if (null == description || description.length() == 0) {
                Logger.w(this.getClass(), "#processScaleReport - Generates description error: " + factor.name);
                return AIGCStateCode.UnitError;
            }

            // 描述
            factor.description = description;

            if (null != prompt.suggestion && prompt.suggestion.length() > 3) {
                String suggestion = null;
                if (evaluationWorker.fast) {
                    Logger.d(this.getClass(), "processScaleReport - factor prompt: " +
                            prompt.name + " - " + prompt.suggestion);
                    suggestion =  ContentTools.extract(prompt.suggestion, this.service.getTokenizer());
                }
                if (null == suggestion) {
                    GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                            prompt.suggestion, new GeneratingOption(), null, null);
                    if (null != result) {
                        suggestion = result.answer;
                    }
                }

                factor.suggestion = (null != suggestion) ? suggestion : "";
            }
        }

        return AIGCStateCode.Ok;
    }

    private long lastCheckReportRetention = 0;

    public void onTick(long now) {
        Iterator<Map.Entry<Long, Report>> iter = this.reportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Report report = iter.next().getValue();
            if (now - report.timestamp > 12 * 60 * 60 * 1000) {
                iter.remove();
            }
        }

        Iterator<Map.Entry<Long, Painting>> piter = this.paintingMap.entrySet().iterator();
        while (piter.hasNext()) {
            Painting painting = piter.next().getValue();
            if (now - painting.timestamp > 12 * 60 * 60 * 1000) {
                piter.remove();
            }
        }

        // 处理超期留存报告
        if (now - this.lastConfigModified > 60 * 60 * 1000) {
            this.lastConfigModified = now;
            int count = this.storage.refreshPsychologyReportRetention();
            Logger.i(this.getClass(), "#onTick - Refresh report retention: " + count);
        }
    }

    public class ReportTask {

        protected AIGCChannel channel;

        protected Attribute attribute;

        protected FileLabel fileLabel;

        protected Theme theme;

        protected int maxIndicators;

        protected boolean adjust;

        protected PaintingReportListener listener;

        protected PaintingReport report;

        public ReportTask(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
                          Theme theme, int maxIndicators, boolean adjust,
                          PaintingReportListener listener, PaintingReport report) {
            this.channel = channel;
            this.attribute = attribute;
            this.fileLabel = fileLabel;
            this.theme = theme;
            this.maxIndicators = Math.min(maxIndicators, 36);
            this.adjust = adjust;
            this.listener = listener;
            this.report = report;
        }
    }

    public class ScaleReportTask {

        protected AIGCChannel channel;

        protected Scale scale;

        protected ScaleReport scaleReport;

        protected ScaleReportListener listener;

        public ScaleReportTask(AIGCChannel channel, Scale scale, ScaleReport scaleReport, ScaleReportListener listener) {
            this.channel = channel;
            this.scale = scale;
            this.scaleReport = scaleReport;
            this.listener = listener;
        }
    }
}
