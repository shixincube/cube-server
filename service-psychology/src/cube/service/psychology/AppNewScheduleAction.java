/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 新建日程动作。
 *
 * <p>对应线协议动作 {@code appNewSchedule}，逐字符等同于既有枚举
 * {@code AIGCAction.AppNewSchedule} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → Failure → Ok}。
 * 前两位由宿主代为产出。</p>
 *
 * <p><b>「复制构造器生成新 id」是本动作的关键语义</b>：
 * {@link ConsultationSchedule#ConsultationSchedule(ConsultationSchedule)}
 * 内部调用 {@code ConfigUtils.generateSerialNumber()} 覆盖 {@code src.id}，
 * 因此客户端提交的 {@code id} 被丢弃。若误用两参构造器
 * {@code new ConsultationSchedule(src.id, src.customerId)}，则会把客户端提交的 id
 * 直接落库，造成 id 冲突与越权覆盖。</p>
 *
 * <p><b>⚠️ 非法主题会以 InvalidParameter 失败</b>：{@code ConsultationTheme.parse}
 * 对无法识别的名称<b>返回 null</b>（而非像 {@code Gender.parse} 那样回退到
 * {@code Unknown}），随后落库或序列化时取 {@code .code} 会抛空指针。该空指针发生在
 * 下方 try 块内会被捕获，故最终以 {@code InvalidParameter} 应答——与既有行为一致。
 * 由于失败时回显的是原始请求而非校验说明，客户端应先枚举合法主题值。</p>
 *
 * <p><b>⚠️ 写入失败回显原始请求</b>（L75），{@code catch} 分支回空对象（L81）。</p>
 */
public final class AppNewScheduleAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            ConsultationSchedule submitted = new ConsultationSchedule(ctx.getRequest().data);

            // 复制构造器：新 id 由 ConfigUtils 生成，客户端提交的 id 被丢弃（L67）
            ConsultationSchedule newSchedule = new ConsultationSchedule(submitted);

            long cid = ctx.getToken().getContactId();
            if (((PsychologyModule) ctx.getModule()).writeSchedule(cid, newSchedule)) {
                ctx.respond(AIGCStateCode.Ok, newSchedule.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求（L75）
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // L81 回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
