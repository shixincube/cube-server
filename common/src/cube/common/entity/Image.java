/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.FileType;

/**
 * 图像实体。
 */
public class Image {

    public final FileType type;

    public final int width;

    public final int height;

    public Image(FileType type, int width, int height) {
        this.type = type;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return this.type.getPreferredExtension() + " " + this.width + "x" + this.height;
    }
}
