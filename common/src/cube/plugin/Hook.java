/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.plugin;

/**
 * 事件钩子。
 */
public class Hook {

    /**
     * 触发钩子的关键字。
     */
    private String key;

    /**
     * 钩子的系统宿主。
     */
    protected PluginSystem system;

    /**
     * 构造函数。
     *
     * @param key
     */
    public Hook(String key) {
        this.key = key;
    }

    /**
     * 获取对应的触发关键字。
     *
     * @return 返回对应的触发关键字。
     */
    public String getKey() {
        return this.key;
    }

    /**
     * 触发插件回调。
     *
     * @param context
     */
    public HookResult apply(PluginContext context) {
        context.setKey(this.key);

        return this.system.apply(this.key, context);
    }
}
