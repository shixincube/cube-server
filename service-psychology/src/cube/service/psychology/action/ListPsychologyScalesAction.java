/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取量表清单动作。
 *
 * <p>对应线协议动作 {@code listPsychologyScales}，逐字符等同于既有枚举
 * {@code AIGCAction.ListPsychologyScales} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → Ok}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：令牌无效时回的是
 * {@code IllegalOperation}，而宿主动作骨架在 {@code requiresToken=true} 时
 * 会改回 {@code InconsistentToken}——这是线协议可见的语义变更。
 * 故保留 {@code false}，令牌有效性由本处理器自行判定。</p>
 *
 * <p><b>资源依赖</b>：{@link Resource#getInstance()} 是全局单例且位于
 * {@code common} 模块，插件与宿主共用同一份量表定义缓存。这是期望行为：
 * 若插件自建一份，会出现两份内存态，宿主写入的量表对插件不可见。</p>
 */
public final class ListPsychologyScalesAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        long contactId = ((PsychologyModule) ctx.getModule()).resolveContactId(ctx.getHost(), tokenCode);
        if (contactId <= 0) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        List<Scale> list = ((PsychologyModule) ctx.getModule()).listScales(contactId);

        JSONArray array = new JSONArray();
        for (Scale scale : list) {
            array.put(scale.toCompactJSON());
        }

        JSONObject responseData = new JSONObject();
        responseData.put("list", array);
        responseData.put("total", list.size());
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }

    /**
     * 列出全部已开放的量表。
     *
     * <p>该过滤逻辑位于 {@code PsychologyScene#listScales}，
     * 此处逐字复刻：未开放的量表不进入结果集。</p>
     *
     * @param contactId 联系人 ID。
     * @return 返回已开放的量表列表。
     */
    static List<Scale> listScales(long contactId) {
        List<Scale> result = new java.util.ArrayList<>();

        for (Scale scale : Resource.getInstance().listScales(contactId)) {
            if (!scale.open) {
                continue;
            }

            result.add(scale);
        }

        return result;
    }
}
