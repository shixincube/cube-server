/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.util.log.Logger;
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.psychology.consultation.ConsultationScheduleState;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.PsychologyModule;

/**
 * 删除日程动作。
 *
 * <p>对应线协议动作 {@code appDeleteSchedule}，逐字符等同于既有枚举
 * {@code AIGCAction.AppDeleteSchedule} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → NoData → Failure → Ok}。
 * 前两位由宿主代为产出。</p>
 *
 * <p><b>「删除」是软删除</b>：把 {@code state} 置为
 * {@link ConsultationScheduleState#Deleted}（code=9）后整行回写；查询侧以
 * {@code state <> Deleted.code} 过滤。</p>
 *
 * <p><b>⚠️ 读-改-写非原子</b>，与客户侧同缺陷，既有。</p>
 *
 * <p><b>⚠️ 两个失败分支回显的都是原始请求</b>：{@code NoData} 与
 * {@code Failure} 两处均以 {@code ctx.getRequest().data} 作为应答载荷；
 * 仅 {@code catch} 分支回空对象。
 * 注意本文件的 catch 日志串是 {@code "#run"}，与其余 7 个文件的空串
 * 不同——不影响应答，但保留以贴合原实现。</p>
 */
public final class AppDeleteScheduleAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            // 刻意不判空：data 为 null 时抛 NPE，由 catch 分支收口
            long id = ctx.getRequest().data.getLong("id");
            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            ConsultationSchedule schedule = module.readSchedule(cid, id);
            if (null == schedule) {
                // ⚠️ 回显原始请求
                ctx.respond(AIGCStateCode.NoData, ctx.getRequest().data);

                return AIGCStateCode.NoData;
            }

            // 软删除：置状态位而非物理删除
            schedule.state = ConsultationScheduleState.Deleted;

            if (module.writeSchedule(cid, schedule)) {
                ctx.respond(AIGCStateCode.Ok, schedule.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);

            // 回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
