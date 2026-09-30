/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.entity.AIGCUnit;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 单元任务队列执行器。
 *
 * <p>统一「取队列 → 起任务 → 收尾」三步模板，替代原先散落在各业务入口里的
 * 「入队 + 裸 {@code new Thread} 派发」与忙等轮询。</p>
 *
 * <p>提供两种派发模式：</p>
 * <ul>
 *     <li><b>独占模式</b>（{@link #submit}、{@link #submitQueue}）：队列按单元查询键
 *         （{@link AIGCUnit#getQueryKey()}）隔离，每个单元同时只有一个排空任务，
 *         排队中的任务由该排空任务串行消化。适用于「一个单元一次只处理一个请求」的业务。</li>
 *     <li><b>共享模式</b>（{@link #submitShared}）：所有单元共用一个队列，允许最多
 *         {@code maxDrainers} 个排空任务并发拉取。适用于「限流而非串行」的业务。</li>
 * </ul>
 *
 * <p><b>为什么排空任务使用独立线程池</b>：排空任务会阻塞在模型推理上（单次可达数分钟），
 * 而服务级线程池被约 90 处短任务（场景子任务、DB 写入、回调）共用。两者混用会在并发推理
 * 增多时把短任务全部堵在队尾，造成服务整体停顿，因此这里使用独立的线程池与后台池隔离。</p>
 *
 * <p><b>为什么不用 {@code AIGCUnit#isRunning()} 判断「是否已有排空任务」</b>：该标志同时被
 * 单元选择器用于避让忙单元，且受「注册 60 秒内视为运行中」规则影响，不能作为排空归属的判据。
 * 本类用 {@link #drainingMap} 显式记录排空归属，从而消除旧实现中「排空任务已 poll 到空、
 * 尚未释放单元」时新任务入队导致<b>任务永久滞留</b>的竞态。</p>
 */
public class UnitQueueExecutor {

    /**
     * 单元排空线程上限。
     *
     * <p>该值必须大于「并发排空任务数上限」，否则会出现线程饥饿死锁：排空任务内部可能有
     * <b>嵌套等待</b>（例如单元元任务在处理过程中调用 {@code AIGCService#syncRetrieveReRank}，
     * 后者又需要提交一个排空任务并在原地等待其结果），若池中没有空闲线程，被等待的排空任务
     * 永远无法启动。</p>
     *
     * <p>并发排空任务数上限 ≈ 独占模式下的「单元数量」（每个单元至多一个排空任务）
     * 加上共享模式的并发上限（声学识别为 32）。128 对该规模留出 4 倍余量。</p>
     */
    public static final int DEFAULT_MAX_THREADS = 128;

    /**
     * 排空标记的键：作用域对象（身份比较）+ 槽位标识。
     *
     * <p>作用域必须参与比较：不同业务各持有自己的队列，同一单元的元任务可能同时出现在
     * 其中两个队列里，此时两者应各自独立排空。</p>
     */
    private static final class DrainKey {

        private final Object scope;

        private final String slot;

        DrainKey(Object scope, String slot) {
            this.scope = scope;
            this.slot = slot;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof DrainKey)) {
                return false;
            }
            DrainKey key = (DrainKey) other;
            // 作用域用身份比较：ConcurrentHashMap 的 equals 是内容比较，空表之间会互相相等。
            return key.scope == this.scope && key.slot.equals(this.slot);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.scope) * 31 + this.slot.hashCode();
        }

        @Override
        public String toString() {
            return this.slot;
        }
    }

    /**
     * 执行中的元任务集合。由本类维护，供外部查询与中断。
     */
    private final Collection<UnitMeta> runningMetas;

    /**
     * 正在排空的队列标记。增删必须与入队操作在同一把队列锁内进行。
     */
    private final Map<DrainKey, Boolean> drainingMap = new ConcurrentHashMap<>();

    /**
     * 单元排空线程池。
     */
    private final ExecutorService unitExecutor;

    /**
     * 服务级后台任务线程池。
     */
    private volatile ExecutorService backgroundExecutor;

    public UnitQueueExecutor(Collection<UnitMeta> runningMetas) {
        this(UnitQueueExecutor.DEFAULT_MAX_THREADS, runningMetas);
    }

    public UnitQueueExecutor(int maxThreads, Collection<UnitMeta> runningMetas) {
        this.runningMetas = runningMetas;
        this.unitExecutor = CachedQueueExecutor.newCachedQueueThreadPool(Math.max(1, maxThreads));
    }

    public void setBackgroundExecutor(ExecutorService executor) {
        this.backgroundExecutor = executor;
    }

    /**
     * 提交通用后台任务。
     *
     * <p>这是所有非单元队列异步任务的统一入口：调用方不再自行区分「提交到服务线程池」
     * 与「新起一个裸线程」，池化策略与异常兜底集中在此。</p>
     *
     * @param task 待执行的任务。
     */
    public void execute(Runnable task) {
        ExecutorService executor = this.backgroundExecutor;
        if (null == executor) {
            Logger.e(this.getClass(), "#execute - Background executor is NOT available, task dropped");
            return;
        }

        try {
            executor.execute(task);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#execute - Can NOT submit background task", e);
        }
    }

    /**
     * 元任务入队并触发排空（独占模式，队尾追加）。
     *
     * @param queueMap 该业务持有的队列集合。
     * @param meta     元任务。
     */
    public void submit(Map<String, Queue<UnitMeta>> queueMap, UnitMeta meta) {
        this.submitQueue(queueMap, ConcurrentLinkedQueue::new, meta, false);
    }

    /**
     * 元任务入队并触发排空（独占模式），支持插入队首。
     *
     * <p>音频流需要「新到的数据优先处理」，因此需要插队能力；请求插队时队列实现必须
     * 实现 {@link Deque}，否则会被拒绝插队并记录错误（不做静默降级）。</p>
     *
     * @param queueMap 该业务持有的队列集合。
     * @param factory  队列为空时使用的创建器。
     * @param meta     元任务。
     * @param toFirst  为 {@code true} 时插入队首，否则追加到队尾。
     * @param <Q>      队列类型。
     */
    public <Q extends Queue<UnitMeta>> void submitQueue(Map<String, Q> queueMap, Supplier<Q> factory,
                                                       UnitMeta meta, boolean toFirst) {
        if (!this.checkMeta(meta)) {
            return;
        }

        final AIGCUnit unit = meta.unit;
        final String queryKey = unit.getQueryKey();
        final DrainKey drainKey = new DrainKey(queueMap, queryKey);

        DrainKey started = null;
        Queue<UnitMeta> target = null;

        synchronized (queueMap) {
            Q queue = queueMap.computeIfAbsent(queryKey, k -> factory.get());

            // 入队与「排空归属」的判定必须在同一个锁内完成：
            // 否则排空任务在 poll 到空之后、撤销标记之前入队的任务会永远滞留。
            synchronized (queue) {
                if (toFirst) {
                    if (queue instanceof Deque) {
                        ((Deque<UnitMeta>) queue).addFirst(meta);
                    }
                    else {
                        // 不支持插队的队列：明确报错，不做静默降级
                        Logger.e(this.getClass(), "#submitQueue - Queue does NOT support addFirst,"
                                + " append to tail - " + queue.getClass().getName());
                        queue.offer(meta);
                    }
                }
                else {
                    queue.offer(meta);
                }

                if (null == this.drainingMap.putIfAbsent(drainKey, Boolean.TRUE)) {
                    // 抢占成功：由本次提交负责启动排空任务。
                    started = drainKey;
                    target = queue;
                    unit.setRunning(true);
                }
            }
        }

        if (null != started) {
            final Queue<UnitMeta> queue = target;
            final DrainKey key = started;
            try {
                this.unitExecutor.execute(() -> this.drain(unit, queue, key));
            } catch (Exception e) {
                // 线程池不可用（服务正在停止）：撤销占位，避免队列永久滞留。
                this.drainingMap.remove(key);
                unit.setRunning(false);
                Logger.w(this.getClass(), "#submitQueue - Can NOT start drain task", e);
            }
        }
    }

    /**
     * 元任务入队并触发排空（共享模式）。
     *
     * <p>所有元任务共用同一个队列，最多 {@code maxDrainers} 个排空任务并发拉取，
     * 每个元任务在执行期间自行占用与释放其所属单元。</p>
     *
     * <p>与独占模式的差别是「并发上限」而非「串行保证」：调用方用 {@code maxDrainers}
     * 表达期望的并发度，实际并发数不会超过它，也不会超过队列中排队的任务数。</p>
     *
     * @param queue        共享队列。
     * @param meta         元任务。
     * @param maxDrainers  并发排空任务上限，小于 1 时按 1 处理。
     */
    public void submitShared(Queue<UnitMeta> queue, UnitMeta meta, int maxDrainers) {
        if (!this.checkMeta(meta)) {
            return;
        }

        final int limit = Math.max(1, maxDrainers);

        DrainKey started = null;

        synchronized (queue) {
            queue.offer(meta);

            // 逐槽位抢占：槽位被占用说明已有对应数量的排空任务在跑。
            for (int slot = 0; slot < limit; ++slot) {
                DrainKey candidate = new DrainKey(queue, Integer.toString(slot));
                if (null == this.drainingMap.putIfAbsent(candidate, Boolean.TRUE)) {
                    started = candidate;
                    break;
                }
            }
        }

        if (null != started) {
            final DrainKey key = started;
            try {
                this.unitExecutor.execute(() -> this.drainShared(queue, key));
            } catch (Exception e) {
                this.drainingMap.remove(key);
                Logger.w(this.getClass(), "#submitShared - Can NOT start drain task", e);
            }
        }
    }

    /**
     * 排空独占模式队列（一个单元对应一个排空任务）。
     *
     * @param unit     队列归属的单元。
     * @param queue    队列。
     * @param drainKey 排空标记键。
     */
    private void drain(AIGCUnit unit, Queue<UnitMeta> queue, DrainKey drainKey) {
        try {
            while (true) {
                UnitMeta meta;

                synchronized (queue) {
                    meta = queue.poll();
                    if (null == meta) {
                        // 在队列锁内一次性完成「撤销排空标记 → 复检队列 → 释放单元」：
                        // 「撤销」与入队方的「入队 + 抢占」互斥，因此并发入队的任务不会被漏掉，
                        // 也不会出现「释放单元」被后启动的排空任务覆盖的情况。
                        this.drainingMap.remove(drainKey);
                        meta = queue.poll();
                        if (null == meta) {
                            unit.setRunning(false);
                            return;
                        }
                        // 撤销标记期间确实有新任务进入：重新占位，继续排空。
                        this.drainingMap.put(drainKey, Boolean.TRUE);
                    }
                }

                this.processMeta(meta);
            }
        } catch (Throwable t) {
            // 兜底：异常逃逸时释放排空标记与单元，避免该队列被永久卡住。
            // 注意这里不能再有 finally 释放单元：正常退出路径已在队列锁内释放，
            // 锁外再释放一次会覆盖「刚抢占成功的新排空任务」所占用的单元。
            Logger.e(this.getClass(), "#drain - Unexpected error", t);
            this.releaseDrainKey(queue, drainKey);
            unit.setRunning(false);
        }
    }

    /**
     * 排空共享模式队列（多个排空任务并发拉取同一队列）。
     *
     * @param queue    共享队列。
     * @param drainKey 排空标记键。
     */
    private void drainShared(Queue<UnitMeta> queue, DrainKey drainKey) {
        try {
            while (true) {
                UnitMeta meta;

                synchronized (queue) {
                    meta = queue.poll();
                    if (null == meta) {
                        // 与 submitShared 的「入队 + 抢占槽位」互斥，保证不丢任务。
                        this.drainingMap.remove(drainKey);
                        meta = queue.poll();
                        if (null == meta) {
                            return;
                        }
                        this.drainingMap.put(drainKey, Boolean.TRUE);
                    }
                }

                // 共享模式下每个元任务自行占用与释放所属单元。
                boolean acquired = false;
                try {
                    meta.unit.setRunning(true);
                    acquired = true;
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#drainShared - Can NOT acquire unit", e);
                }

                try {
                    this.processMeta(meta);
                } finally {
                    if (acquired) {
                        meta.unit.setRunning(false);
                    }
                }
            }
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#drainShared - Unexpected error", t);
            this.releaseDrainKey(queue, drainKey);
        }
    }

    /**
     * 执行单个元任务并维护运行中集合。
     *
     * @param meta 元任务。
     */
    private void processMeta(UnitMeta meta) {
        synchronized (this.runningMetas) {
            this.runningMetas.add(meta);
        }

        try {
            meta.process();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#processMeta - Meta process error", e);
        } finally {
            synchronized (this.runningMetas) {
                this.runningMetas.remove(meta);
            }
        }
    }

    /**
     * 释放排空标记。
     *
     * @param queue    队列锁。
     * @param drainKey 排空标记键。
     */
    private void releaseDrainKey(Queue<UnitMeta> queue, DrainKey drainKey) {
        synchronized (queue) {
            this.drainingMap.remove(drainKey);
        }
    }

    /**
     * 校验元任务与所属单元。
     *
     * @param meta 元任务。
     * @return 合法返回 {@code true}。
     */
    private boolean checkMeta(UnitMeta meta) {
        if (null == meta || null == meta.unit) {
            Logger.e(this.getClass(), "#checkMeta - Invalid unit meta");
            return false;
        }
        return true;
    }

    /**
     * 关闭单元排空线程池。
     */
    public void shutdown() {
        this.unitExecutor.shutdown();
        this.drainingMap.clear();
    }
}
