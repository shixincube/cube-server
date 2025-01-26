/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

/**
 * Ferry House 的标识。
 */
public final class FerryHouseFlag {

    /**
     * 标准模式。
     */
    public final static int STANDARD = 1;

    /**
     * 允许虚拟模式。
     */
    public final static int ALLOW_VIRTUAL_MODE = 1 << 1;

    private FerryHouseFlag() {
    }

    public static boolean isStandard(int flag) {
        return (flag & STANDARD) != 0;
    }

    public static boolean isAllowVirtualMode(int flag) {
        return (flag & ALLOW_VIRTUAL_MODE) != 0;
    }
}
