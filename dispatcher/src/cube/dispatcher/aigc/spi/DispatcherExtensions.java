/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.spi;

import cell.util.log.Logger;
import cube.util.ConfigUtils;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 业务模块扩展发现器。
 *
 * <p><b>为何用显式配置而非 {@code ServiceLoader}</b>：本工程是 ant + 手写启动脚本，
 * classpath 顺序不确定；且模块启停需要运维开关（心理学是付费域，巡检是实验室域），
 * 配置文件天然承载。风格与 {@code config/aigc-modules.properties} 一致。</p>
 *
 * <p>配置项：{@code module.extensions}，逗号分隔的实现类全限定名。
 * 未配置或为空时返回空列表，此时不注册任何扩展。</p>
 *
 * <p><b>本侧只持有接口</b>：实现类由各业务模块提供（编译期反向依赖本模块），
 * 本类仅按类名反射装载，运行期不引用任何实现包中的类型。</p>
 *
 * <p><b>前缀冲突策略</b>：两个扩展声明同一前缀时，<b>先注册者保留</b>，
 * 后者记 ERROR 并跳过，绝不覆盖——否则一个模块的接入会静默改变另一模块的端点归属。
 *
 * <p><b>端点路径冲突策略</b>：同上，按 {@link ContextHandler#getContextPath()}
 * 逐条比对，先注册者保留。Jetty 的 {@code ContextHandlerCollection}
 * 对同一路径是<b>后者覆盖前者</b>，不做检查会让后一个模块静默夺走前者的端点。</p>
 */
public final class DispatcherExtensions {

    /**
     * 配置项名称。
     */
    public final static String CONFIG_EXTENSIONS = "module.extensions";

    private DispatcherExtensions() {
    }

    /**
     * 发现并实例化全部扩展。
     *
     * @return 返回扩展列表；未配置或全部实例化失败时返回空列表。
     */
    public static List<DispatcherExtension> load() {
        List<DispatcherExtension> extensions = new ArrayList<>();

        File file = new File("config/dispatcher.properties");
        if (!file.exists()) {
            file = new File("dispatcher.properties");
        }

        if (!file.exists()) {
            Logger.i(DispatcherExtensions.class, "#load - Config file NOT found, no extension is loaded");
            return extensions;
        }

        Properties properties;
        try {
            properties = ConfigUtils.readProperties(file.getAbsolutePath());
        } catch (Exception e) {
            Logger.e(DispatcherExtensions.class, "#load - Read config failed: " + file.getAbsolutePath(), e);
            return extensions;
        }

        String value = properties.getProperty(CONFIG_EXTENSIONS, "").trim();
        if (value.isEmpty()) {
            Logger.i(DispatcherExtensions.class, "#load - \"" + CONFIG_EXTENSIONS + "\" is empty,"
                    + " no extension is loaded");
            return extensions;
        }

        for (String className : value.split(",")) {
            String name = className.trim();
            if (name.isEmpty()) {
                continue;
            }

            DispatcherExtension extension = instantiate(name);
            if (null != extension) {
                extensions.add(extension);
            }
        }

        return extensions;
    }

    /**
     * 收集全部扩展声明的前缀，并检测冲突。
     *
     * @param extensions 扩展列表。
     * @return 返回去重后的前缀集合。
     */
    public static Set<String> collectPrefixes(List<DispatcherExtension> extensions) {
        Set<String> prefixes = new HashSet<>();

        for (DispatcherExtension extension : extensions) {
            List<String> declared = extension.getRestPrefixes();
            if (null == declared) {
                continue;
            }

            for (String prefix : declared) {
                if (null == prefix || prefix.trim().isEmpty()) {
                    continue;
                }

                String normalized = normalize(prefix.trim());
                if (prefixes.add(normalized)) {
                    Logger.i(DispatcherExtensions.class, "#collectPrefixes - Extension \""
                            + extension.getName() + "\" declares prefix: " + normalized);
                }
                else {
                    // 先注册者保留，不覆盖
                    Logger.e(DispatcherExtensions.class, "#collectPrefixes - Prefix \"" + normalized
                            + "\" is ALREADY declared by another extension, IGNORED \""
                            + extension.getName() + "\"");
                }
            }
        }

        return prefixes;
    }

    /**
     * 注册全部扩展提供的 REST 端点。
     *
     * <p>按扩展装载顺序、扩展内迭代顺序注册；路径重复时<b>先注册者保留</b>。
     * 宿主自身的端点应在本方法调用<b>之前</b>注册完毕，使宿主端点天然优先，
     * 无需任何模块侧配合。</p>
     *
     * <p>单个扩展提供端点时抛异常只记 ERROR，不影响其余扩展——
     * 一个模块的接入问题不应让网关起不来。</p>
     *
     * @param extensions 扩展列表。
     * @param httpServer 上下文处理器收集器。
     */
    public static void registerEndpointHandlers(List<DispatcherExtension> extensions,
            cube.util.HttpServer httpServer) {
        if (null == extensions || extensions.isEmpty() || null == httpServer) {
            return;
        }

        // 宿主已注册的路径：先到先得
        Set<String> paths = new HashSet<>();
        for (ContextHandler handler : httpServer.getContextHandlers()) {
            addPath(paths, handler);
        }

        int total = 0;
        for (DispatcherExtension extension : extensions) {
            List<ContextHandler> handlers;
            try {
                handlers = extension.getEndpointHandlers();
            } catch (Throwable t) {
                Logger.e(DispatcherExtensions.class, "#registerEndpointHandlers - Extension \""
                        + extension.getName() + "\" FAILED on providing endpoints",
                        (t instanceof Exception) ? (Exception) t : null);
                continue;
            }

            if (null == handlers || handlers.isEmpty()) {
                continue;
            }

            int count = 0;
            for (ContextHandler handler : handlers) {
                if (null == handler) {
                    continue;
                }

                // getContextPath 可为 null（挂在根路径），此时不做去重
                String path = handler.getContextPath();
                if (null != path && !paths.add(path)) {
                    Logger.e(DispatcherExtensions.class, "#registerEndpointHandlers - Path \"" + path
                            + "\" is ALREADY registered, IGNORED \"" + extension.getName() + "\"");
                    continue;
                }

                httpServer.addContextHandler(handler);
                ++count;
                ++total;
            }

            Logger.i(DispatcherExtensions.class, "#registerEndpointHandlers - Extension \""
                    + extension.getName() + "\" registered " + count + " endpoint(s)");
        }

        if (total > 0) {
            Logger.i(DispatcherExtensions.class, "#registerEndpointHandlers - "
                    + "Module endpoints total: " + total);
        }
    }

    /**
     * 实例化单个扩展。
     *
     * @param className 实现类全限定名。
     * @return 返回实例；失败时返回 {@code null}。
     */
    private static DispatcherExtension instantiate(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            if (!(instance instanceof DispatcherExtension)) {
                Logger.e(DispatcherExtensions.class, "#instantiate - \"" + className
                        + "\" is NOT a DispatcherExtension");
                return null;
            }

            Logger.i(DispatcherExtensions.class, "#instantiate - Loaded extension: " + className);
            return (DispatcherExtension) instance;
        } catch (Throwable t) {
            // 捕获 Throwable：扩展由模块侧维护，类缺失等 Error 不应让网关启动失败
            Logger.e(DispatcherExtensions.class, "#instantiate - FAILED: " + className,
                    (t instanceof Exception) ? (Exception) t : null);
            return null;
        }
    }

    /**
     * 登记一个处理器占用的路径。
     *
     * @param paths 已登记的路径集合。
     * @param handler 处理器。
     */
    private static void addPath(Set<String> paths, ContextHandler handler) {
        if (null != handler && null != handler.getContextPath()) {
            paths.add(handler.getContextPath());
        }
    }

    /**
     * 归一化前缀：去掉尾斜杠。
     *
     * @param prefix 原始前缀。
     * @return 返回归一化后的前缀。
     */
    private static String normalize(String prefix) {
        String normalized = prefix;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 获取只读的空列表，供调用方兜底。
     *
     * @return 返回空列表。
     */
    public static List<ContextHandler> none() {
        return Collections.emptyList();
    }
}