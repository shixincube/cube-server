/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.plugin;

/**
 * 插件进行数据传递的上下文。
 */
public abstract class PluginContext {

    private String key;

    protected Object parameter;

    public PluginContext() {
    }

    protected void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取当前上下文的 Hook 键。
     *
     * @return 返回当前上下文的键。
     */
    public String getKey() {
        return this.key;
    }

    public boolean hasParameter() {
        return (null != this.parameter);
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }

    public Object getParameter() {
        return this.parameter;
    }

    public abstract Object get(String name);

    public abstract void set(String name, Object value);
}
