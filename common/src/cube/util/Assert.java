/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
