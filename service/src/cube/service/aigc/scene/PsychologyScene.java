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
import cube.storage.StorageType;
import cube.util.ConfigUtils;
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

    private PsychologyStorage storage;

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

    public void start(AIGCService aigcService) {
        this.aigcService = aigcService;

        try {
            JSONObject config = ConfigUtils.readJsonFile("psychology.json");
            JSONObject storage = config.getJSONObject("storage");
            if (storage.getString("type").equalsIgnoreCase("SQLite")) {
                this.storage = new PsychologyStorage(StorageType.SQLite, storage);
            }
            else {
                this.storage = new PsychologyStorage(StorageType.MySQL, storage);
            }

            this.storage.open();
            this.storage.execSelfChecking(null);
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

    public PsychologyReport getPsychologyReport(long sn) {
        PsychologyReport report = this.psychologyReportMap.get(sn);
        if (null != report) {
            return report;
        }

        report = this.storage.readPsychologyReport(sn);
        if (null == report) {
            return null;
        }

        FileLabel fileLabel = this.aigcService.getFile(report.getFileCode());
        if (null == fileLabel) {
            return null;
        }

        report.setFileLabel(fileLabel);
        return report;
    }

    public PsychologyReport getPsychologyReportByFileCode(String fileCode) {
        for (Map.Entry<Long, PsychologyReport> e : this.psychologyReportMap.entrySet()) {
            if (e.getValue().getFileLabel().getFileCode().equals(fileCode)) {
                return e.getValue();
            }
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
     * @param paragraphInferrable 是否对主题的段落进行推理
     * @param listener
     * @return
     */
    public PsychologyReport generateEvaluationReport(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
                                                     Theme theme, boolean paragraphInferrable,
                                                     PsychologySceneListener listener) {
        // 判断频道是否繁忙
        if (null == channel || channel.isProcessing()) {
            Logger.w(this.getClass(), "#generateEvaluationReport - Channel error");
            return null;
        }

        // 设置为正在操作
        channel.setProcessing(true);

        PsychologyReport report = new PsychologyReport(channel.getAuthToken().getContactId(),
                attribute, fileLabel, theme);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 绘图预测
                listener.onPaintingPredict(report, fileLabel);

                Painting painting = processPainting(fileLabel);
                if (null == painting) {
                    // 预测绘图失败
                    Logger.w(PsychologyScene.class, "#generateEvaluationReport - onPaintingPredictFailed: " +
                            fileLabel.getFileCode());
                    channel.setProcessing(false);
                    report.setState(AIGCStateCode.FileError);
                    report.setFinished(true);
                    listener.onPaintingPredictFailed(report, fileLabel);
                    return;
                }

                // 设置绘画属性
                painting.setAttribute(attribute);

                // 绘图预测完成
                listener.onPaintingPredictCompleted(report, fileLabel, painting);

                // 开始进行评估
                listener.onReportEvaluate(report);

                // 根据图像推理报告
                Workflow workflow = processReport(channel, painting, theme, paragraphInferrable);
                if (null == workflow) {
                    // 推理生成报告失败
                    Logger.w(PsychologyScene.class, "#generateEvaluationReport - onReportEvaluateFailed: " +
                            fileLabel.getFileCode());
                    channel.setProcessing(false);
                    report.setState(AIGCStateCode.IllegalOperation);
                    report.setFinished(true);
                    listener.onReportEvaluateFailed(report);
                    return;
                }

                // 填写数据
                workflow.fillReport(report);

                // 设置状态
                if (report.isEmpty()) {
                    report.setState(AIGCStateCode.Failure);
                }
                else {
                    report.setState(AIGCStateCode.Ok);
                }

                // 修改结束状态
                report.setFinished(true);

                listener.onReportEvaluateCompleted(report);

                // 存储
                storage.writePsychologyReport(report);

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

    private Workflow processReport(AIGCChannel channel, Painting painting, Theme theme, boolean paragraphInferrable) {
        Evaluation evaluation = (null == painting) ?
                new Evaluation(new Attribute("male", 28)) : new Evaluation(painting);

        EvaluationReport report = evaluation.makeEvaluationReport();

        Workflow workflow = new Workflow(report, channel, this.aigcService);

        if (report.isEmpty()) {
            Logger.w(this.getClass(), "#processReport - No things in painting: " + channel.getAuthToken().getContactId());
            return workflow;
        }

        // 设置使用的单元
        workflow.setUnitName("Baize", 1200);

        // 制作报告
        return workflow.make(theme, paragraphInferrable);
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
        scene.processReport(channel, null, Theme.Stress, false);
    }
}
