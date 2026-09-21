/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import cell.util.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 报告服务。
 *
 * <p>
 * 熔断策略（2026-09-21）——控制台未启动时节点不应无休止地做无效提交：
 * </p>
 * <ul>
 *     <li>连续 {@link #MAX_CONSECUTIVE_FAILURES} 次提交均失败后，进入
 *     {@link #SUSPEND_DURATION} 毫秒的暂停期。暂停期内 {@link #submitReport(Report)}
 *     直接丢弃报告，不再产生任何提交线程与网络请求开销。</li>
 *     <li>暂停期结束后进入「试探」状态：连续失败计数清零，重新获得
 *     {@link #MAX_CONSECUTIVE_FAILURES} 次尝试机会。仍全部失败则再次暂停
 *     {@link #SUSPEND_DURATION} 毫秒；只要有一次提交成功即清零计数，恢复按原有间隔上报。</li>
 *     <li>进入暂停期时清空队列与重试计数：监控数据以时效优先，恢复后重新采集，
 *     不补报 30 分钟前的陈旧数据。</li>
 *     <li>「一次提交」以控制台侧的一次投递为单位（{@code SubmitThread} 对单份报告
 *     调用一次 {@code submit()}），连接失败与被拒绝（HTTP 非 200）同样计入失败。</li>
 * </ul>
 */
public class ReportService {

    private final static ReportService instance = new ReportService();

    /**
     * 触发暂停所需的连续提交失败次数。
     */
    public final static int MAX_CONSECUTIVE_FAILURES = 10;

    /**
     * 连续失败后暂停提交的时长（毫秒）：30 分钟。
     */
    public final static long SUSPEND_DURATION = 30 * 60 * 1000L;

    /**
     * 接收报告的主机 URL 列表。
     */
    private List<String> hostUrls;

    /**
     * 报告清单。
     */
    private ConcurrentLinkedQueue<Report> reports;

    /**
     * 存储到内存里的最大报告数量。
     */
    private int maxQueueLength = 20;

    /**
     * 提交报告线程是否正在执行。
     */
    private AtomicBoolean running;

    /**
     * 报告的重试计数。跨提交线程持久保存，避免每轮新建线程导致重试预算被清零。
     * 使用 IdentityHashMap 语义：以报告对象身份为键。
     */
    private Map<Report, Integer> retries;

    /**
     * 保护熔断状态的锁。
     */
    private final Object stateLock = new Object();

    /**
     * 连续提交失败计数。任一提交成功即清零。
     */
    private int consecutiveFailures = 0;

    /**
     * 暂停提交的截止时间戳，0 表示当前未暂停。
     */
    private long suspendedUntil = 0;

    private ReportService() {
        this.hostUrls = new ArrayList<>();
        this.reports = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(false);
        this.retries = Collections.synchronizedMap(new IdentityHashMap<Report, Integer>());
    }

    public final static ReportService getInstance() {
        return ReportService.instance;
    }

    public void addHost(String address, int port) {
        this.hostUrls.add("http://" + address + ":" + port + "/report");
    }

    public void setMaxQueueLength(int maxQueueLength) {
        this.maxQueueLength = maxQueueLength;
    }

    /**
     * 队列里当前缓存的报告数量。
     */
    public int getQueueLength() {
        return this.reports.size();
    }

    /**
     * 当前是否处于暂停期（连续提交失败已触发熔断）。
     */
    boolean isSuspended() {
        synchronized (this.stateLock) {
            return 0 != this.suspendedUntil && System.currentTimeMillis() < this.suspendedUntil;
        }
    }

    /**
     * 当前的连续提交失败次数。仅供同包内诊断与自检使用。
     */
    int getConsecutiveFailures() {
        synchronized (this.stateLock) {
            return this.consecutiveFailures;
        }
    }

    public void submitReport(Report report) {
        if (!this.canSubmit()) {
            // 暂停期内直接丢弃：等恢复后控制台会收到重新采集的数据
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "Report: \"" + report.getName() + "\" ("
                        + report.getReporter() + ") skipped - reporting suspended");
            }
            return;
        }

        this.reports.offer(report);

        // 队列超长时丢弃最旧的报告，保证内存占用与数据新鲜度
        while (this.reports.size() > this.maxQueueLength) {
            Report dropped = this.reports.poll();
            if (null != dropped) {
                this.retries.remove(dropped);
            }
        }

        this.processQueue();
    }

    /**
     * 判断当前是否允许提交报告。若暂停期已结束则自动切换到「试探」状态：
     * 清零连续失败计数，允许重新尝试 {@link #MAX_CONSECUTIVE_FAILURES} 次。
     *
     * @return 允许提交返回 {@code true}。
     */
    private boolean canSubmit() {
        synchronized (this.stateLock) {
            if (0 == this.suspendedUntil) {
                return true;
            }

            if (System.currentTimeMillis() < this.suspendedUntil) {
                return false;
            }

            this.suspendedUntil = 0;
            this.consecutiveFailures = 0;

            Logger.i(this.getClass(), "Resume submitting reports - retry up to "
                    + MAX_CONSECUTIVE_FAILURES + " times");

            return true;
        }
    }

    /**
     * 提交线程回调一次提交的结果。连续失败达到 {@link #MAX_CONSECUTIVE_FAILURES} 次
     * 即进入 {@link #SUSPEND_DURATION} 毫秒的暂停期；任一成功则清零失败计数。
     *
     * @param success 本次提交是否成功。
     */
    void onSubmitResult(boolean success) {
        synchronized (this.stateLock) {
            if (success) {
                if (this.consecutiveFailures > 0) {
                    Logger.i(this.getClass(), "Submitting reports to console recovered after "
                            + this.consecutiveFailures + " consecutive failures");
                }

                this.consecutiveFailures = 0;
                this.suspendedUntil = 0;
                return;
            }

            if (this.isSuspended()) {
                // 已处于暂停期，不再重复计数
                return;
            }

            ++this.consecutiveFailures;
            if (this.consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                return;
            }

            this.consecutiveFailures = 0;
            this.suspendedUntil = System.currentTimeMillis() + SUSPEND_DURATION;

            // 清空积压：暂停期结束后重新采集，避免补报陈旧数据
            int dropped = 0;
            while (null != this.reports.poll()) {
                ++dropped;
            }
            this.retries.clear();

            Logger.w(this.getClass(), "Console unreachable for " + MAX_CONSECUTIVE_FAILURES
                    + " consecutive submits - suspend reporting for "
                    + (SUSPEND_DURATION / 1000 / 60) + " minutes"
                    + (dropped > 0 ? " (" + dropped + " queued reports dropped)" : ""));
        }
    }

    private void processQueue() {
        // 用 CAS 保证同一时刻只有一个提交线程，避免并发重复启动
        if (!this.running.compareAndSet(false, true)) {
            return;
        }

        try {
            SubmitThread thread = new SubmitThread(this, this.hostUrls, this.reports,
                    this.maxQueueLength, this.retries, this.running);
            thread.setDaemon(true);
            thread.start();
        }
        catch (Throwable t) {
            // 线程启动失败必须复位标志位，否则后续报告永远不会被提交
            this.running.set(false);
            throw t;
        }
    }
}
