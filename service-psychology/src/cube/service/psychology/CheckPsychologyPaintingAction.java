/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.entity.AIGCUnit;
import cube.common.entity.ObjectInfo;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 校验绘画动作。
 *
 * <p>对应线协议动作 {@code checkPsychologyPainting}，逐字符等同于既有枚举
 * {@code AIGCAction.CheckPsychologyPainting} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → NoToken → InvalidParameter → InvalidParameter → Ok}。</p>
 *
 * <p><b>⚠️ 本动作有两处全批唯一之处，绝不可「顺手统一」</b>：</p>
 * <ol>
 *   <li><b>第二码也是 {@code NoToken}</b>（迁移前 L49：{@code getToken()} 返回
 *       {@code null} 时回 {@code NoToken} 而非 {@code IllegalOperation}）。
 *       骨架在 {@code requiresToken=true} 时会回 {@code InconsistentToken}，
 *       两个码都不同于迁移前，因此必须 {@code false} 并自行复刻；</li>
 *   <li><b>永不回 {@code Failure}</b>——任何无法确认的情形都回
 *       {@code Ok} 且 {@code result=false}（迁移前 L74-77）。</li>
 * </ol>
 *
 * <p><b>⚠️ 本动作是「同名双向」动作</b>：除入站派发外，宿主
 * {@code PsychologyScene} 还用<b>同一动作名</b>把它转发给远端绘画单元
 * 做像素级校验。迁移后该出站调用必须依然发生，否则
 * {@code result} 会退化为「只看有无其他物体」，失去像素校验。</p>
 *
 * <p>判定逻辑逐字复刻迁移前 {@code PsychologyScene#checkPsychologyPainting}：
 * 先检测图像中的物体数量，若远端像素校验通过<b>或</b>画面中没有其他物体，
 * 即认为是绘画。</p>
 */
public final class CheckPsychologyPaintingAction implements AIGCActionTask {

    /**
     * 画面中被判定为「非绘画元素」的物体数量阈值。
     *
     * <p>逐字复刻迁移前 {@code PsychologyScene#hasMoreObjects}：该数量大于此值时
     * 视为画面中还有其他物体。</p>
     */
    private final static int MAX_EXTRA_OBJECTS = 1;

    /**
     * 绘画识别单元的能力名。
     *
     * <p>与宿主 {@code PsychologyScene.UNIT} 逐字相同。该常量原在宿主类上，
     * 插件不能引用宿主类型，故在此自持一份——<b>值必须保持一致</b>，
     * 否则会选不到单元导致校验静默失效。</p>
     */
    private final static String PSYCHOLOGY_UNIT = "Psychology";

    /**
     * 出站调用远端绘画单元时使用的动作名。
     *
     * <p>与本动作同名，但方向相反：本动作作为<b>入站</b>被客户端调用，
     * 而它又以同名<b>出站</b>转发给远端单元做像素校验。</p>
     */
    private final static String ACTION_CHECK_PAINTING = "checkPsychologyPainting";

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);

        // 第二码仍是 NoToken：迁移前此处回的不是 IllegalOperation，勿改
        if (null == authToken) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        JSONObject data = ctx.getRequest().data;
        if (!data.has("fileCode")) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        String fileCode;
        try {
            fileCode = data.getString("fileCode");
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        // 无论检测与否都回 Ok，判定结果放在 result 里（迁移前 L72-77）
        JSONObject responseData = new JSONObject();
        responseData.put("result", this.check(ctx, authToken, fileCode));
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }

    /**
     * 判定给定图像是否为绘画。
     *
     * <p>逐字复刻迁移前 {@code PsychologyScene#checkPsychologyPainting}：
     * 先取文件标签（取不到直接判非绘画），再检测画面中除绘画外的其他元素，
     * 最后由远端绘画单元做像素级校验，两者满足其一即认为是绘画。</p>
     *
     * @param ctx 动作上下文。
     * @param token 访问令牌。
     * @param fileCode 文件码。
     * @return 是绘画返回 {@code true}。
     */
    private boolean check(ActionContext ctx, AuthToken token, String fileCode) {
        AIGCHost host = ctx.getHost();

        // ① 取文件标签：取不到即判非绘画（迁移前 L211-215）
        cube.common.entity.FileLabel fileLabel = host.getFile(token.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#check - File error: " + fileCode);
            return false;
        }

        // ② 物体检测：画面中还有其他元素时判为非绘画（迁移前 L217-222）
        ObjectInfo info = host.detectObject(token.getDomain(), fileCode, false);
        boolean more = this.hasMoreObjects(info);

        // ③ 像素校验：转发给远端绘画单元（迁移前 L224-249）
        boolean predicted = this.predictByUnit(host, fileLabel);

        // 逐字复刻迁移前 L253：像素校验通过「或」无其他元素
        return predicted || !more;
    }

    /**
     * 转发给远端绘画单元做像素级校验。
     *
     * <p>⚠️ 这里用的动作名与本动作<b>同名</b>，但它是<b>出站</b>调用
     * （发往远端绘画单元），与入站派发是两回事，不可混为一谈。
     * 迁移后此调用依然必须发生，否则判定会退化为「只看有无其他物体」，
     * 失去像素级校验。</p>
     *
     * @param host 宿主能力。
     * @param fileLabel 文件标签。
     * @return 远端判定是绘画返回 {@code true}。
     */
    private boolean predictByUnit(AIGCHost host, cube.common.entity.FileLabel fileLabel) {
        AIGCUnit unit = host.selectUnit(PSYCHOLOGY_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#predictByUnit - No psychology unit");
            return false;
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());

        ActionDialect dialect = host.invokeUnit(unit, ACTION_CHECK_PAINTING, data, 60 * 1000L);
        if (null == dialect) {
            Logger.w(this.getClass(), "#predictByUnit - Predict image unit error");
            return false;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#predictByUnit - Predict image response state: "
                    + Packet.extractCode(response));
            return false;
        }

        try {
            return Packet.extractDataPayload(response).getBoolean("result");
        } catch (Exception e) {
            Logger.e(this.getClass(), "#predictByUnit", e);
            return false;
        }
    }

    /**
     * 判断画面中是否存在绘画之外的其他物体。
     *
     * @param info 物体检测结果。
     * @return 存在其他物体返回 {@code true}。
     */
    private boolean hasMoreObjects(ObjectInfo info) {
        if (null == info) {
            return false;
        }

        int count = 0;
        for (cube.common.entity.Material object : info.getObjects()) {
            ++count;
            if (count > MAX_EXTRA_OBJECTS) {
                return true;
            }
        }

        return false;
    }
}
