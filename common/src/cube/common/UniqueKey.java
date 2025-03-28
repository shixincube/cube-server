/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

/**
 * 唯一键。
 */
public final class UniqueKey {

    private UniqueKey() {
    }

    /**
     * 生成唯一键。
     *
     * @param id 对象 ID 。
     * @param domainName 对象所在域。
     * @return 字符串形式的唯一键。
     */
    public static String make(Long id, String domainName) {
        return id.toString() + "_" + domainName;
    }

    /**
     * 生成唯一键。
     *
     * @param id 对象 ID 。
     * @param domain 对象所在域。
     * @return 字符串形式的唯一键。
     */
    public static String make(Long id, Domain domain) {
        return id.toString() + "_" + domain.getName();
    }

    /**
     * 提取 ID 数据。
     *
     * @param key 唯一键。
     * @return 返回 Key 里的 ID 值。如果提取失败，返回 {@code null} 值。
     */
    public static Long extractId(String key) {
        int index = key.indexOf("_");
        if (index > 0) {
            String idstr = key.substring(0, index);
            try {
                return Long.parseLong(idstr);
            } catch (Exception e) {
                // Nothing
            }
        }

        return null;
    }

    /**
     * 提取键包含的 ID 和域名称。
     *
     * @param key 唯一键。
     * @return 包含 ID 和域数组。索引 {@code 0} 是 ID ，索引 {code 1} 是域名称。
     */
    public static Object[] extract(String key) {
        int index = key.indexOf("_");
        if (index > 0) {
            Long id = null;
            String idStr = key.substring(0, index);
            try {
                id = Long.parseLong(idStr);
            } catch (Exception e) {
                return null;
            }

            String domain = key.substring(index + 1, key.length());

            return new Object[] { id, domain };
        }

        return null;
    }
}
