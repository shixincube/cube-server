/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.util.CachedQueueExecutor;
import cube.benchmark.ResponseTime;
import cube.common.entity.AbstractContact;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象的服务 Cellet 单元。
 */
public abstract class AbstractCellet extends Cellet {

    protected static ExecutorService sExecutor;

    protected AtomicLong listenedCounter = new AtomicLong(0L);

    protected ConcurrentHashMap<String, List<ResponseTime>> responseTimeMap = new ConcurrentHashMap<>();

    protected int maxListLength = 50;

    public AbstractCellet(String name) {
        super(name);
    }

    public static void initialize() {
        if (null == AbstractCellet.sExecutor) {
            AbstractCellet.sExecutor = CachedQueueExecutor.newCachedQueueThreadPool(32);
        }
    }

    public static void halt() {
        if (null != AbstractCellet.sExecutor) {
            AbstractCellet.sExecutor.shutdown();
        }
    }

    public ExecutorService getExecutor() {
        return AbstractCellet.sExecutor;
    }

    protected void execute(Runnable task) {
        AbstractCellet.sExecutor.execute(task);
    }

    /**
     * 获取数据接收计数器。
     *
     * @return 返回数据接收计数器。
     */
    public AtomicLong getListenedCounter() {
        return this.listenedCounter;
    }

    /**
     * 获取应答时间记录。
     *
     * @return 返回应答时间记录。
     */
    public Map<String, List<ResponseTime>> getResponseTimes() {
        return this.responseTimeMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        this.listenedCounter.incrementAndGet();
    }

    protected ResponseTime markResponseTime(String mark) {
        ResponseTime time = new ResponseTime(mark);
        time.beginning = System.currentTimeMillis();

        List<ResponseTime> list = this.responseTimeMap.get(mark);
        if (null == list) {
            list = new Vector<>();
            this.responseTimeMap.put(mark, list);
        }
        list.add(time);
        if (list.size() > this.maxListLength) {
            list.remove(0);
        }

        return time;
    }
}
