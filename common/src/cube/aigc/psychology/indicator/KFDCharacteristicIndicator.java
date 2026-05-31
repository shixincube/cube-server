/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.indicator;

public enum KFDCharacteristicIndicator implements Indicable {

    /**
     * 高亲密。
     */
    HighIntimacy("高亲密", "HighIntimacy"),

    /**
     * 低亲密。
     */
    LowIntimacy("低亲密", "LowIntimacy"),

    /**
     * 高控制。
     */
    HighControl("高控制", "HighControl"),

    /**
     * 低控制。
     */
    LowControl("低控制", "LowControl"),

    /**
     * 高张力。
     */
    HighTension("高张力", "HighTension"),

    /**
     * 低张力。
     */
    LowTension("低张力", "LowTension"),

    ;

    private final String name;

    private final String code;

    private final int priority;

    KFDCharacteristicIndicator(String name, String code) {
        this.name = name;
        this.code = code;
        this.priority = 0;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }
}
