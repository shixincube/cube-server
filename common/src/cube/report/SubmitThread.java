/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import cell.util.log.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EofException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 提交线程。
 *
 * <p>
 * 设计要点（2026-09-20 重写）：
 * </p>
 * <ul>
 *     <li>使用 {@code poll()} 取报告而不是 {@code peek()}。原实现用 {@code peek()}，
 *     一旦队首报告无法投递就 {@code break}，队首没有被移除，后续所有报告都被它挡住，
 *     直到队列再次超长触发裁剪——期间控制台看到的是「数据时有时无」。</li>
 *     <li>失败的报告在重试预算内重新入队尾（受队列长度上限约束），超出预算即丢弃。
 *     监控数据新鲜度优先于完整性，不能让一份坏报告拖死整条链路。</li>
 *     <li>{@code running} 标志位的复位放在 {@code finally} 里。原实现只有正常走到方法末尾才复位，
 *     一旦有异常逃逸（例如 {@code HttpClient} 未成功启动就发起请求），
 *     标志位会永久停留在 true，此后 {@code ReportService} 再也不会启动提交线程——
 *     节点彻底静默，且必须重启进程才能恢复。</li>
 *     <li>整个生命周期只创建一个 {@code HttpClient} 并在结束时关闭，避免每次上报都新建
 *     客户端（含线程池与 Selector）造成的资源抖动。</li>
 *     <li><b>控制台未启动属预期情况</b>：连接类异常（拒绝连接、DNS 失败、连接超时等）
 *     只以 DEBUG 等级输出一行提示，不打印异常堆栈，避免服务端日志被刷屏；
 *     其它异常仍按 WARN 记录，以免掩盖真实缺陷。</li>
 * </ul>
 */
public class SubmitThread extends Thread {

    /**
     * 提交结果：成功。
     */
    private final static int SUBMITTED = 0;

    /**
     * 提交结果：控制台可达但拒绝（HTTP 状态非 200）。
     */
    private final static int REJECTED = 1;

    /**
     * 提交结果：控制台不可达（未启动、网络不通、连接超时等）。
     */
    private final static int UNREACHABLE = 2;

    /**
     * 单份报告的最大重试次数，超过后丢弃。
     */
    private final static int MAX_RETRY = 3;

    /**
     * 单次 HTTP 请求超时（毫秒）。
     */
    private final static long REQUEST_TIMEOUT = 10 * 1000;

    /**
     * 建立连接超时（毫秒）。
     */
    private final static long CONNECT_TIMEOUT = 3 * 1000;

    private List<String> hostUrls;

    private ConcurrentLinkedQueue<Report> queue;

    private int maxQueueLength;

    private Map<Report, Integer> retries;

    private AtomicBoolean running;

    public SubmitThread(List<String> hostUrls, ConcurrentLinkedQueue<Report> queue,
                        int maxQueueLength, Map<Report, Integer> retries, AtomicBoolean running) {
        super("SubmitThread");
        this.hostUrls = hostUrls;
        this.queue = queue;
        this.maxQueueLength = maxQueueLength;
        this.retries = retries;
        this.running = running;
    }

    @Override
    public void run() {
        HttpClient client = null;
        try {
            client = new HttpClient();
            client.setConnectTimeout(CONNECT_TIMEOUT);
            client.start();

            this.drain(client);
        }
        catch (Throwable t) {
            Logger.w(this.getClass(), "Submit thread aborted", t);
        }
        finally {
            if (null != client) {
                try {
                    client.stop();
                }
                catch (Exception e) {
                    // Nothing
                }
            }

            // 必须复位，否则上报链路会永久静默
            this.running.set(false);
        }
    }

    /**
     * 排空队列。失败的报告在重试预算内重新入队尾，留给下一轮提交（不在此轮内热重试），
     * 预算耗尽即丢弃。重试计数保存在 {@link ReportService} 中，跨线程累计。
     */
    private void drain(HttpClient client) {
        List<Report> retryLater = new ArrayList<>();

        Report report = null;
        while ((report = this.queue.poll()) != null) {
            int result = this.submit(report, client);

            if (SUBMITTED == result) {
                this.retries.remove(report);
                continue;
            }

            Integer count = this.retries.get(report);
            int attempts = ((null == count) ? 0 : count) + 1;

            if (attempts >= MAX_RETRY) {
                this.retries.remove(report);
                this.logDrop(report, attempts, result);
                continue;
            }

            this.retries.put(report, attempts);
            retryLater.add(report);
        }

        // 排空后再放回队尾，让重试与下一次上报（约 10 秒后）自然错开
        for (Report item : retryLater) {
            if (this.queue.size() < this.maxQueueLength) {
                this.queue.offer(item);
            }
            else {
                this.retries.remove(item);
            }
        }
    }

    /**
     * 记录一份报告被丢弃。控制台不可达属预期情况，只以 DEBUG 提示。
     */
    private void logDrop(Report report, int attempts, int result) {
        if (UNREACHABLE == result) {
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "Report: \"" + report.getName() + "\" ("
                        + report.getReporter() + ") dropped - console unreachable - attempts "
                        + attempts);
            }
        }
        else {
            Logger.w(this.getClass(), "Drop report: \"" + report.getName() + "\" ("
                    + report.getReporter() + ") after " + attempts + " attempts");
        }
    }

    /**
     * 向所有目标主机提交一份报告，任一主机返回 200 即视为成功。
     *
     * @param report
     * @param client
     * @return {@link #SUBMITTED}、{@link #REJECTED} 或 {@link #UNREACHABLE}。
     */
    private int submit(Report report, HttpClient client) {
        String json = null;
        try {
            json = report.toJSON().toString();
        }
        catch (Exception e) {
            Logger.w(this.getClass(), "Serialize report \"" + report.getName() + "\" failed", e);
            return SUBMITTED;
        }

        if (this.hostUrls.isEmpty()) {
            return SUBMITTED;
        }

        // 只要有一次不是「不可达」，就按「可达但被拒绝」处理，避免真实缺陷被降级成 DEBUG
        boolean unreachable = true;

        for (String url : this.hostUrls) {
            StringContentProvider provider = new StringContentProvider(json);

            try {
                long time = System.currentTimeMillis();
                ContentResponse response = client.POST(url).content(provider)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS).send();
                long duration = System.currentTimeMillis() - time;

                if (response.getStatus() == HttpStatus.OK_200) {
                    Logger.i(this.getClass(), "Report: \"" + report.getName() + "\" ("
                            + report.getReporter() + ") submitted - " + url + " - " + duration);
                    return SUBMITTED;
                }

                unreachable = false;
                Logger.w(this.getClass(), "Report: \"" + report.getName() + "\" ("
                        + report.getReporter() + ") submit failed - " + url + " - "
                        + response.getStatus() + " - " + duration);
            }
            catch (InterruptedException e) {
                // 恢复中断状态
                Thread.currentThread().interrupt();
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "Report: \"" + report.getName() + "\" ("
                            + report.getReporter() + ") submit interrupted - " + url);
                }
            }
            catch (Exception e) {
                if (isConnectionFailure(e)) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "Report: \"" + report.getName() + "\" ("
                                + report.getReporter() + ") skipped - console unreachable - "
                                + url + " - " + describe(e));
                    }
                }
                else {
                    unreachable = false;
                    Logger.w(this.getClass(), "Submitting report \"" + report.getName() + "\" ("
                            + report.getReporter() + ") failed - " + url, e);
                }
            }
        }

        return unreachable ? UNREACHABLE : REJECTED;
    }

    /**
     * 判断异常链是否属于「控制台不可达」这类连接期异常。
     */
    private static boolean isConnectionFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (null != cause) {
            if (cause instanceof ConnectException
                    || cause instanceof SocketException
                    || cause instanceof UnknownHostException
                    || cause instanceof NoRouteToHostException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof UnresolvedAddressException
                    || cause instanceof TimeoutException
                    || cause instanceof EofException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    /**
     * 取最内层异常的描述，用于 DEBUG 提示。
     */
    private static String describe(Throwable throwable) {
        Throwable cause = throwable;
        while (null != cause.getCause()) {
            cause = cause.getCause();
        }

        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
