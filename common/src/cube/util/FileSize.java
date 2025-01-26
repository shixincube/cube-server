/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

/**
 * 文件大小描述。
 */
public class FileSize {

    private long size;

    private String value;

    private String unit;

    /**
     * 构造函数。
     *
     * @param size 文件大小。
     * @param value 数值。
     * @param unit 数值对应的单位。
     */
    public FileSize(long size, String value, String unit) {
        this.size = size;
        this.value = value;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return this.value + " " + this.unit;
    }
}
