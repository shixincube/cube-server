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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 查询日程列表动作。
 *
 * <p>对应线协议动作 {@code appQuerySchedule}，逐字符等同于既有枚举
 * {@code AIGCAction.AppQuerySchedule} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code InvalidParameter → InconsistentToken → Ok}。前两位由宿主代为产出。</p>
 *
 * <p><b>⚠️ 未注册分支的字段名是 {@code starting}/{@code ending}，不是
 * {@code page}/{@code size}</b>。
 * 本动作与 {@link AppQueryCustomerAction} 在这一点上<b>刻意不同</b>，因为日程查询是
 * 「时间窗筛选」而非「分页」，客户端据此回显查询区间。不要照抄客户侧的字段名。</p>
 *
 * <p><b>时间窗默认值</b>：缺 {@code starting} 时取「当前时间 − 365 天」，
 * 缺 {@code ending} 时取「当前时间 + 365 天」。默认值依赖
 * {@code System.currentTimeMillis()}，因此同一请求在不同毫秒派发可能落在不同窗口
 * ——这是既有行为。</p>
 */
public final class AppQueryScheduleAction implements AIGCActionTask {

    /**
     * 时间窗默认跨度（毫秒），约一年。
     */
    private final static long DEFAULT_SPAN = 365 * 24 * 60 * 60 * 1000L;

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        if (!ctx.getHost().isRegistered(ctx.getToken().getCode())) {
            // 未注册用户无数据：注意字段为 starting/ending，不是 page/size
            JSONObject responseData = new JSONObject();
            responseData.put("list", new JSONArray());
            responseData.put("total", 0);
            responseData.put("starting", 0);
            responseData.put("ending", 0);
            ctx.respond(AIGCStateCode.Ok, responseData);

            return AIGCStateCode.Ok;
        }

        try {
            // 刻意不判空：request 为 null 时 NPE，最终回 InvalidParameter
            JSONObject request = ctx.getRequest().data;

            long starting = request.has("starting")
                    ? request.getLong("starting")
                    : System.currentTimeMillis() - DEFAULT_SPAN;
            long ending = request.has("ending")
                    ? request.getLong("ending")
                    : System.currentTimeMillis() + DEFAULT_SPAN;

            long cid = ctx.getToken().getContactId();
            PsychologyModule module = (PsychologyModule) ctx.getModule();

            int total = module.countSchedules(cid, starting, ending);
            List<ConsultationSchedule> list = module.readSchedules(cid, starting, ending);

            JSONArray array = new JSONArray();
            for (ConsultationSchedule schedule : list) {
                array.put(schedule.toJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("list", array);
            responseData.put("total", total);
            responseData.put("starting", starting);
            responseData.put("ending", ending);
            ctx.respond(AIGCStateCode.Ok, responseData);

            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);

            // ⚠️ 回显原始请求（可能为 null）
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);

            return AIGCStateCode.InvalidParameter;
        }
    }
}
