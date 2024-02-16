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
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心理学场景。
 */
public class PsychologyScene {

    private final static PsychologyScene instance = new PsychologyScene();

    private AIGCService aigcService;

    /**
     * Key：报告序列号。
     */
    private Map<Long, PsychologyReport> psychologyReportMap;

    private PsychologyScene() {
        this.psychologyReportMap = new ConcurrentHashMap<>();
    }

    public static PsychologyScene getInstance() {
        return PsychologyScene.instance;
    }

    public void setAigcService(AIGCService aigcService) {
        this.aigcService = aigcService;
    }

    public PsychologyReport getPsychologyReport(long sn) {
        return this.psychologyReportMap.get(sn);
    }

    public PsychologyReport getPsychologyReportByFileCode(String fileCode) {
        for (Map.Entry<Long, PsychologyReport> e : this.psychologyReportMap.entrySet()) {
            if (e.getValue().getFileLabel().getFileCode().equals(fileCode)) {
                return e.getValue();
            }
        }
        return null;
    }

    public PsychologyReport generateEvaluationReport(AIGCChannel channel, ReportAttribute reportAttribute, FileLabel fileLabel,
                                                     Theme theme, PsychologySceneListener listener) {
        // 判断频道是否繁忙
        if (null == channel || channel.isProcessing()) {
            Logger.w(this.getClass(), "#generateEvaluationReport - Channel error");
            return null;
        }

        // 设置为正在操作
        channel.setProcessing(true);

        PsychologyReport report = new PsychologyReport(channel.getAuthToken().getCode(),
                reportAttribute, fileLabel, theme);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 绘图预测
                listener.onPaintingPredict(report, fileLabel);

                Painting painting = null;//processPainting(fileLabel);
//                if (null == painting) {
//                    // 预测绘图失败
//                    channel.setProcessing(false);
//                    listener.onPaintingPredictFailed(report, fileLabel);
//                    return;
//                }

                // 设置绘画作者属性
//                painting.setAuthor(author);

                // 绘图预测完成
                listener.onPaintingPredictCompleted(report, fileLabel, painting);

                // 开始进行评估
                listener.onReportEvaluate(report);

                // 根据图像推理报告
                Workflow workflow = processReport(channel, painting, theme);
                if (null == workflow) {
                    // 推理生成报告失败
                    channel.setProcessing(false);
                    listener.onReportEvaluateFailed(report);
                    return;
                }

                workflow.fillReport(report);

                channel.setProcessing(false);
                listener.onReportEvaluateCompleted(report);

                // 记录
//                List<PsychologyReport> list = psychologyReportMap.get(channel.getAuthToken().getCode());
//                if (null == list) {
//                    list = new ArrayList<>();
//                    psychologyReportMap.put(channel.getAuthToken().getCode(), list);
//                }
//                list.add(report);

                channel.setProcessing(false);
            }
        });
        thread.start();

        this.psychologyReportMap.put(report.sn, report);

        return report;
    }

    private Painting processPainting(FileLabel fileLabel) {
        AIGCUnit unit = this.aigcService.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#processPainting - Can NOT find CV unit in server");
            return null;
        }

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

        JSONObject responseData = Packet.extractDataPayload(response);
        // 绘画识别结果
        return new Painting(responseData.getJSONArray("result").getJSONObject(0));
    }

    private Workflow processReport(AIGCChannel channel, Painting painting, Theme theme) {
        Evaluation evaluation = (null == painting) ?
                new Evaluation(new ReportAttribute("男", 28)) : new Evaluation(painting);

        EvaluationReport report = evaluation.makeEvaluationReport();

        Workflow workflow = new Workflow(report, channel, this.aigcService);

        switch (theme) {
            case Stress:
                workflow.makeStress();
                break;
            case FamilyRelationships:
                break;
            case Intimacy:
                break;
            case Cognition:
                break;
            default:
                workflow = null;
                break;
        }

        return workflow;
    }

    public void onTick(long now) {
        Iterator<Map.Entry<Long, PsychologyReport>> iter = this.psychologyReportMap.entrySet().iterator();
        while (iter.hasNext()) {
            PsychologyReport report = iter.next().getValue();
            if (now - report.timestamp > 30 * 60 * 1000) {
                iter.remove();
            }
        }
    }

    public static void main(String[] args) {
        PsychologyScene scene = PsychologyScene.getInstance();

        AuthToken authToken = new AuthToken(Utils.randomString(16), "shixincube.com", "AppKey",
                1000L, System.currentTimeMillis(), System.currentTimeMillis() + 60 * 60 * 1000, false);
        AIGCChannel channel = new AIGCChannel(authToken, "Test");
        scene.processReport(channel, null, Theme.Stress);
    }
}
