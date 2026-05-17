/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.indicator;

public enum PIRIndicator implements Indicable {

    RainIntensity("雨势", "RainIntensity", 10),

    RainShelterEffectiveness("避雨有效性", "RainShelterEffectiveness", 9),

    RainShelteringMethod("避雨方式", "RainShelteringMethod", 8),

    PersonDetail("人物细节", "PersonDetails", 5),

    ;

    final String name;

    final String code;

    final int priority;

    PIRIndicator(String name, String code, int priority) {
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
}
