/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.util.log.Logger;
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.PsychologyModule;

/**
 * 更新日程动作。
 *
 * <p>对应线协议动作 {@code appUpdateSchedule}，逐字符等同于既有枚举
 * {@code AIGCAction.AppUpdateSchedule} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → NoData → Failure → Ok}。
 * 前两位由宿主代为产出。</p>
 *
 * <p><b>与 {@link AppUpdateCustomerAction} 的关键差异：不做状态对齐</b>。
 * 直接以提交值 {@code writeSchedule(cid, submitted)}，
 * {@code state} 完全由客户端决定。因此本动作<b>允许</b>客户端把日程状态改为
 * {@code Deleted}；这与客户侧的软删除保护是<b>既有的有意不对称</b>，
 * 不要「顺手补上对齐」。</p>
 *
 * <p><b>⚠️ 非法主题会以 InvalidParameter 失败</b>：同
 * {@link AppNewScheduleAction}，{@code ConsultationTheme.parse} 对无法识别的名称
 * 返回 null，后续取 {@code .code} 抛空指针被 try 捕获。</p>
 *
 * <p><b>⚠️ 两个失败分支回显的都是原始请求</b>：{@code NoData} 与
 * {@code Failure} 两处均以 {@code ctx.getRequest().data} 作为应答载荷；
 * 仅 {@code catch} 分支回空对象。</p>
 */
public final class AppUpdateScheduleAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            ConsultationSchedule submitted = new ConsultationSchedule(ctx.getRequest().data);
            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            // 存在性校验；current 仅用于判空与（客户侧的状态对齐），其字段一律不参与回写
            ConsultationSchedule current = module.readSchedule(cid, submitted.id);
            if (null == current) {
                // ⚠️ 回显原始请求
                ctx.respond(AIGCStateCode.NoData, ctx.getRequest().data);

                return AIGCStateCode.NoData;
            }

            // 不做 state 对齐：日程侧 state 由客户端决定
            if (module.writeSchedule(cid, submitted)) {
                ctx.respond(AIGCStateCode.Ok, submitted.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // 回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
