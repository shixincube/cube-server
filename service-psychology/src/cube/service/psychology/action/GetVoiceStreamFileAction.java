/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.VoiceStreamService;
import org.json.JSONObject;

/**
 * 读取语音流录音文件动作。
 *
 * <p>对应线协议动作 {@code getVoiceStreamFile}，逐字符等同于既有枚举
 * {@code AIGCAction.GetVoiceStreamFile} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：{@code InvalidParameter → Ok / Failure}。</p>
 *
 * <p>本动作与前三个语音动作同批迁入：它读的是咨询录音归档表，
 * 该表已随咨询业务迁入本模块。</p>
 */
public final class GetVoiceStreamFileAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode || !ctx.getRequest().data.has("streamName")) {
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
            return AIGCStateCode.InvalidParameter;
        }

        String streamName = ctx.getRequest().data.getString("streamName");
        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);

        JSONObject recording = VoiceStreamService.getInstance().readRecording(streamName);
        if (null == recording) {
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
            return AIGCStateCode.Failure;
        }

        FileLabel fileLabel = ctx.getHost().getFile(
                authToken.getDomain(), recording.getString("fileCode"));
        if (null == fileLabel) {
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
            return AIGCStateCode.Failure;
        }

        ctx.respond(AIGCStateCode.Ok, fileLabel.toCompactJSON());
        return AIGCStateCode.Ok;
    }
}
