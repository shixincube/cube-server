/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cell.util.log.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * 节点版本号读取工具。
 *
 * 版本号的唯一来源是各节点自己的 {@code Version} 类：调度机为 {@code cube.dispatcher.Version}，
 * 服务单元为 {@code cube.service.Version}，两者都是「MAJOR.MINOR.REVISION」形式的
 * {@code toVersionString()}。
 *
 * 这两个类没有任何运行时依赖，因此这里直接从**该节点部署目录里的 jar** 中取出并独立加载：
 * 多个已部署节点可能来自不同的构建版本，只有读各自部署目录里的 jar 才能反映真实版本；
 * 控制台自身的 classpath 只作为兜底（部署目录不可读时）。
 *
 * 加载时把父加载器显式置为 {@code null}（等价于只用引导类加载器），避免「控制台自身
 * classpath 上恰好也有同名类」导致读到控制台的版本；加载器用完即关，不长期占用 jar 文件句柄。
 */
public final class NodeVersion {

    /**
     * 调度机版本类名。
     */
    public final static String DISPATCHER = "cube.dispatcher.Version";

    /**
     * 服务单元版本类名。
     */
    public final static String SERVICE = "cube.service.Version";

    /**
     * 版本类上提供版本串的静态方法名。
     */
    private final static String VERSION_METHOD = "toVersionString";

    /**
     * 在部署目录下查找 jar 的相对目录（先 `libs/`，再部署根目录本身）。
     */
    private final static String[] LIB_DIRS = new String[] { "libs", "" };

    /**
     * 版本号缓存，键为 jar 的绝对路径。
     *
     * 列表接口每次序列化都会读一次版本，这里按 jar 的修改时间做缓存，
     * 避免反复打开几十个 jar 做条目扫描。
     */
    private final static Map<String, CachedVersion> cache = new ConcurrentHashMap<>();

    private NodeVersion() {
    }

    /**
     * 读取指定部署目录里的节点版本号。
     *
     * @param deployPath       节点的部署根目录，可以为 {@code null}。
     * @param versionClassName 版本类全名，取值 {@link #DISPATCHER} 或 {@link #SERVICE}。
     * @return 形如 `3.0.157` 的版本串；无法确定时返回 {@code null}。
     */
    public static String read(String deployPath, String versionClassName) {
        String version = readFromDeployPath(deployPath, versionClassName);
        if (null == version) {
            version = readFromClasspath(versionClassName);
        }
        return version;
    }

    private static String readFromDeployPath(String deployPath, String versionClassName) {
        if (null == deployPath || deployPath.trim().isEmpty()) {
            return null;
        }

        File jar = locateJar(deployPath, versionClassName);
        if (null == jar) {
            return null;
        }

        CachedVersion cached = cache.get(jar.getAbsolutePath());
        if (null != cached && cached.lastModified == jar.lastModified()) {
            return cached.version;
        }

        String version = readFromJar(jar, versionClassName);
        cache.put(jar.getAbsolutePath(), new CachedVersion(jar.lastModified(), version));
        return version;
    }

    /**
     * 在部署目录里定位包含版本类的 jar。
     *
     * 不按文件名匹配（`cube-service-*.jar` 有多个同级产物），而是直接判断 jar 里是否存在
     * 版本类的 class 条目，只有主产物会命中。
     */
    private static File locateJar(String deployPath, String versionClassName) {
        String entryName = versionClassName.replace('.', '/') + ".class";

        for (String libDir : LIB_DIRS) {
            File dir = libDir.isEmpty()
                    ? new File(deployPath)
                    : Paths.get(deployPath, libDir).toFile();
            if (!dir.isDirectory()) {
                continue;
            }

            File[] files = dir.listFiles();
            if (null == files) {
                continue;
            }
            // 排序让多次选择结果稳定
            Arrays.sort(files);

            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".jar")) {
                    continue;
                }
                if (containsEntry(file, entryName)) {
                    return file;
                }
            }
        }

        return null;
    }

    private static boolean containsEntry(File jar, String entryName) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar);
            return null != jarFile.getJarEntry(entryName);
        } catch (IOException e) {
            return false;
        } finally {
            if (null != jarFile) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
        }
    }

    private static String readFromJar(File jar, String versionClassName) {
        URLClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, null);
            Class<?> clazz = Class.forName(versionClassName, true, loader);
            // 父加载器为 null 时类必然由该加载器定义，这里再确认一次，防止读到控制台自身的类
            if (loader != clazz.getClassLoader()) {
                return null;
            }
            return invokeVersion(clazz);
        } catch (Throwable t) {
            // 版本读不到不影响列表展示，只记录一条告警
            Logger.w(NodeVersion.class,
                    "#readFromJar - read version from '" + jar.getName() + "' failed", t);
            return null;
        } finally {
            if (null != loader) {
                try {
                    loader.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
        }
    }

    private static String readFromClasspath(String versionClassName) {
        try {
            return invokeVersion(Class.forName(versionClassName));
        } catch (Throwable t) {
            return null;
        }
    }

    private static String invokeVersion(Class<?> clazz) throws ReflectiveOperationException {
        Method method = clazz.getMethod(VERSION_METHOD);
        Object value = method.invoke(null);
        if (value instanceof String) {
            String version = ((String) value).trim();
            return version.isEmpty() ? null : version;
        }
        return null;
    }

    /**
     * 缓存的版本号。
     */
    private static final class CachedVersion {

        final long lastModified;

        final String version;

        CachedVersion(long lastModified, String version) {
            this.lastModified = lastModified;
            this.version = version;
        }
    }
}
