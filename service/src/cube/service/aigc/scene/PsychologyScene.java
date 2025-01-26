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
import cube.aigc.psychology.composition.*;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
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

    private AIGCService aigcService;

    private PsychologyStorage storage;

    private long lastConfigModified;

    private String unitName;

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

    private PsychologyScene() {
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.runningTaskQueue = new ConcurrentLinkedQueue<>();
        this.numRunningTasks = new AtomicInteger(0);
        this.scaleTaskQueue = new ConcurrentLinkedQueue<>();
        this.numRunningScaleTasks = new AtomicInteger(0);
        this.reportMap = new ConcurrentHashMap<>();
    }

    public static PsychologyScene getInstance() {
        return PsychologyScene.instance;
    }

    public void start(AIGCService aigcService) {
        this.aigcService = aigcService;

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

            this.storage.open(this.aigcService.getTokenizer());
            this.storage.execSelfChecking(null);

            this.lastConfigModified = System.currentTimeMillis();

            JSONObject unitConfig = config.getJSONObject("unit");
            this.unitName = unitConfig.getString("name");
            this.unitContextLength = unitConfig.getInt("contextLength");

            // 数据管理器设置
            SceneManager.getInstance().setService(aigcService);

            // 激活数据集
            Workflow workflow = new Workflow(aigcService, new Attribute("male", 18, false));
            String r = workflow.infer("白泽京智");
            Logger.i(this.getClass(), "#start - Active dataset: " + r);
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

            this.lastConfigModified = file.lastModified();

            JSONObject config = ConfigUtils.readJsonFile("psychology.json");
            JSONObject unitConfig = config.getJSONObject("unit");
            this.unitName = unitConfig.getString("name");
            this.unitContextLength = unitConfig.getInt("contextLength");
        } catch (Exception e) {
            Logger.w(this.getClass(), "#loadConfig", e);
        }
    }

    public String getUnitName() {
        return this.unitName;
    }

    public boolean recordUsage(Usage usage) {
        return this.storage.writeUsage(usage.cid, usage.token, usage.timestamp, usage.remoteHost, usage.query,
                usage.queryType, usage.queryTokens, usage.completionTokens, usage.completionSN);
    }

    public boolean checkPsychologyPainting(AuthToken authToken, String fileCode) {
        FileLabel fileLabel = this.aigcService.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - File error: " + fileCode);
            return false;
        }

        AIGCUnit unit = this.aigcService.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - No psychology unit: " + fileCode);
            return false;
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        Packet request = new Packet(AIGCAction.CheckPsychologyPainting.name, data);
        ActionDialect dialect = this.aigcService.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
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

        FileLabel fileLabel = this.aigcService.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
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
            FileLabel fileLabel = this.aigcService.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
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
            FileLabel fileLabel = this.aigcService.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
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
        int numUnit = this.aigcService.numUnitsByName(ModelConfig.BAIZE_UNIT);
        if (0 == numUnit) {
            Logger.e(this.getClass(), "#generatePredictingReport - No baize unit");
            return null;
        }

        PaintingReport report = new PaintingReport(channel.getAuthToken().getContactId(),
                attribute, fileLabel, theme);

        ReportTask task = new ReportTask(channel, attribute, fileLabel, theme, maxIndicatorTexts, listener, report);

        this.taskQueue.offer(task);

        this.reportMap.put(report.sn, report);

        // 判断并发数量
        if (this.numRunningTasks.get() >= numUnit) {
            // 并发数量等于单元数量，在队列中等待
            return report;
        }

        this.numRunningTasks.incrementAndGet();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.i(PsychologyScene.class, "Generating thread start");

                while (!taskQueue.isEmpty()) {
                    ReportTask reportTask = taskQueue.poll();
                    if (null == reportTask) {
                        continue;
                    }

                    runningTaskQueue.offer(reportTask);

                    // 判断频道是否繁忙
//                    if (channel.isProcessing()) {
//                        Logger.w(this.getClass(), "#generatePredictingReport - Channel busy");
//                    }

                    // 设置为正在操作
                    reportTask.channel.setProcessing(true);

                    // 获取单元
                    AIGCUnit unit = aigcService.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
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

                    // 绘图预测
                    reportTask.listener.onPaintingPredicting(reportTask.report, reportTask.fileLabel);

                    Painting painting = processPainting(unit, reportTask.fileLabel);
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

                    // 关联绘画
                    reportTask.report.painting = painting;

                    // 绘图预测完成
                    reportTask.listener.onPaintingPredictCompleted(reportTask.report, reportTask.fileLabel, painting);

                    // 开始进行评估
                    reportTask.listener.onReportEvaluating(reportTask.report);

                    // 根据图像推理报告
                    Workflow workflow = processReport(reportTask.channel, painting, reportTask.theme, unit,
                            reportTask.maxIndicatorTexts);

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
        int numUnit = this.aigcService.numUnitsByName(ModelConfig.BAIZE_UNIT);
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

    public String buildPrompt(List<ReportRelation> relations, String query) {
        StringBuilder result = new StringBuilder();

        if (relations.size() == 1) {
            ReportRelation relation = relations.get(0);
            PaintingReport paintingReport = this.getPaintingReport(relation.reportSn);
            if (null != paintingReport) {
                QueryRevolver queryRevolver = new QueryRevolver(this.aigcService, this.storage);
                result.append(queryRevolver.generatePrompt(relation, paintingReport, query));
            }
            else {
                ScaleReport scaleReport = this.getScaleReport(relation.reportSn);
                if (null != scaleReport) {
                    QueryRevolver queryRevolver = new QueryRevolver(this.aigcService, this.storage);
                    result.append(queryRevolver.generatePrompt(relation, scaleReport, query));
                }
                else {
                    Logger.w(this.getClass(), "#buildPrompt - Can NOT find report: " + relation.reportSn);
                    return null;
                }
            }
        }
        else {
            List<ReportRelation> relationList = new ArrayList<>();
            List<Report> reportList = new ArrayList<>();

            for (ReportRelation relation : relations) {
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

            QueryRevolver queryRevolver = new QueryRevolver(this.aigcService, this.storage);
            result.append(queryRevolver.generatePrompt(relationList, reportList, query));
        }

        return result.toString();
    }

    public GeneratingRecord buildHistory(List<ReportRelation> relations, String currentQuery) {
        ReportRelation relation = relations.get(0);

        Report report = this.getPaintingReport(relation.reportSn);
        if (null == report) {
            report = this.getScaleReport(relation.reportSn);

            if (null == report) {
                Logger.w(this.getClass(), "#buildHistory - Can NOT find report: " + relation.reportSn);
                return null;
            }
        }

        QueryRevolver revolver = new QueryRevolver(this.aigcService, this.storage);
        return revolver.generateSupplement(relation, report, currentQuery);
    }

    public String buildPrompt(GeneratingRecord context, String query) {
        final StringBuilder result = new StringBuilder();

        boolean success = this.aigcService.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                List<QuestionAnswer> list = new ArrayList<>();
                for (QuestionAnswer qa : questionAnswers) {
                    if (qa.getScore() > 0.8) {
                        list.add(qa);
                    }
                }

                if (!list.isEmpty()) {
                    result.append("已知信息：\n\n");
                    for (QuestionAnswer qa : list) {
                        for (String answer : qa.getAnswers()) {
                            result.append(answer).append("\n");
                        }
                    }
                    result.append("\n根据以上信息，专业地回答问题，如果无法从中得到答案，请说“暂时没有足够的相关信息。”，不允许在答案中添加编造成分。问题是：");
                    result.append(query);
                }

                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (result.length() == 0) {
            result.append("专业地回答问题，在答案最后增加一句：“建议您可以问一些心理知识相关的问题。”，问题是：");
            result.append(query);
        }

        return result.toString();
    }

    public Painting getPainting(long reportSn) {
        return this.storage.readPainting(reportSn);
    }

    public FileLabel getPredictedPainting(long reportSn) {
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

    private Painting processPainting(AIGCUnit unit, FileLabel fileLabel) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.aigcService.getCellet().transmit(unit.getContext(), request.toDialect(), 90 * 1000);
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

    private Workflow processReport(AIGCChannel channel, Painting painting, Theme theme, AIGCUnit unit,
                                   int maxIndicatorText) {
        HTPEvaluation evaluation = (null == painting) ?
                new HTPEvaluation(new Attribute("male", 28, false)) : new HTPEvaluation(painting);

        // 生成评估报告
        EvaluationReport report = evaluation.makeEvaluationReport();

        Workflow workflow = new Workflow(report, channel, this.aigcService);
        if (report.isEmpty()) {
            Logger.w(this.getClass(), "#processReport - No things in painting: " + channel.getAuthToken().getContactId());
            return workflow;
        }

        // 设置使用的单元
        workflow.setUnitName(this.unitName);

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
        ActionDialect dialect = this.aigcService.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
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

        // 制作报告
        return workflow.make(theme, maxIndicatorText);
    }

    private AIGCStateCode processScaleReport(ScaleReportTask task) {
        Workflow workflow = new Workflow(this.aigcService, task.scaleReport.getAttribute());

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
                description = this.aigcService.syncGenerateText(this.unitName, prompt.description, new GeneratingOption(),
                        null, null);
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
                    suggestion = this.aigcService.syncGenerateText(this.unitName, prompt.suggestion, new GeneratingOption(),
                            null, null);
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
