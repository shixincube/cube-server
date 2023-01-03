/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.util;

/**
 * 辅助测试的断言类。
 */
public final class Assert {

    private Assert() {
    }

    public static void equals(String expectedValue, String actualValue) {
        if (expectedValue.equals(actualValue)) {
            System.out.println("Assert OK - " + expectedValue);
            return;
        }

        System.err.println("Assert Failed : " + expectedValue + " != " + actualValue);
    }

    public static void equals(Long expectedValue, Long actualValue) {
        if (expectedValue.longValue() == actualValue.longValue()) {
            System.out.println("Assert OK - " + expectedValue);
            return;
        }

        System.err.println("Assert Failed : " + expectedValue + " != " + actualValue);
    }

    public static void equals(String tag, int expectedValue, int actualValue) {
        if (expectedValue == actualValue) {
            if (null != tag) {
                System.out.println(tag + " - Assert OK - " + expectedValue);
            }
            else {
                System.out.println("Assert OK - " + expectedValue);
            }
            return;
        }

        if (null != tag) {
            System.err.println(tag + " - Assert Failed : " + expectedValue + " != " + actualValue);
        }
        else {
            System.err.println("Assert Failed : " + expectedValue + " != " + actualValue);
        }
    }

    public static void equals(int expectedValue, int actualValue) {
        Assert.equals(null, expectedValue, actualValue);
    }

    public static void equals(String tag, boolean expectedValue, boolean actualValue) {
        if (expectedValue == actualValue) {
            if (null != tag) {
                System.out.println(tag + " - Assert OK - " + expectedValue);
            }
            else {
                System.out.println("Assert OK - " + expectedValue);
            }
            return;
        }

        if (null != tag) {
            System.err.println(tag + " - Assert Failed : " + expectedValue + " != " + actualValue);
        }
        else {
            System.err.println("Assert Failed : " + expectedValue + " != " + actualValue);
        }
    }

    public static void equals(boolean expectedValue, boolean actualValue) {
        Assert.equals(null, expectedValue, actualValue);
    }
}
