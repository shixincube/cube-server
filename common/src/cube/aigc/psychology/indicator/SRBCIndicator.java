/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.indicator;

/**
 * 指标。
 */
public enum SRBCIndicator implements Indicable {
    /**
     * 榕树型。
     */
    BanyanTree("榕树型", "BanyanTree", 9),

    /**
     * 橡树型。
     */
    OakTree("橡树型", "OakTree", 9),

    /**
     * 藤蔓型。
     */
    Vine("藤蔓型", "Vine", 9),

    /**
     * 向日葵型。
     */
    Sunflower("向日葵型", "Sunflower", 9),

    /**
     * 仙人掌型。
     */
    Cactus("仙人掌型", "Cactus", 9),

    /**
     * 竹子型。
     */
    Bamboo("竹子型", "Bamboo", 9),

    /**
     * 并蒂莲型。
     */
    TwinLotus("并蒂莲型", "TwinLotus", 9),

    /**
     * 蒲公英型。
     */
    Dandelion("蒲公英型", "Dandelion", 9),

    /**
     * 雪松型。
     */
    Cedar("雪松型", "Cedar", 9),

    /**
     * 未知。
     */
    Unknown("未知", "Unknown", 0)

    ;

    private final String name;

    private final String code;

    private final int priority;

    SRBCIndicator(String name, String code, int priority) {
        this.name = name;
        this.code = code;
        this.priority = priority;
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

    public final static int length() {
        return SRBCIndicator.values().length - 1;
    }

    public final static SRBCIndicator parse(String nameOrCode) {
        for (SRBCIndicator indicator : SRBCIndicator.values()) {
            if (indicator.code.equalsIgnoreCase(nameOrCode) ||
                    indicator.name.equalsIgnoreCase(nameOrCode)) {
                return indicator;
            }
        }
        return SRBCIndicator.Unknown;
    }
}
