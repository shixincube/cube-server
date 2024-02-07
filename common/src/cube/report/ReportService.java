/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.report;

import java.util.ArrayList;
import java.util.List;
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

    private ReportService() {
        this.hostUrls = new ArrayList<>();
        this.reports = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(false);
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

    public void submitReport(Report report) {
        this.reports.offer(report);

        if (this.reports.size() > this.maxQueueLength) {
            // 当队列超长时，删除队首报告
            this.reports.poll();
        }

        if (!this.running.get()) {
            this.running.set(true);
            this.processQueue();
        }
    }

    private void processQueue() {
        SubmitThread thread = new SubmitThread(this.hostUrls, this.reports, this.running);
        thread.start();
    }
}
