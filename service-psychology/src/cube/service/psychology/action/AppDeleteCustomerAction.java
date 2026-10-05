/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.util.log.Logger;
import cube.aigc.psychology.app.Customer;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.PsychologyModule;

/**
 * 删除客户动作。
 *
 * <p>对应线协议动作 {@code appDeleteCustomer}，逐字符等同于既有枚举
 * {@code AIGCAction.AppDeleteCustomer} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → NoData → Failure → Ok}。
 * 前两位由宿主代为产出。</p>
 *
 * <p><b>「删除」是软删除</b>：不执行 SQL {@code DELETE}，而是把 {@code state} 置为
 * {@link Customer#STATE_DELETE} 后整行回写；查询侧以 {@code state = STATE_NORMAL}
 * 过滤，因此删除后不再出现。</p>
 *
 * <p><b>⚠️ 读-改-写非原子</b>：{@code readCustomer} 与 {@code writeCustomer} 之间
 * 无锁无事务，两个并发删除可能丢失其中一次的写入。这是<b>既有</b>缺陷
 * （{@code AppDeleteCustomerTask:65-76}），需由存储层修复。</p>
 *
 * <p><b>⚠️ 两个失败分支回显的都是原始请求</b>：{@code NoData}（L68）与
 * {@code Failure}（L83）；仅 {@code catch} 分支回空对象（L89）。</p>
 */
public final class AppDeleteCustomerAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            // 刻意不判空：data 为 null 时 NPE，与L64 一致
            long id = ctx.getRequest().data.getLong("id");
            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            Customer customer = module.readCustomer(cid, id);
            if (null == customer) {
                // ⚠️ 回显原始请求（L68）
                ctx.respond(AIGCStateCode.NoData, ctx.getRequest().data);

                return AIGCStateCode.NoData;
            }

            // 软删除：置状态位而非物理删除（L74）
            customer.state = Customer.STATE_DELETE;

            if (module.writeCustomer(cid, customer)) {
                ctx.respond(AIGCStateCode.Ok, customer.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求（L83）
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // L89 回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
