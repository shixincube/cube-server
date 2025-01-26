/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

/**
 * 字段约束。
 */
public enum Constraint {

    /**
     * 对应 NOT NULL 。
     */
    NOT_NULL("NOT NULL"),

    /**
     * 对应 PRIMARY KEY 。
     */
    PRIMARY_KEY("PRIMARY KEY"),

    /**
     * 对应 AUTOINCREMENT 。
     */
    AUTOINCREMENT("AUTOINCREMENT"),

    /**
     * 对应 AUTO_INCREMENT 。
     */
    AUTO_INCREMENT("AUTO_INCREMENT"),

    /**
     * 对应 UNIQUE 。
     */
    UNIQUE("UNIQUE"),

    /**
     * 对应 DEFAULT 。
     */
    DEFAULT("DEFAULT"),

    /**
     * 对应 DEFAULT 0 。
     */
    DEFAULT_0("DEFAULT 0"),

    /**
     * 对应 DEFAULT 1 。
     */
    DEFAULT_1("DEFAULT 1"),

    /**
     * 对应 DEFAULT NULL 。
     */
    DEFAULT_NULL("DEFAULT NULL"),

    /**
     * 对应 CHECK 。
     */
    CHECK("CHECK");

    private String statement;

    Constraint(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return this.statement;
    }
}
