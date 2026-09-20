/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 报告服务。
 */
public class ReportService {

    private final static ReportService instance = new ReportService();

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

    public void submitReport(Report report) {
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

    private void processQueue() {
        // 用 CAS 保证同一时刻只有一个提交线程，避免并发重复启动
        if (!this.running.compareAndSet(false, true)) {
            return;
        }

        try {
            SubmitThread thread = new SubmitThread(this.hostUrls, this.reports,
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
