/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.*;
import cube.aigc.psychology.material.Label;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import cube.util.ImageUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

    private int unitContextLength;

    private Queue<ReportTask> taskQueue;

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

            this.storage.open(this.service.getTokenizer());
            this.storage.execSelfChecking(null);

            this.lastConfigModified = System.currentTimeMillis();

            // 数据管理器设置
            SceneManager.getInstance().setService(service);

            // 激活数据集
            Workflow workflow = new Workflow(service, new Attribute("male", 18, false));
            String r = workflow.infer("白泽京智");
            Logger.i(this.getClass(), "#start - Active dataset: " + r);

            String corpus = Resource.getInstance().getCorpus("baize", "MIND_ECHO");
            Logger.i(this.getClass(), "#start - Active corpus: " + corpus);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.w(this.getClass(), "#start", e);
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }
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
            return responseData.getBoolean("result");
        } catch (Exception e) {
            Logger.e(this.getClass(), "#checkPsychologyPainting", e);
            return false;
        }
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

    public int numPsychologyReports() {
        return this.storage.countPsychologyReports();
    }

    public int numPsychologyReports(long contactId, long startTime, long endTime) {
        return this.storage.countPsychologyReports(contactId, startTime, endTime);
    }

    public int numPsychologyReports(int state) {
        return this.storage.countPsychologyReports(state);
    }

    public List<PaintingReport> getPsychologyReports(int pageIndex, int pageSize, boolean descending) {
        List<PaintingReport> list = this.storage.readPsychologyReports(pageIndex, pageSize, descending);
        Iterator<PaintingReport> iter = list.iterator();
        while (iter.hasNext()) {
            PaintingReport report = iter.next();
            FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
            }
            else {
                iter.remove();
            }
        }
        return list;
    }

    public List<PaintingReport> getPsychologyReportsWithState(int pageIndex, int pageSize, boolean descending, int state) {
        List<PaintingReport> list = this.storage.readPsychologyReports(pageIndex, pageSize, descending, state);
        Iterator<PaintingReport> iter = list.iterator();
        while (iter.hasNext()) {
            PaintingReport report = iter.next();
            FileLabel fileLabel = this.service.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
            }
            else {
                iter.remove();
            }
        }
        return list;
    }

    /**
     * 根据主题生成评测报告。
     *
     * @param channel
     * @param attribute
     * @param fileLabel
     * @param theme
     * @param maxIndicatorTexts
     * @param listener
     * @return
     */
    public synchronized PaintingReport generatePredictingReport(AIGCChannel channel, Attribute attribute,
                                                                FileLabel fileLabel, Theme theme, int maxIndicatorTexts,
                                                                PaintingReportListener listener) {
        // 判断属性限制
        if (attribute.age < Attribute.MIN_AGE || attribute.age > Attribute.MAX_AGE) {
            Logger.w(this.getClass(), "#generatePredictingReport - Age param overflow: " +
                    attribute.age);
            return null;
        }

        if (null == channel) {
            Logger.e(this.getClass(), "#generatePredictingReport - Channel is null");
            return null;
        }

        // 并发数量
        int concurrency = this.service.numUnitsByName(ModelConfig.BAIZE_NEXT_UNIT) +
                this.service.numUnitsByName(ModelConfig.BAIZE_UNIT);
        if (0 == concurrency) {
            Logger.e(this.getClass(), "#generatePredictingReport - No baize unit");
            return null;
        }

        PaintingReport report = new PaintingReport(channel.getAuthToken().getContactId(),
                attribute, fileLabel, theme);

        ReportTask task = new ReportTask(channel, attribute, fileLabel, theme, maxIndicatorTexts, listener, report);

        this.taskQueue.offer(task);

        this.reportMap.put(report.sn, report);

        // 判断并发数量
        if (this.numRunningTasks.get() >= concurrency) {
            // 并发数量等于单元数量，在队列中等待
            return report;
        }

        this.numRunningTasks.incrementAndGet();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.i(PsychologyScene.class, "Generating thread start");

                while (!taskQueue.isEmpty()) {
                    final ReportTask reportTask = taskQueue.poll();
                    if (null == reportTask) {
                        continue;
                    }

                    runningTaskQueue.offer(reportTask);

                    // 设置为正在操作
                    reportTask.channel.setProcessing(true);

                    // 获取单元
                    AIGCUnit unit = service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                    if (null == unit) {
                        // 没有可用单元
                        runningTaskQueue.remove(reportTask);
                        reportTask.channel.setProcessing(false);
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

                    Painting painting = processPainting(unit, reportTask.fileLabel, true);
                    if (null == painting) {
                        // 预测绘图失败
                        Logger.w(PsychologyScene.class, "#generatePredictingReport - onPaintingPredictFailed: " +
                                reportTask.fileLabel.getFileCode());
                        // 记录故障
                        unit.markFailure(AIGCStateCode.FileError.code, System.currentTimeMillis(),
                                reportTask.channel.getAuthToken().getContactId());
                        // 更新单元状态
                        unit.setRunning(false);
                        runningTaskQueue.remove(reportTask);
                        reportTask.channel.setProcessing(false);
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
                    Workflow workflow = processReport(reportTask.channel, painting, unit);

                    // 将特征集数据填写到报告，这里仅仅是方便客户端获取特征描述文本
                    reportTask.report.paintingFeatureSet = workflow.getPaintingFeatureSet();

                    // 修改状态
                    reportTask.report.setState(AIGCStateCode.Inferencing);

                    // 执行工作流，制作报告数据
                    workflow = workflow.make(reportTask.theme, reportTask.maxIndicatorTexts);
                    if (null == workflow) {
                        // 推理生成报告失败
                        Logger.w(PsychologyScene.class, "#generatePredictingReport - onReportEvaluateFailed (IllegalOperation): " +
                                reportTask.fileLabel.getFileCode());
                        runningTaskQueue.remove(reportTask);
                        reportTask.channel.setProcessing(false);
                        reportTask.report.setState(AIGCStateCode.IllegalOperation);
                        reportTask.report.setFinished(true);
                        reportTask.listener.onReportEvaluateFailed(reportTask.report);
                        continue;
                    }

                    // 填写数据
                    workflow.fillReport(reportTask.report);

                    if (workflow.isUnknown()) {
                        // 未能处理的图片
                        Logger.w(PsychologyScene.class, "#generatePredictingReport - onReportEvaluateCompleted (InvalidData): " +
                                reportTask.fileLabel.getFileCode());
                        runningTaskQueue.remove(reportTask);
                        reportTask.channel.setProcessing(false);
                        reportTask.report.setState(AIGCStateCode.InvalidData);
                        reportTask.report.setFinished(true);

                        // 存储
                        storage.writePsychologyReport(reportTask.report);
                        storage.writePainting(reportTask.report.sn, reportTask.fileLabel.getFileCode(), painting);
                        if (null != workflow.getPaintingFeatureSet()) {
                            PaintingFeatureSet paintingFeatureSet = workflow.getPaintingFeatureSet();
                            storage.writePaintingFeatureSet(paintingFeatureSet);
                        }

                        // 按照正常状态返回
                        reportTask.listener.onReportEvaluateCompleted(reportTask.report);
//                        reportTask.listener.onReportEvaluateFailed(reportTask.report);
                        continue;
                    }

                    // 生成 Markdown 调试信息
                    reportTask.report.makeMarkdown();

                    // 设置状态
                    if (reportTask.report.isNull()) {
                        reportTask.report.setState(AIGCStateCode.Failure);
                    }
                    else {
                        reportTask.report.setState(AIGCStateCode.Ok);
                    }

                    // 修改结束状态
                    reportTask.report.setFinished(true);
                    reportTask.listener.onReportEvaluateCompleted(reportTask.report);
                    reportTask.channel.setProcessing(false);

                    // 填写数据
                    workflow.fillReport(reportTask.report);
                    // 生成 Markdown 调试信息
                    reportTask.report.makeMarkdown();

                    // 存储
                    storage.writePsychologyReport(reportTask.report);
                    storage.writePainting(reportTask.report.sn, reportTask.fileLabel.getFileCode(), painting);
                    if (null != workflow.getPaintingFeatureSet()) {
                        PaintingFeatureSet paintingFeatureSet = workflow.getPaintingFeatureSet();
                        storage.writePaintingFeatureSet(paintingFeatureSet);
                    }

                    // 使用数据管理器生成关联数据
                    SceneManager.getInstance().writeReportChart(reportTask.report);

                    // 从正在执行队列移除
                    runningTaskQueue.remove(reportTask);
                } // while

                // 更新运行计数
                numRunningTasks.decrementAndGet();

                Logger.i(PsychologyScene.class, "Generating thread end");
            }
        });
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

    public List<Scale> listScales() {
        return Resource.getInstance().listScales();
    }

    public Scale getScale(long sn) {
        return this.storage.readScale(sn);
    }

    public Scale generateScale(String scaleName, Attribute attribute) {
        Scale scale = Resource.getInstance().loadScaleByName(scaleName);
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
     * @param listener
     * @return
     */
    public ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, ScaleReportListener listener) {
        if (!scale.isComplete()) {
            Logger.e(this.getClass(), "#generateScaleReport - Scale is NOT complete: " + scale.getSN());
            return null;
        }

        if (this.reportMap.containsKey(scale.getSN())) {
            Logger.e(this.getClass(), "#generateScaleReport - Submits data repeatedly: " + scale.getSN());
            return null;
        }

        if (null == scale.getResult() || null == scale.getResult().prompt || scale.getResult().prompt.isEmpty()) {
            Logger.w(this.getClass(), "#generateScaleReport - Scale prompt data is empty: " + scale.getSN());
            return null;
        }

        // 并发数量
        int numUnit = this.service.numUnitsByName(ModelConfig.BAIZE_UNIT);
        if (0 == numUnit) {
            Logger.e(this.getClass(), "#generateScaleReport - No baize unit");
            return null;
        }

        ScaleReport report = new ScaleReport(channel.getAuthToken().getContactId(), scale);

        ScaleReportTask task = new ScaleReportTask(channel, scale, report);

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
                Logger.i(PsychologyScene.class, "Generating thread start");

                ScaleReportTask scaleReportTask = scaleTaskQueue.poll();
                while (null != scaleReportTask) {
                    // 回调
                    listener.onReportEvaluating(scaleReportTask.scaleReport);

                    // 生成报告
                    AIGCStateCode state = processScaleReport(scaleReportTask);
                    if (state == AIGCStateCode.Ok) {
                        // 写入数据库
                        storage.writeScaleReport(scaleReportTask.scaleReport);

                        // 变更状态
                        scaleReportTask.scaleReport.setFinished(true);
                        scaleReportTask.scaleReport.setState(AIGCStateCode.Ok);

                        // 回调
                        listener.onReportEvaluateCompleted(scaleReportTask.scaleReport);
                    }
                    else {
                        // 变更状态
                        scaleReportTask.scaleReport.setFinished(true);
                        scaleReportTask.scaleReport.setState(state);

                        // 回调
                        listener.onReportEvaluateFailed(scaleReportTask.scaleReport);
                    }

                    scaleReportTask = scaleTaskQueue.poll();
                }

                numRunningScaleTasks.decrementAndGet();
            }
        });
        thread.start();

        return report;
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

        Scale scale = this.getScale(sn);
        report.setScale(scale);

        return report;
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
                Report report = null;
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

    public GeneratingRecord buildHistory(List<ConversationRelation> relations, String currentQuery) {
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
    }

    /**
     * 构建基于上下文数据的提示词。
     *
     * @param context
     * @param query
     * @return
     */
    public String buildPrompt(ConversationContext context, String query) {
        QueryRevolver revolver = new QueryRevolver(this.service, this.storage);
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

        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.e(this.getClass(), "#getPredictedPainting - No unit: " + fileCode);
            return null;
        }
        // 进行绘画元素预测
        Painting resultPainting = this.processPainting(unit, resultFile, false);
        if (null == resultPainting) {
            Logger.e(this.getClass(), "#getPredictedPainting - Predict painting failed: " + fileCode);
            return null;
        }
        resultPainting.fileLabel = resultFile;
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

        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.e(this.getClass(), "#getPredictedPainting - No unit");
            return null;
        }
        // 进行绘画元素预测
        Painting resultPainting = this.processPainting(unit, rawFileLabel, false);
        if (null == resultPainting) {
            Logger.e(this.getClass(), "#getPredictedPainting - Process painting failed");
            return null;
        }
        resultPainting.fileLabel = rawFileLabel;

        List<Material> materials = new ArrayList<>();
        for (Material material : resultPainting.getMaterials()) {
            if (material.prob >= probability) {
                materials.add(material);
            }
        }
        // 绘制预测数据
        File outputFile = new File(this.service.getWorkingPath(),
                rawFileLabel.getFileCode() + "_result.jpg");
        PaintingUtils.drawMaterial(rawFile, materials, bbox, vparam, outputFile);

        // 删除中间文件
        if (rawFile.exists()) {
            rawFile.delete();
        }

        if (!outputFile.exists()) {
            Logger.e(this.getClass(), "#getPredictedPainting - Drawing picture material box failed: " + reportSn);
            return null;
        }

        filename = reportSn + "_predict.jpg";
        tmpFileCode = FileUtils.makeFileCode(reportSn, authToken.getDomain(), filename);
        FileLabel result = this.service.saveFile(authToken, tmpFileCode, outputFile, filename, true);
        return result;
    }

    public JSONObject getPaintingInferenceData(AuthToken authToken, long reportSn) {
        PaintingReport report = this.getPaintingReport(reportSn);
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

    private Painting processPainting(AIGCUnit unit, FileLabel fileLabel, boolean adjust) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        data.put("adjust", adjust);
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 90 * 1000);
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

    private Workflow processReport(AIGCChannel channel, Painting painting, AIGCUnit unit) {
        HTPEvaluation evaluation = (null == painting) ?
                new HTPEvaluation(new Attribute("male", 28, false)) : new HTPEvaluation(painting);

        // 生成评估报告
        EvaluationReport report = evaluation.makeEvaluationReport();

        Workflow workflow = new Workflow(report, channel, this.service);
        if (report.isEmpty()) {
            Logger.w(this.getClass(), "#processReport - No things in painting: " + channel.getAuthToken().getContactId());
            return workflow;
        }

        // 设置绘画特征集
        workflow.setPaintingFeatureSet(evaluation.getPaintingFeatureSet());

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
                workflow.mergeFactorSet(factorSet);
            }
            else {
                Logger.w(this.getClass(), "#processReport - Predict factor response state: " +
                        Packet.extractCode(response));
            }
        }
        else {
            Logger.w(this.getClass(), "#processReport - Predict factor unit error");
        }

        return workflow;
    }

    private AIGCStateCode processScaleReport(ScaleReportTask task) {
        Workflow workflow = new Workflow(this.service, task.scaleReport.getAttribute());

        for (ScaleFactor factor : task.scaleReport.getFactors()) {
            ScalePrompt.Factor prompt = task.scale.getResult().prompt.getFactor(factor.name);
            if (null == prompt) {
                Logger.w(this.getClass(), "#processScaleReport - Can NOT find prompt: " + factor.name);
                return AIGCStateCode.IllegalOperation;
            }

            String description = null;
            if (workflow.isSpeed()) {
                Logger.d(this.getClass(), "processScaleReport - factor prompt: " +
                        prompt.name + " - " + prompt.description);
                description = workflow.infer(prompt.description);
            }
            if (null == description) {
                GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt.description, new GeneratingOption(),
                        null, null);
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
                if (workflow.isSpeed()) {
                    Logger.d(this.getClass(), "processScaleReport - factor prompt: " +
                            prompt.name + " - " + prompt.suggestion);
                    suggestion = workflow.infer(prompt.suggestion);
                }
                if (null == suggestion) {
                    GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt.suggestion, new GeneratingOption(),
                            null, null);
                    if (null != result) {
                        suggestion = result.answer;
                    }
                }

                factor.suggestion = (null != suggestion) ? suggestion : "";
            }
        }

        return AIGCStateCode.Ok;
    }

    public void onTick(long now) {
        Iterator<Map.Entry<Long, Report>> iter = this.reportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Report report = iter.next().getValue();
            if (now - report.timestamp > 72 * 60 * 60 * 1000) {
                iter.remove();
            }
        }

        Iterator<Map.Entry<Long, Painting>> piter = this.paintingMap.entrySet().iterator();
        while (piter.hasNext()) {
            Painting painting = piter.next().getValue();
            if (now - painting.timestamp > 72 * 60 * 60 * 1000) {
                piter.remove();
            }
        }
    }

    public class ReportTask {

        protected AIGCChannel channel;

        protected Attribute attribute;

        protected FileLabel fileLabel;

        protected Theme theme;

        protected int maxIndicatorTexts;

        protected PaintingReportListener listener;

        protected PaintingReport report;

        public ReportTask(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
                          Theme theme, int maxIndicatorTexts,
                          PaintingReportListener listener, PaintingReport report) {
            this.channel = channel;
            this.attribute = attribute;
            this.fileLabel = fileLabel;
            this.theme = theme;
            this.maxIndicatorTexts = Math.min(maxIndicatorTexts, 36);
            this.listener = listener;
            this.report = report;
        }
    }

    public class ScaleReportTask {

        protected AIGCChannel channel;

        protected Scale scale;

        protected ScaleReport scaleReport;

        public ScaleReportTask(AIGCChannel channel, Scale scale, ScaleReport scaleReport) {
            this.channel = channel;
            this.scale = scale;
            this.scaleReport = scaleReport;
        }
    }
}
