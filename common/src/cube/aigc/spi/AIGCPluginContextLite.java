/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cube.auth.AuthToken;
import cube.common.entity.AIGCUnit;
import cube.plugin.PluginContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 钩子上下文（SPI 侧）。
 *
 * <p>宿主侧的钩子上下文携带业务字段并负责构造任务描述符，但其类型位于
 * service 模块，插件无法引用。若不提供本类，{@link AIGCHost#fireHook} 只能
 * 接受一个「无法携带任何信息」的空上下文，插件调用它等同无效——这是
 * {@code fireHook} 在仅有接口时不可用的根因。</p>
 *
 * <p>本类是插件唯一可构造的钩子上下文：字段类型全部取自 common，
 * 插件不引入任何 service 依赖。宿主实现收到本对象后，会将其翻译成
 * service 侧上下文再交给钩子消费者，因此<b>插件不得缓存宿主翻译后的结果</b>
 * （翻译结果的生命周期由宿主管理）。</p>
 *
 * <p>用法：构造时填入令牌与任务名，生成结束后补入 token 计数与单元，
 * 再调用 {@link AIGCHost#fireHook}。</p>
 */
public class AIGCPluginContextLite extends PluginContext {

    /**
     * 访问令牌。
     */
    private AuthToken authToken;

    /**
     * 任务名。
     */
    private String task;

    /**
     * 执行单元。
     */
    private AIGCUnit unit;

    /**
     * 输入 token 数。
     */
    private int inputTokens;

    /**
     * 输出 token 数。
     */
    private int outputTokens;

    /**
     * 关联文件码列表。
     */
    private final List<String> fileCodeList = new ArrayList<>();

    /**
     * 构造函数。
     *
     * @param authToken 访问令牌，可为 <code>null</code>。
     * @param task 任务名，可为 <code>null</code>。
     */
    public AIGCPluginContextLite(AuthToken authToken, String task) {
        this.authToken = authToken;
        this.task = task;
    }

    /**
     * 构造函数。
     *
     * @param task 任务名，可为 <code>null</code>。
     */
    public AIGCPluginContextLite(String task) {
        this(null, task);
    }

    /**
     * 获取访问令牌。
     *
     * @return 返回访问令牌，可为 <code>null</code>。
     */
    public AuthToken getAuthToken() {
        return this.authToken;
    }

    /**
     * 获取任务名。
     *
     * @return 返回任务名，可为 <code>null</code>。
     */
    public String getTask() {
        return this.task;
    }

    /**
     * 设置任务名。
     *
     * @param task 任务名。
     */
    public void setTask(String task) {
        this.task = task;
    }

    /**
     * 获取执行单元。
     *
     * @return 返回执行单元，可为 <code>null</code>。
     */
    public AIGCUnit getUnit() {
        return this.unit;
    }

    /**
     * 设置执行单元。
     *
     * @param unit 执行单元。
     */
    public void setUnit(AIGCUnit unit) {
        this.unit = unit;
    }

    /**
     * 设置输入 token 数。
     *
     * @param tokens token 数。
     */
    public void setInputTokens(int tokens) {
        this.inputTokens = tokens;
    }

    /**
     * 获取输入 token 数。
     *
     * @return 返回 token 数。
     */
    public int getInputTokens() {
        return this.inputTokens;
    }

    /**
     * 设置输出 token 数。
     *
     * @param tokens token 数。
     */
    public void setOutputTokens(int tokens) {
        this.outputTokens = tokens;
    }

    /**
     * 获取输出 token 数。
     *
     * @return 返回 token 数。
     */
    public int getOutputTokens() {
        return this.outputTokens;
    }

    /**
     * 追加关联文件码。
     *
     * @param fileCode 文件码。
     */
    public void addFileCode(String fileCode) {
        if (null == fileCode) {
            return;
        }

        this.fileCodeList.add(fileCode);
    }

    /**
     * 获取关联文件码列表（只读视图）。
     *
     * @return 返回文件码列表。
     */
    public List<String> getFileCodeList() {
        return java.util.Collections.unmodifiableList(this.fileCodeList);
    }

    @Override
    public Object get(String name) {
        if ("authToken".equalsIgnoreCase(name)) {
            return this.authToken;
        }
        else if ("task".equalsIgnoreCase(name)) {
            return this.task;
        }
        else if ("unit".equalsIgnoreCase(name)) {
            return this.unit;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
        if ("task".equalsIgnoreCase(name) && null != value) {
            this.task = value.toString();
        }
        else if ("unit".equalsIgnoreCase(name) && value instanceof AIGCUnit) {
            this.unit = (AIGCUnit) value;
        }
    }
}