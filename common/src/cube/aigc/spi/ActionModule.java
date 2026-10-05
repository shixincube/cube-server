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

    /**
     * 声明本模块的动作名，<b>不要求实例化成功</b>。
     *
     * <p><b>为何需要它</b>：宿主在动作表未命中时，靠「已声明」判定
     * 该动作属于某个未就绪的模块，从而回 {@code AIGCStateCode#ModuleNotLoaded}
     * 而非让请求悬挂。但若声明只在实例化之后登记，那么
     * <b>模块类找不到、jar 缺失、清单文件不存在</b>这三种最常见的装载失败，
     * 声明表是空的——动作既不在模块里、也不在宿主分支里，
     * 请求将<b>无人应答而悬挂</b>。</p>
     *
     * <p>本方法在清单解析出类名之后、实例化之前被调用，因此它必须是
     * <b>静态</b>方法且不依赖实例状态。实现应返回该模块将提供的全部动作名；
     * 返回空列表表示「本模块不参与静态声明」，此时行为退化为
     * 仅靠 {@link #getActions()} 登记（能覆盖 setup 失败，
     * 但覆盖不了类加载失败）。</p>
     *
     * <p>实现约束：不得抛异常、不得阻塞、不得有副作用；
     * 宿主对本方法返回值的异常做吞没处理。</p>
     *
     * @return 返回动作名列表；不可为 <code>null</code>，无声明时返回空列表。
     */
    static List<String> declareActionNames() {
        return java.util.Collections.emptyList();
    }
}
