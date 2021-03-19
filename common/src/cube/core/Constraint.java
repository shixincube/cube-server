/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
