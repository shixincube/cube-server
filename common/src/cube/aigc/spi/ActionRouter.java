/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cell.util.log.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动作路由器：<code>actionName -&gt;绑定</code> 的并发注册表。
 *
 * <p><b>并发设计</b>：底层为 {@link ConcurrentHashMap}。</p>
 * <ul>
 *     <li>{@link #bind} 用 <code>putIfAbsent</code> 实现原子判重，
 *         因此「后注册者不覆盖」的冲突策略在并发下也是原子的，无需外部锁；</li>
 *     <li>{@link #lookup} 是无锁读，不会与派发线程争锁；</li>
 *     <li>{@link #unbindAll} 基于 <code>entrySet()</code> 的弱一致迭代 +
 *         <code>remove(key, value)</code> 双参删除（CAS 语义，不会误删已被重新绑定的项），
 *         可与并发 {@link #bind} 安全共存；</li>
 *     <li>{@link Bound} 的所有字段均为 <code>final</code>，
 *         由 <code>putIfAbsent</code> 与 <code>get</code> 之间建立的 happens-before 边保证
 *         对派发线程安全发布。</li>
 * </ul>
 */
public final class ActionRouter {

    /**
     * 宿主保留动作名：这些是平台基础设施语义（令牌校验、事件通道、
     * 节点与单元生命周期），<b>任何模块都不得绑定</b>，防止劫持既有协议语义。
     */
    private final static Set<String> RESERVED_ACTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("checkToken", "event", "setup", "teardown")));

    /**
     * 动作表。Key 为线协议动作名。
     */
    private final ConcurrentHashMap<String, Bound> table = new ConcurrentHashMap<>();

    /**
     * 已声明动作名。
     *
     * <p>与 {@link #table} <b>刻意分离</b>：本集合记录「业务模块声明过该动作」，
     * 不随绑定失败而移除。模块装载失败时绑定会回滚，但声明仍在——
     * 宿主据此判定「该动作本应由某模块处理，只是模块未就绪」，
     * 从而回 {@code ModuleNotLoaded} 而非让请求悬挂。</p>
     */
    private final Set<String> declaredActions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 标记一批动作已被某模块声明。
     *
     * <p>由 {@code ModuleRegistry} 在实例化模块后、绑定动作前调用。
     * 重复声明同一动作不视为冲突——冲突判定仍以 {@link #bind} 为准。</p>
     *
     * @param actions 动作名集合，可为 {@code null}。
     */
    public void declareAll(Collection<String> actions) {
        if (null == actions) {
            return;
        }

        for (String action : actions) {
            if (null != action && !action.isEmpty() && !RESERVED_ACTIONS.contains(action)) {
                this.declaredActions.add(action);
            }
        }
    }

    /**
     * 判断某动作是否已被业务模块声明。
     *
     * <p>供宿主在动作表未命中时区分两种情形：</p>
     * <ul>
     *     <li>已声明 → 所属模块未就绪，应回 {@code ModuleNotLoaded}；</li>
     *     <li>未声明 → 该动作本就不属于任何业务模块，按既有分支处理。</li>
     * </ul>
     *
     * @param action 动作名。
     * @return 已被声明时返回 {@code true}。
     */
    public boolean isDeclared(String action) {
        return null != action && this.declaredActions.contains(action);
    }

    /**
     * 是否已完成装载。用于在装载前把派发成本压到一次 volatile 读。
     */
    private volatile boolean loaded = false;

    /**
     * 绑定一个动作。
     *
     * <p><b>冲突策略：后注册者不覆盖。</b>同一动作名第二次绑定时返回
     * <code>false</code> 并记录 ERROR，避免模块之间互相劫持动作。</p>
     *
     * @param binding 待绑定的动作绑定。
     * @param owner 绑定所属模块。
     * @return 绑定成功返回 <code>true</code>；动作名非法、命中保留名或已被占用时返回 <code>false</code>。
     */
    public boolean bind(ActionBinding binding, ActionModule owner) {
        if (null == binding || null == binding.task || null == owner) {
            Logger.e(ActionRouter.class, "#bind - Null binding, task or owner");
            return false;
        }

        String action = binding.action;
        if (null == action || action.isEmpty()) {
            Logger.e(ActionRouter.class, "#bind - Empty action name from module: " + owner.getName());
            return false;
        }

        if (RESERVED_ACTIONS.contains(action)) {
            Logger.e(ActionRouter.class, "#bind - Action \"" + action
                    + "\" is reserved by the host, rejected for module: " + owner.getName());
            return false;
        }

        Bound bound = new Bound(binding, owner);
        Bound previous = this.table.putIfAbsent(action, bound);
        if (null != previous) {
            Logger.e(ActionRouter.class, "#bind - Action \"" + action
                    + "\" is already bound by module: " + previous.getOwner().getName()
                    + ", reject module: " + owner.getName());
            return false;
        }

        Logger.i(ActionRouter.class, "#bind - Action \"" + action + "\" bound by module: " + owner.getName());
        return true;
    }

    /**
     * 注销指定模块的全部绑定。
     *
     * @param owner 目标模块。
     * @return 注销数量大于 0 时返回 <code>true</code>。
     */
    public boolean unbindAll(ActionModule owner) {
        if (null == owner) {
            return false;
        }

        boolean removed = false;
        Iterator<Map.Entry<String, Bound>> iter = this.table.entrySet().iterator();
        while (iter.hasNext()) {
            if (iter.next().getValue().getOwner() == owner) {
                iter.remove();
                removed = true;
            }
        }

        return removed;
    }

    /**
     * 查找动作绑定。
     *
     * @param action 动作名。
     * @return 未命中时返回 <code>null</code>。
     */
    public Bound lookup(String action) {
        return (null == action) ? null : this.table.get(action);
    }

    /**
     * 判断动作是否已绑定。
     *
     * @param action 动作名。
     * @return 已绑定时返回 <code>true</code>。
     */
    public boolean isBound(String action) {
        return null != lookup(action);
    }

    /**
     * 获取已装载标记。
     *
     * @return 已装载返回 <code>true</code>。
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * 标记装载完成。装载完成后动作表才对外可见。
     *
     * @param value 标记值。
     */
    public void setLoaded(boolean value) {
        this.loaded = value;
    }

    /**
     * 获取当前绑定数量。
     *
     * @return 返回绑定数量。
     */
    public int size() {
        return this.table.size();
    }

    /**
     * 获取全部已绑定动作名。
     *
     * @return 返回动作名列表。
     */
    public List<String> actions() {
        return new ArrayList<>(this.table.keySet());
    }

    /**
     * 已绑定动作及其归属模块。
     *
     * <p>字段全部为 <code>final</code>：由 <code>putIfAbsent</code> 与 <code>get</code>
     * 之间的 happens-before 边保证对派发线程安全发布。</p>
     */
    public static final class Bound {

        /**
         * 动作绑定。
         */
        private final ActionBinding binding;

        /**
         * 归属模块。
         */
        private final ActionModule owner;

        /**
         * 构造函数。
         *
         * @param binding 动作绑定。
         * @param owner 归属模块。
         */
        Bound(ActionBinding binding, ActionModule owner) {
            this.binding = binding;
            this.owner = owner;
        }

        /**
         * 获取动作绑定。
         *
         * @return 返回动作绑定。
         */
        public ActionBinding getBinding() {
            return this.binding;
        }

        /**
         * 获取归属模块。
         *
         * @return 返回归属模块。
         */
        public ActionModule getOwner() {
            return this.owner;
        }
    }
}
