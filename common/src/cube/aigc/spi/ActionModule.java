/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import java.util.List;

/**
 * AIGC 业务模块。
 *
 * <p>实现类必须：</p>
 * <ul>
 *     <li>是 <code>public</code> 类且提供 <code>public</code> 无参构造器；</li>
 *     <li>打包进 <code>deploy/libs/</code>（与宿主同一 ClassLoader），
 *         <b>禁止</b>放入 <code>deploy/cellets/</code>——后者由 Carpet 独立加载，
 *         会导致共享类型不可见；</li>
 *     <li>对 <code>cube-common-*.jar</code> 之外的依赖数为 0。</li>
 * </ul>
 */
public interface ActionModule {

    /**
     * 获取模块名。
     *
     * <p>全局唯一，用于动作命名空间、存储命名与资源目录前缀。</p>
     *
     * @return 返回模块名。
     */
    String getName();

    /**
     * 获取能力声明。
     *
     * @return 返回模块描述符。
     */
    ModuleDescriptor getDescriptor();

    /**
     * 装载模块。
     *
     * <p><b>并发契约</b>：宿主保证本方法返回之后才把本模块的
     * {@link ActionBinding} 注册进 {@link ActionRouter}。模块因此可以在
     * <code>setup</code> 中完成全部自身初始化，而派发线程一旦看到绑定，
     * 即可安全读到初始化结果（由注册动作建立 happens-before 边）。</p>
     *
     * <p>实现禁止阻塞超过 {@link ModuleDescriptor#bootTimeoutMs}；失败必须抛
     * {@link ModuleException}，由宿主按 {@link ModuleDescriptor#optional} 决定降级或拒绝。</p>
     *
     * @param host 宿主能力接口。
     * @throws ModuleException 装载失败。
     */
    void setup(AIGCHost host) throws ModuleException;

    /**
     * 卸载模块。
     *
     * <p>宿主保证在调用本方法之前已先从路由器注销本模块的全部绑定，
     * 因此本方法内不会再有新的派发到达。</p>
     */
    void teardown();

    /**
     * 宿主统一心跳。
     *
     * <p>对齐宿主既有的 60 秒 <code>onTick</code> 节奏。默认空实现。</p>
     *
     * @param now 当前时刻（毫秒）。
     */
    default void onTick(long now) {
    }

    /**
     * 获取本模块提供的全部动作绑定。
     *
     * <p>在 {@link #setup(AIGCHost)} <b>之前</b>调用，用于建立路由表，
     * 因此本方法不得依赖 <code>setup</code> 产生的状态。</p>
     *
     * @return 返回动作绑定列表，不可为 <code>null</code>。
     */
    List<ActionBinding> getActions();
}
