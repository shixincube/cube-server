/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.util.log.Logger;
import cube.aigc.spi.DispatcherExtension;
import cube.util.ConfigUtils;

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
 * <p><b>前缀冲突策略</b>：两个扩展声明同一前缀时，<b>先注册者保留</b>，
 * 后者记ERROR 并跳过，绝不覆盖——否则一个模块的接入会静默改变另一模块的端点归属。</p>
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
    public static List<DispatcherExtension> none() {
        return Collections.emptyList();
    }
}
