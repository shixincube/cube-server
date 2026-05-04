/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.entity;

public class MutableGeneratingRecord {

    private GeneratingRecord value;

    public MutableGeneratingRecord() {
    }

    public MutableGeneratingRecord(GeneratingRecord value) {
        this.value = value;
    }

    public void setValue(GeneratingRecord value) {
        this.value = value;
    }

    public GeneratingRecord getValue() {
        return this.value;
    }

    public boolean isNull() {
        return (null == this.value);
    }

    public boolean isNotNull() {
        return (null != this.value);
    }
}
