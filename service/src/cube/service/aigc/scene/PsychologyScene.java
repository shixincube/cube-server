/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心理学场景。
 */
public class PsychologyScene {

    private final static PsychologyScene instance = new PsychologyScene();

    private AIGCService aigcService;

    /**
     * Key：令牌码。
     */
    private Map<String, List<PsychologyReport>> psychologyReportMap;

    private PsychologyScene() {
        this.psychologyReportMap = new ConcurrentHashMap<>();
    }

    public static PsychologyScene getInstance() {
        return PsychologyScene.instance;
    }

    public void setAigcService(AIGCService aigcService) {
        this.aigcService = aigcService;
    }

    public PsychologyReport getPsychologyReport(String token, String fileCode) {
        List<PsychologyReport> list = this.psychologyReportMap.get(token);
        if (null == list) {
            return null;
        }
        for (PsychologyReport report : list) {
            if (report.getFileLabel().getFileCode().equals(fileCode)) {
                return report;
            }
        }
        return null;
    }

    public PsychologyReport generateEvaluationReport(AIGCChannel channel, FileLabel fileLabel,
                                                     Theme theme, SceneListener listener) {
        // 判断频道是否繁忙
        if (null == channel || channel.isProcessing()) {
            Logger.w(this.getClass(), "#generateEvaluationReport - Channel error");
            return null;
        }

        // 设置为正在操作
        channel.setProcessing(true);

        PsychologyReport report = new PsychologyReport(fileLabel, theme, channel);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 处理绘图预测
                Painting painting = processPainting(fileLabel);
                if (null == painting) {
                    // 预测绘图失败
                    report.resetPhase(PsychologyReport.PHASE_PREDICT_FAILED);
                    channel.setProcessing(false);
                    listener.onPaintingPredictFailed(report);
                    return;
                }

                // 绘图预测完成
                listener.onPaintingPredictCompleted(report, painting);
                report.resetPhase(PsychologyReport.PHASE_INFER);

                // 根据图像推理报告
                Workflow workflow = processReport(channel, painting, theme);
                if (null == workflow) {
                    // 推理生成报告失败
                    report.resetPhase(PsychologyReport.PHASE_INFER_FAILED);
                    channel.setProcessing(false);
                    listener.onReportEvaluateFailed(report);
                    return;
                }

                report.setWorkflow(workflow);
                report.resetPhase(PsychologyReport.PHASE_FINISH);
                channel.setProcessing(false);
                listener.onReportEvaluated(report);

                // 记录
                List<PsychologyReport> list = psychologyReportMap.get(channel.getAuthToken().getCode());
                if (null == list) {
                    list = new ArrayList<>();
                    psychologyReportMap.put(channel.getAuthToken().getCode(), list);
                }
                list.add(report);
            }
        });
        thread.start();

        return report;
    }

    private Painting processPainting(FileLabel fileLabel) {
        AIGCUnit unit = this.aigcService.selectUnitByName(ModelConfig.PREFERENCE_UNIT_FOR_CV);
        if (null == unit) {
            Logger.w(this.getClass(), "#processPainting - Can NOT find CV unit in server");
            return null;
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        Packet request = new Packet(AIGCAction.PredictImage.name, data);
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

        JSONObject payload = Packet.extractDataPayload(response);
        // 绘画识别结果
        Painting painting = new Painting(payload);
        return painting;
    }

    private Workflow processReport(AIGCChannel channel, Painting painting, Theme theme) {
        Workflow workflow = null;

        Evaluation evaluation = (null == painting) ? new Evaluation() : new Evaluation(painting);

        EvaluationReport report = evaluation.makeEvaluationReport();

        switch (theme) {
            case Stress:
                workflow = report.makeStress(channel, this.aigcService);
                break;
            case FamilyRelationships:
                break;
            case Intimacy:
                break;
            case Cognition:
                break;
            default:
                break;
        }



        return workflow;
    }
}
