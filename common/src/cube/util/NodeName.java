/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import java.util.Set;

/**
 * 节点命名策略。
 *
 * 节点名统一为 <code>&lt;identity&gt;#&lt;role&gt;#&lt;port&gt;</code>，例如 <code>344e19c671bc9376#service#6000</code>：
 * <ul>
 *     <li><code>identity</code>：节点标识（默认由 {@link ConfigUtils#makeStableNodeId()} 生成，可用 <code>node.id</code> 显式指定）；</li>
 *     <li><code>role</code>：节点角色，取值 {@link #ROLE_DISPATCHER} 或 {@link #ROLE_SERVICE}；</li>
 *     <li><code>port</code>：节点服务的接入端口。</li>
 * </ul>
 *
 * 控制台侧由 <code>tag</code>（控制台自身的节点标识）+ 角色 + 端口推导出“期望名”，节点侧上报时携带自报名。
 * 两者前缀不一致（跨机部署、历史版本、接口集合变化）时，{@link #resolve(String, Set)} 提供以“角色 + 端口”
 * 后缀唯一命中为准的归一能力，避免监控数据静默丢失。
 */
public final class NodeName {

    /**
     * 调度机角色。
     */
    public final static String ROLE_DISPATCHER = "dispatcher";

    /**
     * 服务单元角色。
     */
    public final static String ROLE_SERVICE = "service";

    /**
     * 名字分隔符。
     */
    public final static String SEPARATOR = "#";

    /**
     * 显式指定节点标识的配置键。
     */
    public final static String KEY_NODE_ID = "node.id";

    /**
     * 进程内的节点标识缓存。
     */
    private static volatile String cachedIdentity = null;

    private NodeName() {
    }

    /**
     * 返回本机的节点标识（进程内缓存）。
     *
     * @return 节点标识。
     */
    public static String identity() {
        String id = cachedIdentity;
        if (null == id) {
            synchronized (NodeName.class) {
                id = cachedIdentity;
                if (null == id) {
                    id = ConfigUtils.makeStableNodeId();
                    cachedIdentity = id;
                }
            }
        }
        return id;
    }

    /**
     * 返回节点标识：显式配置优先，未配置时使用本机稳定标识。
     *
     * @param configured 配置值，可为 null 或空串。
     * @return 节点标识。
     */
    public static String identity(String configured) {
        if (null != configured) {
            String trimmed = configured.trim();
            if (trimmed.length() > 0) {
                return trimmed;
            }
        }
        return identity();
    }

    /**
     * 组合节点名。
     *
     * @param identity 节点标识。
     * @param role 角色。
     * @param port 端口。
     * @return 节点名。
     */
    public static String makeName(String identity, String role, int port) {
        StringBuilder sb = new StringBuilder();
        sb.append((null == identity) ? "" : identity);
        sb.append(SEPARATOR).append(role).append(SEPARATOR).append(port);
        return sb.toString();
    }

    /**
     * 提取节点名中的角色。
     *
     * @param name 节点名。
     * @return 角色，无法识别时返回 null。
     */
    public static String roleOf(String name) {
        String[] parts = split(name);
        return (null == parts) ? null : parts[1];
    }

    /**
     * 提取节点名中的端口。
     *
     * @param name 节点名。
     * @return 端口，无法识别时返回 -1。
     */
    public static int portOf(String name) {
        String[] parts = split(name);
        if (null == parts) {
            return -1;
        }

        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 提取节点名中“角色 + 端口”的后缀，例如 <code>#service#6000</code>。
     *
     * @param name 节点名。
     * @return 后缀，无法识别时返回 null。
     */
    public static String suffixOf(String name) {
        String[] parts = split(name);
        if (null == parts) {
            return null;
        }
        return SEPARATOR + parts[1] + SEPARATOR + parts[2];
    }

    /**
     * 将节点自报名归一为控制台的期望名。
     *
     * 规则：
     * <ol>
     *     <li>自报名已在期望名集合中 —— 精确命中，不需要归一；</li>
     *     <li>否则取“角色 + 端口”后缀，在期望名集合中查找，**唯一命中**时才归一；</li>
     *     <li>命中多个（同名后缀的节点不止一个）时拒绝猜测，保持原样并标记歧义。</li>
     * </ol>
     *
     * @param reporter 节点自报名。
     * @param expectedNames 控制台的期望名集合。
     * @return 解析结果。
     */
    public static Resolution resolve(String reporter, Set<String> expectedNames) {
        if (null == reporter || reporter.length() == 0) {
            return new Resolution(reporter, false, false);
        }

        if (null == expectedNames || expectedNames.isEmpty()) {
            return new Resolution(reporter, false, false);
        }

        // 精确命中
        if (expectedNames.contains(reporter)) {
            return new Resolution(reporter, false, false);
        }

        String suffix = suffixOf(reporter);
        if (null == suffix) {
            return new Resolution(reporter, false, false);
        }

        String matched = null;
        int count = 0;
        for (String name : expectedNames) {
            if (null != name && name.endsWith(suffix)) {
                matched = name;
                ++count;
                if (count > 1) {
                    break;
                }
            }
        }

        if (1 == count) {
            return new Resolution(matched, true, false);
        }

        return new Resolution(reporter, false, count > 1);
    }

    /**
     * 拆分节点名。
     *
     * @param name 节点名。
     * @return 长度为 3 的数组，格式不合法时返回 null。
     */
    private static String[] split(String name) {
        if (null == name) {
            return null;
        }

        int last = name.lastIndexOf(SEPARATOR);
        if (last <= 0 || last == name.length() - 1) {
            return null;
        }

        int prev = name.lastIndexOf(SEPARATOR, last - 1);
        if (prev < 0) {
            return null;
        }

        String role = name.substring(prev + 1, last);
        if (!ROLE_DISPATCHER.equals(role) && !ROLE_SERVICE.equals(role)) {
            return null;
        }

        String port = name.substring(last + 1);
        for (int i = 0, len = port.length(); i < len; ++i) {
            if (port.charAt(i) < '0' || port.charAt(i) > '9') {
                return null;
            }
        }

        return new String[]{ name.substring(0, prev), role, port };
    }

    /**
     * 归一结果。
     */
    public static final class Resolution {

        /**
         * 归一后的节点名。
         */
        public final String name;

        /**
         * 是否由“角色 + 端口”后缀归一而来。
         */
        public final boolean normalized;

        /**
         * 是否因为后缀命中多个期望名而无法归一。
         */
        public final boolean ambiguous;

        public Resolution(String name, boolean normalized, boolean ambiguous) {
            this.name = name;
            this.normalized = normalized;
            this.ambiguous = ambiguous;
        }
    }
}
