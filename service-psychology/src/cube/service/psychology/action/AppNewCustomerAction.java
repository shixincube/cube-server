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
 * 新增客户动作。
 *
 * <p>对应线协议动作 {@code appNewCustomer}，逐字符等同于既有枚举
 * {@code AIGCAction.AppNewCustomer} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → IllegalOperation → Failure → Ok}，
 * 末位还有一个由 {@code catch} 兜底的 {@code InvalidParameter}。前两位由宿主
 * {@code ActionRunner} 代为产出。</p>
 *
 * <p><b>写入语义</b>：先以请求体解析出提交数据，再用<b>不含 id</b> 的六参构造器
 * 生成一个新 id 的实例落库——即客户端不能自选 id。</p>
 *
 * <p><b>⚠️ 写入失败时回显的是原始请求而非空对象</b>。这一点极易写错：
 * 若改为 {@code respondEmpty(Failure)}，客户端会丢失自己提交的数据而无法重试。</p>
 */
public final class AppNewCustomerAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);

            return AIGCStateCode.IllegalOperation;
        }

        try {
            // 刻意不判空：data 为 null 时构造器抛 NPE，与既有行为一致，最终回 InvalidParameter
            Customer submitted = new Customer(ctx.getRequest().data);

            // 六参构造器内部生成新 id：客户端提交的 id 被丢弃
            Customer newCustomer = new Customer(submitted.name, submitted.gender, submitted.age,
                    submitted.mobile, submitted.comment, submitted.timestamp);

            long cid = ctx.getToken().getContactId();
            if (((PsychologyModule) ctx.getModule()).writeCustomer(cid, newCustomer)) {
                ctx.respond(AIGCStateCode.Ok, newCustomer.toJSON());

                return AIGCStateCode.Ok;
            }

            // ⚠️ 回显原始请求，不是空对象
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);

            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // 回空对象，与上面 write 失败分支不同
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
