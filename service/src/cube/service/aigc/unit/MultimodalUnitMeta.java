/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Usage;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.listener.MultimodalListener;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.File;
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

    public AIGCChannel getChannel() {
        return this.channel;
    }

    @Override
    public void process() {
        try {
            Logger.d(this.getClass(), "#process - Model: " + this.unit.getCapability().getName());

            // 如果内容是超链接，先下载内容
            if (this.input.content.toLowerCase().startsWith("http://")
                    || this.input.content.toLowerCase().startsWith("https://")) {
                FileLabel fileLabel = this.service.downloadFile(this.channel.getAuthToken(), this.input.content);
                if (null != fileLabel) {
                    if (fileLabel.getFileType() == FileType.TEXT || fileLabel.getFileType() == FileType.MD) {
                        // 文本文件，读取内容进行替换
                        File file = this.service.loadFile(this.channel.getAuthToken().getDomain(), fileLabel.getFileCode());
                        if (null != file) {
                            String fileContent = FileUtils.readTextFile(file.getAbsolutePath());
                            // 删除临时文件
                            this.service.deleteFile(this.channel.getAuthToken().getDomain(), fileLabel.getFileCode());
                            if (null != fileContent) {
                                this.input.content = fileContent;
                            }
                            else {
                                this.listener.onFailed(this.channel, AIGCStateCode.FileError);
                                return;
                            }
                        }
                        else {
                            this.listener.onFailed(this.channel, AIGCStateCode.FileError);
                            return;
                        }
                    }
                    else {
                        // 其他文件，则直接传给 Unit
                        this.input.content = fileLabel.getFileCode();
                    }
                }
                else {
                    this.listener.onFailed(this.channel, AIGCStateCode.FileError);
                    return;
                }
            }

            // 处理文件
            List<FileLabel> fileLabelList = new ArrayList<>();
            for (String fileItem : this.input.fileCodes) {
                if (fileItem.startsWith("http")) {
                    // 下载文件
                    FileLabel fileLabel = this.service.downloadFile(this.channel.getAuthToken(), fileItem);
                    if (null != fileLabel) {
                        fileLabelList.add(fileLabel);
                    }
                }
                else if (fileItem.startsWith("rtsp") || fileItem.startsWith("rtmp")) {
                    // RTSP 或 RTMP 视频流
                    fileLabelList = null;
                    break;
                }
                else {
                    // 加载文件
                    FileLabel fileLabel = this.service.getFile(this.channel.getAuthToken().getDomain(), fileItem);
                    if (null != fileLabel) {
                        fileLabelList.add(fileLabel);
                    }
                }
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
                Logger.w(this.getClass(), "#process - unit failed: " + Packet.extractCode(response));
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.Failure);
                return;
            }

            JSONObject payload = Packet.extractDataPayload(response);
            String responseText = payload.getString("response");
            String thoughtText = payload.has("thought") ? payload.getString("thought") : "";
            JSONObject resultPayload = payload.has("resultPayload") ? payload.getJSONObject("resultPayload") : null;
            Usage usage = null;
            if (payload.has("performance")) {
                usage = new Usage(payload.getJSONObject("performance"));
            }
            else if (payload.has("usage")) {
                usage = new Usage(payload.getJSONObject("usage"));
            }
            this.output = new MultimodalOutput(this.sn, this.unit.getCapability().getName(),
                    responseText, thoughtText, resultPayload);
            // 关联用量信息
            this.output.usage = usage;
            this.listener.onResponse(this.channel, this.output);

            // 更新用量
            if (null != this.output.usage) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (output.resultPayload.has("streamId") && output.resultPayload.has("status")) {
                            String id = output.resultPayload.getString("streamId");
                            String status = output.resultPayload.getString("status");

                            if (status.equalsIgnoreCase("starting")) {
                                // 将 UnitMeta 记录到事件中心
                                EventCenter.getInstance().putUnitMeta(id, MultimodalUnitMeta.this);
                            } else if (status.equalsIgnoreCase("stopping")) {
                                // 将 UnitMeta 移除事件中心
                                EventCenter.getInstance().removeUnitMeta(id);
                            }
                        }

                        if (output.usage.inputTokens != 0 && output.usage.outputTokens != 0) {
                            Logger.d(MultimodalUnitMeta.class, "Update token usage: " +
                                    output.usage.inputTokens + "/" + output.usage.outputTokens);
                            service.getStorage().updateUsage(channel.getAuthToken().getContactId(),
                                    unit.getCapability().getName(),
                                    output.usage.outputTokens,
                                    output.usage.inputTokens);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#process", e);
            this.listener.onFailed(this.channel, AIGCStateCode.IllegalOperation);
        } finally {
            this.channel.setProcessing(false);
        }
    }
}
