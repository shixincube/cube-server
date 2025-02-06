/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.Utils;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.common.entity.AIGCChatHistory;
import cube.common.entity.Chart;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心理学模块数据管理器。
 */
public class SceneManager {

    private final static SceneManager instance = new SceneManager();

    private AIGCService aigcService;

    private Map<String, ConversationContext> conversationContexts;

    private SceneManager() {
        this.conversationContexts = new ConcurrentHashMap<>();
    }

    public static SceneManager getInstance() {
        return SceneManager.instance;
    }

    public void setService(AIGCService aigcService) {
        this.aigcService = aigcService;
    }

    public ConversationContext getConversationContext(String channelCode) {
        return this.conversationContexts.get(channelCode);
    }

    public void putConversationContext(String channelCode, ConversationContext context) {
        this.conversationContexts.put(channelCode, context);
    }

    public void writeRecord(String channelCode, String unitName, GeneratingRecord record) {
        AIGCChatHistory history = new AIGCChatHistory(Utils.generateSerialNumber(), channelCode, unitName, null);
        history.queryContent = record.query;
        history.queryTime = record.timestamp;
        history.queryFileLabels = record.queryFileLabels;
        history.answerContent = record.answer;
        history.answerTime = System.currentTimeMillis();
        history.answerFileLabels = record.answerFileLabels;
        history.context = record.context;
        this.aigcService.getStorage().writeChatHistory(history);
    }

    public Chart readReportChart(long reportSn) {
        if (null == this.aigcService) {
            return null;
        }

        return this.aigcService.getStorage().readLastChart(String.valueOf(reportSn));
    }

    public boolean writeReportChart(PaintingReport report) {
        if (null == this.aigcService) {
            return false;
        }

        Chart chart = new Chart(String.valueOf(report.sn),
                "$" + report.sn + "$", report.timestamp);
        chart.setLegend(Arrays.asList("负权重分", "正权重分"));
        chart.setXAxis(Chart.AXIS_TYPE_VALUE);

        Chart.Axis yAxis = chart.setYAxis(Chart.AXIS_TYPE_CATEGORY);
        yAxis.axisTick = new JSONObject();
        yAxis.axisTick.put("show", false);

        yAxis.data = new ArrayList<>();
        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            yAxis.data.add(es.indicator.name);
        }

        Chart.Series series = chart.addSeries("负权重分", "bar");
        series.stack = "Total";
        series.setLabel(true).position = "left";
        series.emphasis = new JSONObject();
        series.emphasis.put("focus", "series");
        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            series.data.add(- (int) Math.round(es.negativeScore * 100));
        }

        series = chart.addSeries("正权重分", "bar");
        series.stack = "Total";
        series.setLabel(true).position = "right";
        series.emphasis = new JSONObject();
        series.emphasis.put("focus", "series");
        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            series.data.add((int) Math.round(es.positiveScore * 100));
        }

        // 插入数据库
        return this.aigcService.getStorage().insertChart(chart);
    }
}
