/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.common.entity.Chart;
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
