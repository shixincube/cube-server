/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.mgmt.HostMonitor;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 主机信息句柄。
 *
 * <p>静态配置与动态性能分成两个接口：静态信息几乎不变，前端只需拉一次；
 * 动态指标按秒级轮询，单独走一个轻量接口，避免每次刷新都重算硬件规格。</p>
 *
 * <ul>
 *   <li>{@code GET /host/static} —— 主机静态配置（CPU 规格、内存总量、磁盘、网卡、操作系统、运行时）。</li>
 *   <li>{@code GET /host/metrics} —— 主机动态性能（CPU 使用率与构成、内存、网络 / 磁盘吞吐、分区占用、系统负载）。</li>
 * </ul>
 *
 * <p>说明：与工程内其他只读查询接口（{@code /servers}、{@code /statistic}、{@code /log}）保持一致，
 * 本句柄不做 Cookie 校验。如需收紧权限，可在 {@link #doGet} 起始处补
 * {@link cube.console.container.Handlers#checkCookie} 判断。</p>
 */
public class HostInfoHandler extends ContextHandler {

    private final HostMonitor hostMonitor;

    public HostInfoHandler() {
        super("/host");
        // 接口挂在上下文根路径上时允许直接访问 /host ，避免 Jetty 302 补尾斜杠
        this.setAllowNullPathInfo(true);
        this.setHandler(new Handler());

        this.hostMonitor = HostMonitor.getInstance();
    }

    protected class Handler extends CrossDomainHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            // ContextHandler 已剥掉上下文路径，这里 target 是 "/static" / "/metrics"
            if (target.equals("/static")) {
                respondOk(response, hostMonitor.getStaticInfo());
            }
            else if (target.equals("/metrics")) {
                respondOk(response, hostMonitor.getMetrics());
            }
            else {
                respond(response, HttpStatus.NOT_FOUND_404, makeError(HttpStatus.NOT_FOUND_404));
            }

            complete();
        }
    }
}
