/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 工具箱动作。
 */
public enum ToolKitAction {

    /**
     * 生成条形码。
     */
    MakeBarCode("makeBarCode"),

    /**
     * 扫描条形码。
     */
    ScanBarCode("scanBarCode"),

    ;

    public final String name;

    ToolKitAction(String name) {
        this.name = name;
    }
}
