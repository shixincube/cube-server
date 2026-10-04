/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.common.action.AIGCAction;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 业务模块兜底通道。
 *
 * <p>路径形如 {@code /aigc/module/{moduleName}/{actionName}}，例如
 * {@code POST /aigc/module/psychology/generatePsychologyReport}。</p>
 *
 * <p><b>定位</b>：供「低频 / 内部 / 尚未做专属 REST 端点」的动作快速接入。
 * <b>不替代</b>专属端点——专属端点承担参数校验、字段裁剪与状态码语义；
 * 本通道只做透传转发，把 {@code data} 原样交给动作处理器，
 * 应答即处理器给出的 {@code data}。</p>
 *
 * <p><b>为何路径不带动作前缀</b>：线协议字符串必须与既有值完全一致
 * （如 {@code generatePsychologyReport}，无模块前缀），故本通道按
 * {@code {moduleName}} 定位模块、在该模块的动作命名空间内匹配
 * {@code {actionName}}，而非拼接。</p>
 *
 * <p><b>启用开关</b>：由 {@code config/dispatcher.properties} 的
 * {@code module.rest.enabled} 控制，默认关闭。关闭时该路径不注册，
 * 访问返回 Jetty 的 404，不影响任何既有端点。</p>
 */
public class ModuleAction extends ContextHandler {

    /**
     * 通道根路径。
     */
    public final static String PATH = "/aigc/module";

    /**
     * 兜底通道配置开关的属性名。
     */
    public final static String CONFIG_ENABLED = "module.rest.enabled";

    public ModuleAction() {
        super(PATH + "/*");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            // 本通道需要「模块名 + 动作名」两段，故不能用基类的
            // getLastRequestPath（它只返回路径的最后一段，那是取 token 用的），
            // 直接取 pathInfo 再剥掉前导斜杠。
            String path = request.getPathInfo();
            if (null == path) {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            path = path.trim();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (!path.startsWith(ModuleAction.PATH.substring(1) + "/")) {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            String remainder = path.substring(ModuleAction.PATH.length());
            int index = remainder.indexOf('/');
            if (index <= 0 || index == remainder.length() - 1) {
                // 路径形如 /aigc/module/{module} 或 /aigc/module/{module}/
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            String moduleName = remainder.substring(0, index);
            String actionName = remainder.substring(index + 1);

            AIGCAction action = this.resolveAction(moduleName, actionName);
            if (null == action) {
                // 动作不属于该模块，或根本不存在——不泄露模块内部动作清单
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            JSONObject data;
            try {
                data = this.readBodyAsJSONObject(request);
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            try {
                JSONObject result = Manager.getInstance().syncRequest(token, action, data);
                if (null != result) {
                    this.respondOk(response, result);
                }
                else {
                    // 动作已受理但未给出数据（如异步动作仅回状态码）
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
            }

            this.complete();
        }

        /**
         * 把路径中的动作名解析为枚举，并校验其确属该模块。
         *
         * <p>动作字符串是线协议，<b>不能</b>拼上模块前缀，故按枚举
         * {@code name} 精确匹配后，再判定其是否落在给定模块的命名空间内
         * （见 {@link #belongsToModule}）。</p>
         *
         * @param moduleName 模块名。
         * @param actionName 动作名。
         * @return 返回匹配的枚举；不存在或不属该模块时返回 <code>null</code>。
         */
        private AIGCAction resolveAction(String moduleName, String actionName) {
            AIGCAction action = null;
            for (AIGCAction candidate : AIGCAction.values()) {
                if (candidate.name.equals(actionName)) {
                    action = candidate;
                    break;
                }
            }

            if (null == action) {
                Logger.d(ModuleAction.class, "#resolveAction - No such action: " + actionName);
                return null;
            }

            if (!this.belongsToModule(moduleName, action)) {
                Logger.d(ModuleAction.class, "#resolveAction - Action \"" + actionName
                        + "\" does NOT belong to module \"" + moduleName + "\"");
                return null;
            }

            return action;
        }

        /**
         * 判断动作是否属于该模块。
         *
         * <p>归属规则：模块名前缀匹配（{@code psychology*}），
         * 或动作名出现在该模块已绑定的动作表中。后者由宿主
         * {@code ActionRouter} 持有，但 dispatcher 与 service 分属不同进程，
         * 无法直接查询，故此处以前缀规则为准，足够覆盖现有动作命名。</p>
         *
         * @param moduleName 模块名。
         * @param action 动作。
         * @return 属于该模块时返回 <code>true</code>。
         */
        private boolean belongsToModule(String moduleName, AIGCAction action) {
            if (null == moduleName || moduleName.isEmpty()) {
                return false;
            }

            // 动作名以模块名开头，或模块名以动作的语义前缀开头。
            // 心理学动作均为 "psychology*" / "painting*" / "counseling*" 形态，
            // 故按 "动作名包含模块名" 与 "模块名包含动作前缀" 两向匹配。
            String name = action.name;
            if (name.startsWith(moduleName)) {
                return true;
            }

            // painting / counseling 系列归心理学模块（历史命名如此）
            if (moduleName.startsWith("psych")) {
                return name.startsWith("psychology")
                        || name.startsWith("painting")
                        || name.startsWith("counseling")
                        || name.startsWith("scale")
                        || name.startsWith("app");
            }

            return false;
        }
    }

    /**
     * 读取兜底通道开关。
     *
     * @return 启用时返回 <code>true</code>；配置缺失或读取失败时返回
     *         <code>false</code>（默认关闭，保证升级后行为与改造前完全一致）。
     */
    public static boolean isEnabled() {
        java.io.File file = new java.io.File("config/dispatcher.properties");
        if (!file.exists()) {
            file = new java.io.File("dispatcher.properties");
        }

        if (!file.exists()) {
            return false;
        }

        try {
            java.util.Properties properties = cube.util.ConfigUtils
                    .readProperties(file.getAbsolutePath());
            return Boolean.parseBoolean(properties.getProperty(CONFIG_ENABLED, "false").trim());
        } catch (Exception e) {
            Logger.w(ModuleAction.class, "#isEnabled - Read config failed", e);
            return false;
        }
    }
}
