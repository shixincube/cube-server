/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 单页应用路由回退。
 *
 * 控制台前端采用前端路由（history 模式），形如 {@code /dispatcher} 的地址在磁盘上
 * 并不存在对应文件。该句柄把这些「非静态、非接口」的 GET 请求统一回退到
 * {@code index.html}，交给前端路由解析。
 *
 * 注意：只有磁盘上确实不存在的路径才会被回退，因此不会抢占真实的静态资源。
 */
public class SpaFallbackHandler extends AbstractHandler {

    /** 该前缀及其子路径均属于接口，不参与回退 */
    private final static String[] RESERVED_PREFIXES = {
            "/signin",
            "/signout",
            "/servers",
            "/deploy",
            "/auth",
            "/statistic",
            "/host",
            "/log",
            "/server-report",
            "/report",
            "/stop"
    };

    /**
     * 仅子路径属于接口的前缀。
     *
     * 调度机与服务单元的接口分别挂在 {@code /dispatcher/xxx} 与 {@code /service/xxx}，
     * 而前端路由恰好占用了 {@code /dispatcher} 与 {@code /service} 两个裸路径，
     * 因此这里只把子路径当作接口，裸路径仍需回退给前端路由。
     */
    private final static String[] SUB_PATH_ONLY_PREFIXES = {
            "/dispatcher",
            "/service"
    };

    private final File resourceBase;

    private final File indexFile;

    private byte[] indexContent = null;

    private long indexLastModified = 0L;

    public SpaFallbackHandler(String resourceBase) {
        this.resourceBase = new File(resourceBase);
        this.indexFile = new File(this.resourceBase, "index.html");
    }

    /**
     * 判断路径是否属于单页应用的路由地址（而非静态资源或接口）。
     *
     * @param target 请求路径
     * @return 属于前端路由时返回 {@code true}
     */
    private static boolean isSpaRoute(String target) {
        if (null == target || target.length() <= 1 || !target.startsWith("/")) {
            return false;
        }

        for (String prefix : RESERVED_PREFIXES) {
            if (target.equals(prefix) || target.startsWith(prefix + "/")) {
                return false;
            }
        }

        for (String prefix : SUB_PATH_ONLY_PREFIXES) {
            if (target.startsWith(prefix + "/")) {
                return false;
            }
        }

        // 末级路径包含扩展名的一律视为静态资源
        int slash = target.lastIndexOf('/');
        String last = target.substring(slash + 1);
        return last.indexOf('.') < 0;
    }

    /**
     * 读取入口页面内容，文件发生变化时自动重新加载。
     *
     * @return 入口页面的字节内容，文件不存在时返回 {@code null}
     */
    private synchronized byte[] loadIndexContent() {
        if (!this.indexFile.exists() || !this.indexFile.isFile()) {
            return null;
        }

        long modified = this.indexFile.lastModified();
        if (null == this.indexContent || modified != this.indexLastModified) {
            try {
                this.indexContent = Files.readAllBytes(this.indexFile.toPath());
                this.indexLastModified = modified;
            }
            catch (IOException e) {
                return null;
            }
        }

        return this.indexContent;
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (baseRequest.isHandled()) {
            return;
        }

        String method = baseRequest.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return;
        }

        if (!isSpaRoute(target)) {
            return;
        }

        // 磁盘上真实存在的文件交给 ResourceHandler 处理
        if (new File(this.resourceBase, target).exists()) {
            return;
        }

        byte[] content = loadIndexContent();
        if (null == content) {
            return;
        }

        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(content.length);
        response.getOutputStream().write(content);

        baseRequest.setHandled(true);
    }
}
