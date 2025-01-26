/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub;

/**
 * 产品类型枚举。
 */
public enum Product {

    WeChat("WeChat"),

    FeiShu("FeiShu"),

    DingDing("DingDing"),

    Unknown("Unknown")

    ;

    public final String name;

    Product(String name) {
        this.name = name;
    }

    public final static Product parse(String name) {
        for (Product app : Product.values()) {
            if (app.name.equalsIgnoreCase(name)) {
                return app;
            }
        }

        return Unknown;
    }
}
