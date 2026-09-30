/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIGC 任务执行器。
 *
 * <p>本类是「任务从进入服务到被单元消化」这一段链路的唯一持有者：</p>
 * <ul>
 *     <li><b>各能力的任务队列</b>：文本生成、文生图、文生文件、多模态、语义搜索、检索重排
 *         各持有独立的「查询键 → 队列」映射；声学识别使用共享队列；音频流使用可插队的双端队列。</li>
 *     <li><b>两层线程池</b>：单元排空任务走 {@link UnitQueueExecutor} 的独立池，
 *         服务级短任务走本类的后台池（由 {@code aigc.properties} 的
 *         {@code threadpool.max} / {@code threadpool.type} 配置）。</li>
 *     <li><b>派发模板</b>：入队与排空统一委托给 {@link UnitQueueExecutor}
 *         （取队列 → 起任务 → 收尾），本类只负责「哪个能力用哪个队列、用什么模式」。</li>
 *     <li><b>运行计数</b>：按单元能力名统计正在执行的生成任务数量，供控制台观测。</li>
 * </ul>
 *
 * <p><b>为什么排空任务与短任务不能共用线程池</b>：排空任务会阻塞在模型推理上，
 * 而服务级后台池被约 90 处短任务（场景子任务、DB 写入、回调）共用且使用共享 FIFO 队列，
 * 混用会让长任务把短任务全部堵在队尾，造成服务整体停顿。</p>
 */
public class AIGCTaskExecutor {

    /**
     * 后台线程池的缺省大小。配置文件缺失或未变更时按该值补建，避免线程池恒为空
     * 导致所有异步入口静默失效。
     */
    private static final int DEFAULT_BACKGROUND_THREADS = 8;

    /**
     * 声学识别并发上限（共享队列的排空任务数上限）。
     */
    private static final int MAX_SPEECH_CONCURRENCES = 32;

    /**
     * 文本生成队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> generateTextQueueMap;

    /**
     * 文生文件队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> textToFileQueueMap;

    /**
     * 文生图队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> textToImageQueueMap;

    /**
     * 多模态队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> multimodalQueueMap;

    /**
     * 语义搜索队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> semanticSearchQueueMap;

    /**
     * 检索重排队列。Key 是单元的查询键。
     */
    private final Map<String, Queue<UnitMeta>> retrieveReRankQueueMap;

    /**
     * 声学识别共享队列。并发度由 {@link #MAX_SPEECH_CONCURRENCES} 限定，
     * 多个排空任务并发拉取同一个队列，而不是按单元串行。
     */
    private final Queue<UnitMeta> speechQueue;

    /**
     * 音频流队列。Key 是单元的查询键，允许插队。
     */
    private final Map<String, LinkedList<UnitMeta>> audioQueueMap;

    /**
     * 正在执行的元任务集合。与 {@link UnitQueueExecutor} 共享同一实例。
     */
    private final List<UnitMeta> runningMetas;

    /**
     * 单元任务队列执行器：统一的「取队列 → 起任务 → 收尾」模板。
     */
    private final UnitQueueExecutor unitQueueExecutor;

    /**
     * 服务级后台任务线程池，仅供短任务使用。
     */
    private volatile ExecutorService backgroundExecutor;

    /**
     * 按单元能力名统计的生成任务实时计数。
     */
    private final ConcurrentHashMap<String, AtomicInteger> unitTaskCountMap;

    public AIGCTaskExecutor() {
        this.generateTextQueueMap = new ConcurrentHashMap<>();
        this.textToFileQueueMap = new ConcurrentHashMap<>();
        this.textToImageQueueMap = new ConcurrentHashMap<>();
        this.multimodalQueueMap = new ConcurrentHashMap<>();
        this.semanticSearchQueueMap = new ConcurrentHashMap<>();
        this.retrieveReRankQueueMap = new ConcurrentHashMap<>();
        this.speechQueue = new ConcurrentLinkedQueue<>();
        this.audioQueueMap = new ConcurrentHashMap<>();
        this.runningMetas = new LinkedList<>();
        this.unitTaskCountMap = new ConcurrentHashMap<>();

        // 单元排空任务使用独立线程池，与后台池隔离
        this.unitQueueExecutor = new UnitQueueExecutor(this.runningMetas);
    }

    /**
     * 创建服务级后台线程池，并绑定给单元任务队列执行器。
     *
     * <p>线程池只创建一次：首次按配置创建，之后调用仅用于补绑定。</p>
     *
     * @param max  最大线程数，小于 1 时按 1 处理。
     * @param type 线程池类型，{@code cached} 使用缓存队列线程池，其余使用固定线程池。
     * @return 返回后台线程池。
     */
    public synchronized ExecutorService createExecutor(int max, String type) {
        if (null != this.backgroundExecutor) {
            this.unitQueueExecutor.setBackgroundExecutor(this.backgroundExecutor);
            return this.backgroundExecutor;
        }

        int size = Math.max(1, max);
        if (type.equalsIgnoreCase("cached")) {
            this.backgroundExecutor = CachedQueueExecutor.newCachedQueueThreadPool(size);
            Logger.i(this.getClass(), "AI Service - Thread pool type: cached - max: " + size);
        }
        else {
            this.backgroundExecutor = Executors.newFixedThreadPool(size);
            Logger.i(this.getClass(), "AI Service - Thread pool type: fixed - max: " + size);
        }

        // 通用后台任务（非单元队列）统一走该线程池
        this.unitQueueExecutor.setBackgroundExecutor(this.backgroundExecutor);
        return this.backgroundExecutor;
    }

    /**
     * 获取服务级后台线程池。
     *
     * @return 返回线程池，尚未创建时返回 {@code null}。
     */
    public ExecutorService getExecutor() {
        return this.backgroundExecutor;
    }

    /**
     * 获取单元任务队列执行器。
     *
     * @return 返回执行器。
     */
    public UnitQueueExecutor getUnitQueueExecutor() {
        return this.unitQueueExecutor;
    }

    /**
     * 提交通用后台任务。
     *
     * @param task 待执行的任务。
     */
    public void execute(Runnable task) {
        this.unitQueueExecutor.execute(task);
    }

    /**
     * 提交文本生成任务（独占模式，同单元串行）。
     *
     * @param meta 元任务。
     */
    public void submitGenerateText(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.generateTextQueueMap, meta);
    }

    /**
     * 提交多模态任务（独占模式）。
     *
     * @param meta 元任务。
     */
    public void submitMultimodal(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.multimodalQueueMap, meta);
    }

    /**
     * 提交文生图任务（独占模式）。
     *
     * @param meta 元任务。
     */
    public void submitTextToImage(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.textToImageQueueMap, meta);
    }

    /**
     * 提交文生文件任务（独占模式）。
     *
     * @param meta 元任务。
     */
    public void submitTextToFile(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.textToFileQueueMap, meta);
    }

    /**
     * 提交语义搜索任务（独占模式）。
     *
     * @param meta 元任务。
     */
    public void submitSemanticSearch(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.semanticSearchQueueMap, meta);
    }

    /**
     * 提交检索重排任务（独占模式）。
     *
     * @param meta 元任务。
     */
    public void submitRetrieveReRank(UnitMeta meta) {
        this.unitQueueExecutor.submit(this.retrieveReRankQueueMap, meta);
    }

    /**
     * 提交声学识别任务（共享模式，最多 {@link #MAX_SPEECH_CONCURRENCES} 个任务并发）。
     *
     * @param meta 元任务。
     */
    public void submitSpeechRecognition(UnitMeta meta) {
        this.unitQueueExecutor.submitShared(this.speechQueue, meta, MAX_SPEECH_CONCURRENCES);
    }

    /**
     * 提交音频流任务（独占模式，支持插队）。
     *
     * @param meta    元任务。
     * @param toFirst 为 {@code true} 时插入队首。
     */
    public void submitAudio(UnitMeta meta, boolean toFirst) {
        this.unitQueueExecutor.submitQueue(this.audioQueueMap, LinkedList::new, meta, toFirst);
    }

    /**
     * 判断指定文件是否已有音频任务正在执行。
     *
     * <p>用于避免同一文件的音频任务被重复提交。</p>
     *
     * @param fileCode 文件码。
     * @return 存在正在执行的同文件任务返回 {@code true}。
     */
    public boolean hasPendingAudio(String fileCode) {
        synchronized (this.runningMetas) {
            for (UnitMeta meta : this.runningMetas) {
                if (meta instanceof AudioUnitMeta) {
                    AudioUnitMeta aum = (AudioUnitMeta) meta;
                    if (aum.getFile().getFileCode().equals(fileCode)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 从音频队列中移除指定文件的待处理任务。
     *
     * <p>用于停止语音流时清理尚未开始执行的任务。</p>
     *
     * @param fileCodes 文件码集合。
     */
    public void discardAudio(Collection<String> fileCodes) {
        if (null == fileCodes || fileCodes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, LinkedList<UnitMeta>> entry : this.audioQueueMap.entrySet()) {
            LinkedList<UnitMeta> metas = entry.getValue();
            synchronized (metas) {
                Iterator<UnitMeta> metaIterator = metas.iterator();
                while (metaIterator.hasNext()) {
                    UnitMeta meta = metaIterator.next();
                    if (!(meta instanceof AudioUnitMeta)) {
                        continue;
                    }

                    AudioUnitMeta aum = (AudioUnitMeta) meta;

                    // 删除指定文件码的 meta
                    for (String fileCode : fileCodes) {
                        if (aum.getFile().getFileCode().equals(fileCode)) {
                            metaIterator.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 开始一个单元任务，返回该单元能力名对应的实时计数器。
     *
     * <p>用 <code>computeIfAbsent</code> 保证「取计数器」这一步是原子的，避免
     * <code>get</code> + <code>put</code> 在并发下重复创建计数器而丢计数。
     * 调用方必须在 <code>finally</code> 中递减返回的计数器。</p>
     *
     * @param unitName 单元能力名称。
     * @return 返回该单元名对应的计数器。
     */
    public AtomicInteger beginUnitTask(String unitName) {
        AtomicInteger count = this.unitTaskCountMap.computeIfAbsent(unitName,
                k -> new AtomicInteger(0));
        count.incrementAndGet();
        return count;
    }

    /**
     * 返回生成文本单元实时运行计数表。
     *
     * @return 返回计数表。
     */
    public Map<String, AtomicInteger> getUnitTaskCountMap() {
        return this.unitTaskCountMap;
    }

    /**
     * 关闭线程池并释放排队中的排空标记。
     */
    public void shutdown() {
        ExecutorService executor = this.backgroundExecutor;
        if (null != executor) {
            executor.shutdown();
            this.backgroundExecutor = null;
        }

        // 单元排空线程池一并关闭，避免停止后仍持有单元资源
        this.unitQueueExecutor.shutdown();
    }
}
