/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.app.Customer;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 更新客户动作。
 *
 * <p>对应线协议动作 {@code appUpdateCustomer}，逐字符等同于既有枚举
 * {@code AIGCAction.AppUpdateCustomer} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → NoData → Failure → Ok}。
 * 前两位由宿主代为产出。</p>
 *
 * <p><b>状态对齐是本动作的关键语义</b>：{@code state} 字段<b>不由客户端决定</b>——
 * 迁移前 L76 用库中当前值的 {@code state} 覆盖提交值，因此客户端无法借此把已删除
 * 的记录「复活」。本实现必须保留这一步，它是软删除语义的一部分。</p>
 *
 * <p><b>⚠️ 两个失败分支回显的都是原始请求</b>：{@code NoData}（迁移前 L70）与
 * {@code Failure}（迁移前 L85）；仅 {@code catch} 分支回空对象（L91）。</p>
 *
 * <p><b>⚠️ 与 {@link AppUpdateScheduleAction} 的有意不对称</b>：日程侧不做状态对齐。
 * 本动作必须对齐，不得为了「统一风格」而改动。</p>
 */
public final class AppUpdateCustomerAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            Customer submitted = new Customer(ctx.getRequest().data);
            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            Customer current = module.readCustomer(cid, submitted.id);
            if (null == current) {
                // ⚠️ 回显原始请求（迁移前 L70）
                ctx.respond(AIGCStateCode.NoData, ctx.getRequest().data);

                return AIGCStateCode.NoData;
            }

            // 状态对齐：state 取库中当前值，客户端提交值被丢弃（迁移前 L76）
            submitted.state = current.state;

            if (module.writeCustomer(cid, submitted)) {
                ctx.respond(AIGCStateCode.Ok, submitted.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求（迁移前 L85）
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // 迁移前 L91 回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
