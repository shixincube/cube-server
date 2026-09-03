/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.MultimodalListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MultimodalUnitMeta extends UnitMeta {

    public final long sn;

    private final AIGCChannel channel;

    private final MultimodalInput input;

    private final MultimodalListener listener;

    private MultimodalOutput output;

    public MultimodalUnitMeta(AIGCService service, AIGCUnit unit, AIGCChannel channel,
                              MultimodalInput input, MultimodalListener listener) {
        super(service, unit);
        this.sn = Utils.generateSerialNumber();
        this.channel = channel;
        this.input = input;
        this.listener = listener;
    }

    @Override
    public void process() {
        try {
            // 处理文件
            List<FileLabel> fileLabelList = new ArrayList<>();
            for (String filepath : this.input.files) {
                if (filepath.startsWith("http")) {
                    // 下载文件
                    FileLabel fileLabel = this.service.downloadFile(this.channel.getAuthToken(), filepath);
                    if (null != fileLabel) {
                        fileLabelList.add(fileLabel);
                    }
                }
                else {
                    // 加载文件
                    FileLabel fileLabel = this.service.getFile(this.channel.getAuthToken().getDomain(), filepath);
                    if (null != fileLabel) {
                        fileLabelList.add(fileLabel);
                    }
                }
            }

            if (fileLabelList.isEmpty()) {
                Logger.w(this.getClass(), "#process - Get files failed: " + this.channel.getCode());
                this.listener.onFailed(this.channel, AIGCStateCode.InvalidParameter);
                return;
            }

            // 设置文件标签清单
            this.input.fileLabels = fileLabelList;

            Packet request = new Packet(AIGCAction.Multimodal.name, this.input.toUnitJson());
            ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(),
                    3 * 60 * 1000, this.sn);
            // 处理单元返回
            if (null == dialect) {
                Logger.w(this.getClass(), "#process - return null: " + this.unit.getCapability().getName());
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                return;
            }

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                Logger.w(this.getClass(), "#process - multimodal failed: " + Packet.extractCode(response));
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.Failure);
                return;
            }

            JSONObject payload = Packet.extractDataPayload(response);
            String responseText = payload.getString("response");
            String thoughtText = payload.has("thought") ? payload.getString("thought") : "";
            JSONObject resultPayload = payload.has("resultPayload") ? payload.getJSONObject("resultPayload") : null;
            this.output = new MultimodalOutput(this.sn, this.unit.getCapability().getName(),
                    responseText, thoughtText, resultPayload);
            this.listener.onResponse(this.channel, this.output);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#process", e);
            this.listener.onFailed(this.channel, AIGCStateCode.IllegalOperation);
        } finally {
            this.channel.setProcessing(false);
        }
    }
}
