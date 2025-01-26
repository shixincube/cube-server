/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

/**
 * 用于线程间异步传值。
 */
public class FileTypeMutable {

    public FileType value = FileType.UNKNOWN;

    public FileTypeMutable() {
    }

    public FileTypeMutable(FileType value) {
        this.value = value;
    }

    public FileType getValue() {
        synchronized (this) {
            return this.value;
        }
    }

    public void setValue(FileType value) {
        synchronized (this) {
            this.value = value;
        }
    }
}
