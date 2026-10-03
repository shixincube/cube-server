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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询客户列表动作。
 *
 * <p>对应线协议动作 {@code appQueryCustomer}，逐字符等同于既有枚举
 * {@code AIGCAction.AppQueryCustomer} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code InvalidParameter → InconsistentToken → Ok}。前两个码由宿主
 * {@code ActionRunner} 在 {@code requiresToken=true} 下代为产出，
 * 本处理器不重复实现令牌校验。</p>
 *
 * <p><b>为何未注册用户回 Ok 而非 IllegalOperation</b>：这是本动作与同批其余
 * 7 个的唯一语义分歧，且为<b>迁移前既有</b>——未注册用户没有客户数据，
 * 「查不到」不是「操作非法」，故回一个空列表让客户端无需分支处理。
 * 此处<b>不得</b>「顺手修正」为 {@code IllegalOperation}。</p>
 *
 * <p><b>为何读 {@code ctx.getRequest().data} 而非 {@code ctx.getParams()}</b>：
 * 后者在请求未带 {@code data} 时返回空对象，会让 {@code has("page")} 变成
 * {@code false} 而静默进入正常分支回 {@code Ok}；迁移前是
 * {@code packet.data} 为 {@code null} 触发 NPE 后回 {@code InvalidParameter}。
 * 两者是<b>语义反转（失败变成功）</b>，必须保留 {@code null}。</p>
 */
public final class AppQueryCustomerAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            // 未注册用户无数据：回空列表（迁移前 AppQueryCustomerTask:62-67）
            JSONObject responseData = new JSONObject();
            responseData.put("list", new JSONArray());
            responseData.put("total", 0);
            responseData.put("page", 0);
            responseData.put("size", 0);
            ctx.respond(AIGCStateCode.Ok, responseData);

            return AIGCStateCode.Ok;
        }

        try {
            // 刻意不判空：request 为 null 时此处 NPE，与迁移前 packet.data 为 null 的行为一致
            JSONObject request = ctx.getRequest().data;

            int page = request.has("page") ? request.getInt("page") : 0;
            int size = request.has("size") ? request.getInt("size") : 0;

            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            int total = module.countCustomers(cid);
            List<Customer> list = new ArrayList<>();
            // 分页参数为 0/0 时才返回完整列表，与迁移前 L80-82 一致
            if (page == 0 && size == 0) {
                list = module.readCustomers(cid);
            }

            JSONArray array = new JSONArray();
            for (Customer customer : list) {
                array.put(customer.toJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("list", array);
            responseData.put("total", total);
            responseData.put("page", page);
            responseData.put("size", size);
            ctx.respond(AIGCStateCode.Ok, responseData);

            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // 回显原始请求（可能为 null），与迁移前 L100 一致
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
