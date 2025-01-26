/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

/**
 * 授权的被管理域。
 */
public final class Domain {

    private String name;

    /**
     * 构造函数。
     *
     * @param name 指定域名称。
     */
    public Domain(String name) {
        this.name = name;
    }

    /**
     * 获取域的名称。
     *
     * @return 返回域的名称。
     */
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object other) {
        if (null != other && other instanceof Domain) {
            if (((Domain)other).name.equals(this.name)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
