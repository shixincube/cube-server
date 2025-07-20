/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.TextToImageListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TextToImageUnitMeta extends UnitMeta {

    protected long sn;

    protected AIGCChannel channel;

    protected String text;

    protected FileLabel fileLabel;

    protected TextToImageListener listener;

    protected AIGCChatHistory history;

    public TextToImageUnitMeta(AIGCService service, AIGCUnit unit, AIGCChannel channel, String text, TextToImageListener listener) {
        super(service, unit);
        this.sn = Utils.generateSerialNumber();
        this.channel = channel;
        this.text = text;
        this.listener = listener;

        this.history = new AIGCChatHistory(this.sn, channel.getCode(), unit.getCapability().getName(),
                channel.getDomain().getName());
        this.history.queryContactId = channel.getAuthToken().getContactId();
        this.history.queryTime = System.currentTimeMillis();
        this.history.queryContent = text;
    }

    @Override
    public void process() {
        this.channel.setLastUnitMetaSn(this.sn);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                listener.onProcessing(channel);
            }
        });

        JSONObject data = new JSONObject();
        data.put("text", this.text);

        Packet request = new Packet(AIGCAction.TextToImage.name, data);
        // 使用较长的超时时间，以便等待上传数据
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(),
                180 * 1000, this.sn);
        if (null == dialect) {
            Logger.w(AIGCService.class, "TextToImage unit error");
            this.channel.setProcessing(false);
            // 回调错误
            this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
            return;
        }

        // 是否被中断
        if (this.service.getCellet().isInterruption(dialect)) {
            Logger.d(AIGCService.class, "Channel interrupted: " + this.channel.getCode());
            this.channel.setProcessing(false);
            // 回调错误
            this.listener.onFailed(this.channel, AIGCStateCode.Interrupted);
            return;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);
        if (payload.has("fileLabel")) {
            this.fileLabel = new FileLabel(payload.getJSONObject("fileLabel"));
            this.fileLabel.resetURLsToken(this.channel.getAuthToken().getCode());

            // 记录
            GeneratingRecord record = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                    this.text, this.fileLabel);
            this.listener.onCompleted(record);

            // 填写历史
            this.history.answerContactId = this.unit.getContact().getId();
            this.history.answerTime = System.currentTimeMillis();
            this.history.answerContent = this.fileLabel.toCompactJSON().toString();

            this.history.answerFileLabels = new ArrayList<>();
            this.history.answerFileLabels.add(this.fileLabel);

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // 计算用量
                    long contactId = channel.getAuthToken().getContactId();
                    List<String> tokens = calcTokens(text);
                    long promptTokens = tokens.size();
                    long completionTokens = (long) Math.floor(fileLabel.getFileSize() / 1024.0);
                    service.getStorage().updateUsage(contactId, ModelConfig.getModelByUnit(history.unit),
                            completionTokens, promptTokens);

                    // 保存历史记录
                    service.getStorage().writeHistory(history);
                }
            });
        }
        else {
            this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
        }

        // 重置状态
        this.channel.setProcessing(false);
    }
}
