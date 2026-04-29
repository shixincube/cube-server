/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.indicator;

public enum KFDIndicator implements Indicable {
    /**
     * 猫咪家族。
     */
    CatFamily("猫咪家族", "CatFamily", 0),

    /**
     * 金毛犬家族。
     */
    GoldenRetrieverFamily("金毛犬家族", "GoldenRetrieverFamily", 0),

    /**
     * 大雁家族。
     */
    GooseFamily("大雁家族", "GooseFamily", 0),

    /**
     * 袋鼠家族。
     */
    KangarooFamily("袋鼠家族", "KangarooFamily", 0),

    /**
     * 麻雀家族。
     */
    SparrowFamily("麻雀家族", "SparrowFamily", 0),

    /**
     * 刺猬家族。
     */
    HedgehogFamily("刺猬家族", "HedgehogFamily", 0),

    /**
     * 老鹰家族。
     */
    EagleFamily("老鹰家族", "EagleFamily", 0),

    /**
     * 老虎家族。
     */
    TigerFamily("老虎家族", "TigerFamily", 0),

    /**
     * 未知。
     */
    Unknown("未知", "Unknown", 0)

    ;

    private final String name;

    private final String code;

    private final int priority;

    KFDIndicator(String name, String code, int priority) {
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

    public static int length() {
        return KFDIndicator.values().length - 1;
    }

    public static KFDIndicator parse(String nameOrCode) {
        for (KFDIndicator indicator : KFDIndicator.values()) {
            if (indicator.code.equalsIgnoreCase(nameOrCode) ||
                    indicator.name.equalsIgnoreCase(nameOrCode)) {
                return indicator;
            }
        }
        return KFDIndicator.Unknown;
    }
}
