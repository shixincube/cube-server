/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc.scene;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.*;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.GenerativeRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
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

    /**
     * Key：报告序列号。
     */
    private Map<Long, PsychologyReport> psychologyReportMap;

    private PsychologyScene() {
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.runningTaskQueue = new ConcurrentLinkedQueue<>();
        this.numRunningTasks = new AtomicInteger(0);
        this.psychologyReportMap = new ConcurrentHashMap<>();
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

            this.storage.open();
            this.storage.execSelfChecking(null);

            this.lastConfigModified = System.currentTimeMillis();

            JSONObject unitConfig = config.getJSONObject("unit");
            this.unitName = unitConfig.getString("name");
            this.unitContextLength = unitConfig.getInt("contextLength");

            // 设置存储器
            PsychologyDataManager.getInstance().setService(aigcService);
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

    public PsychologyReport getPsychologyReport(long sn) {
        PsychologyReport report = this.psychologyReportMap.get(sn);
        if (null != report) {
            return report;
        }

        report = this.storage.readPsychologyReport(sn);
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

    public int numPsychologyReports(long contactId, long startTime, long endTime) {
        return this.storage.countPsychologyReports(contactId, startTime, endTime);
    }

    public List<PsychologyReport> getPsychologyReports(long contactId, long startTime, long endTime, int pageIndex) {
        List<PsychologyReport> list = this.storage.readPsychologyReports(contactId, startTime, endTime, pageIndex);
        for (PsychologyReport report : list) {
            FileLabel fileLabel = this.aigcService.getFile(AuthConsts.DEFAULT_DOMAIN, report.getFileCode());
            if (null != fileLabel) {
                report.setFileLabel(fileLabel);
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
     * @param generatesDescription
     * @param listener
     * @return
     */
    public synchronized PsychologyReport generateEvaluationReport(AIGCChannel channel, Attribute attribute,
                FileLabel fileLabel, Theme theme, int maxIndicatorTexts, boolean generatesDescription,
                PsychologySceneListener listener) {
        // 判断属性限制
        if (attribute.age < Attribute.MIN_AGE || attribute.age > Attribute.MAX_AGE) {
            Logger.w(this.getClass(), "#generateEvaluationReport - Age param overflow: " +
                    attribute.age);
            return null;
        }

        if (null == channel) {
            Logger.e(this.getClass(), "#generateEvaluationReport - Channel is null");
            return null;
        }

        // 判断并发数量
        int numUnit = this.aigcService.numUnitsByName(ModelConfig.BAIZE_UNIT);
        if (0 == numUnit) {
            Logger.e(this.getClass(), "#generateEvaluationReport - No baize unit");
            return null;
        }

        PsychologyReport report = new PsychologyReport(channel.getAuthToken().getContactId(),
                attribute, fileLabel, theme);

        ReportTask task = new ReportTask(channel, attribute, fileLabel, theme,
                generatesDescription, maxIndicatorTexts, listener, report);

        this.taskQueue.offer(task);

        this.psychologyReportMap.put(report.sn, report);

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
//                        Logger.w(this.getClass(), "#generateEvaluationReport - Channel busy");
//                    }

                    // 设置为正在操作
                    reportTask.channel.setProcessing(true);

                    // 获取单元
                    AIGCUnit unit = aigcService.selectIdleUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                    if (null == unit) {
                        // 等待空闲单元
                        int retryCount = 0;
                        while (retryCount < 10) {
                            Logger.w(this.getClass(), "#processPainting - Unit is busy");
                            ++retryCount;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            unit = aigcService.selectIdleUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                            if (null != unit) {
                                break;
                            }
                        }
                    }

                    if (null == unit) {
                        unit = aigcService.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                        if (null == unit) {
                            // 没有可用单元
                            runningTaskQueue.remove(reportTask);
                            reportTask.channel.setProcessing(false);
                            reportTask.report.setState(AIGCStateCode.UnitError);
                            reportTask.report.setFinished(true);
                            reportTask.listener.onPaintingPredictFailed(reportTask.report);
                            continue;
                        }
                    }

                    // 更新单元状态
                    unit.setRunning(true);

                    // 加载配置
                    loadConfig();

                    // 绘图预测
                    reportTask.listener.onPaintingPredict(reportTask.report, reportTask.fileLabel);

                    Painting painting = processPainting(unit, reportTask.fileLabel);
                    if (null == painting) {
                        // 预测绘图失败
                        Logger.w(PsychologyScene.class, "#generateEvaluationReport - onPaintingPredictFailed: " +
                                reportTask.fileLabel.getFileCode());
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

                    // 绘图预测完成
                    reportTask.listener.onPaintingPredictCompleted(reportTask.report, reportTask.fileLabel, painting);

                    // 开始进行评估
                    reportTask.listener.onReportEvaluate(reportTask.report);

                    // 根据图像推理报告
                    Workflow workflow = processReport(reportTask.channel, painting, reportTask.theme,
                            reportTask.maxIndicatorTexts, reportTask.generatesDescription, new WorkflowListener() {
                                @Override
                                public void onInferCompleted(Workflow workflow) {
                                    synchronized (workflow) {
                                        workflow.notifyAll();
                                    }
                                }
                            });

                    if (null == workflow) {
                        // 推理生成报告失败
                        Logger.w(PsychologyScene.class, "#generateEvaluationReport - onReportEvaluateFailed: " +
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
                    // 更新行为推理状态
                    reportTask.report.inferenceState = AIGCStateCode.Processing;

                    // 生成 Markdown 调试信息
                    reportTask.report.makeMarkdown(false);

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

                    // 等待推理完成
                    if (!workflow.inferCompleted.get()) {
                        synchronized (workflow) {
                            try {
                                workflow.wait(3 * 60 * 1000);
                            } catch (InterruptedException e) {
                                Logger.e(PsychologyScene.class, "#workflow.wait", e);
                            }
                        }
                    }

                    // 填写数据
                    workflow.fillReport(reportTask.report);
                    // 生成 Markdown 调试信息
                    reportTask.report.makeMarkdown(false);

                    // 存储
                    storage.writePsychologyReport(reportTask.report);
                    storage.writePainting(reportTask.report.sn, reportTask.fileLabel.getFileCode(), painting);

                    // 使用数据管理器生成关联数据
                    PsychologyDataManager.getInstance().writeReportChart(reportTask.report);

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

    public PsychologyReport stopGenerating(long sn) {
        PsychologyReport report = this.psychologyReportMap.get(sn);
        if (null == report) {
            // 没有找到报告
            Logger.i(this.getClass(), "#stopGenerating - Can NOT find report: " + sn);
            return null;
        }

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

        if (!scale.isComplete()) {
            Logger.d(this.getClass(), "#submitAnswerSheet - Scale complete: false");
            return new ScaleResult(scale);
        }

        try {
            return scale.scoring(Resource.getInstance().getQuestionnairesPath());
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
        PsychologyReport report = this.getPsychologyReport(reportSn);
        if (null == report) {
            Logger.w(this.getClass(), "#recommendScale - Can NOT find report: " + reportSn);
            return null;
        }

        ScaleEvaluation evaluation = new ScaleEvaluation();
        return evaluation.recommendScale(report.getEvaluationReport());
    }

    public String buildPrompt(List<ReportRelation> relations, String query) {
        StringBuilder result = new StringBuilder();

        if (relations.size() == 1) {
            ReportRelation relation = relations.get(0);
            PsychologyReport report = this.getPsychologyReport(relation.reportSn);
            if (null == report) {
                Logger.w(this.getClass(), "#buildAddition - Can NOT find report: " + relation.reportSn);
                return null;
            }

            QueryRevolver queryRevolver = new QueryRevolver();
            result.append(queryRevolver.generatePrompt(report, query));
        }
        else {
            result.append(query);
        }

        return result.toString();
    }

    public GenerativeRecord buildHistory(List<ReportRelation> relations, String currentQuery) {
        ReportRelation relation = relations.get(0);
        PsychologyReport report = this.getPsychologyReport(relation.reportSn);
        if (null == report) {
            Logger.w(this.getClass(), "#buildHistory - Can NOT find report: " + relation.reportSn);
            return null;
        }

        StringBuilder query = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        query.append("当前我们讨论的对象（个体）有哪些信息？");
        answer.append("我们现在讨论的对象（个体）的年龄是");
        answer.append(report.getAttribute().age).append("岁");
        answer.append("，性别是").append(report.getAttribute().getGenderText()).append("性");
        answer.append("，其心理症状是：");
        int count = 0;
        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            String word = es.generateWord();
            if (null == word || word.length() == 0) {
                continue;
            }
            answer.append(word).append("、");
            ++count;
            if (count > 7) {
                break;
            }
        }
        answer.delete(answer.length() - 1, answer.length());
        answer.append("。");

        if (null != report.getEvaluationReport().getPersonalityAccelerator()) {
            answer.append("此人的大五人格画像是“");
            answer.append(report.getEvaluationReport().getPersonalityAccelerator().getBigFiveFeature().getDisplayName());
            answer.append("”。");
        }

        GenerativeRecord result = new GenerativeRecord(this.unitName, query.toString(), answer.toString());
        return result;
    }

    private Painting processPainting(AIGCUnit unit, FileLabel fileLabel) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.aigcService.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
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

    private Workflow processReport(AIGCChannel channel, Painting painting, Theme theme,
                                   int maxIndicatorText, boolean generatesDescription, WorkflowListener listener) {
        Evaluation evaluation = (null == painting) ?
                new Evaluation(new Attribute("male", 28, false)) : new Evaluation(painting);

        // 生成评估报告
        EvaluationReport report = evaluation.makeEvaluationReport();

        Workflow workflow = new Workflow(report, channel, this.aigcService);

        if (report.isEmpty()) {
            Logger.w(this.getClass(), "#processReport - No things in painting: " + channel.getAuthToken().getContactId());
            return workflow;
        }

        // 设置使用的单元
        workflow.setUnitName(this.unitName, this.unitContextLength);

        // 制作报告
        return workflow.make(theme, maxIndicatorText, generatesDescription, listener);
    }

    public void onTick(long now) {
        Iterator<Map.Entry<Long, PsychologyReport>> iter = this.psychologyReportMap.entrySet().iterator();
        while (iter.hasNext()) {
            PsychologyReport report = iter.next().getValue();
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

        protected boolean generatesDescription;

        protected PsychologySceneListener listener;

        protected PsychologyReport report;

        public ReportTask(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
                          Theme theme, boolean generatesDescription, int maxIndicatorTexts,
                          PsychologySceneListener listener, PsychologyReport report) {
            this.channel = channel;
            this.attribute = attribute;
            this.fileLabel = fileLabel;
            this.theme = theme;
            this.generatesDescription = generatesDescription;
            this.maxIndicatorTexts = Math.min(maxIndicatorTexts, 5);
            this.listener = listener;
            this.report = report;
        }
    }
}
