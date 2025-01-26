/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.plugin;

/**
 * 插件接口。
 */
public interface Plugin {

    /**
     * 在插件注册之前的操作。
     */
    void setup();

    /**
     * 插件需要被销毁时调用。
     */
    void teardown();

    /**
     * 当对应关键字的钩子被触发时回调该函数。
     *
     * @param context
     */
    HookResult launch(PluginContext context);

}
