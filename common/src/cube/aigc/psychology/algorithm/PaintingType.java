/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

public enum PaintingType {

    HouseTreePerson("HTP", "房树人"),

    PersonInTheRain("PIR", "雨中人"),

    PersonSwordShield("PSS", "人剑盾")

    ;

    public final String name;

    public final String displayName;

    PaintingType(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public static PaintingType parse(String name) {
        for (PaintingType type : PaintingType.values()) {
            if (type.name.equalsIgnoreCase(name) ||
                type.displayName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
