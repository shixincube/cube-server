/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

/**
 * 算法版本号。
 */
public class AlgorithmVersion {

    public final static int MAJOR = 0;

    public final static int MINOR = 77;

    public final static int REVISION = 0;

    private AlgorithmVersion() {
    }

    public static String toVersionString() {
        StringBuilder buf = new StringBuilder();
        buf.append(MAJOR);
        buf.append(".");
        buf.append(MINOR);
        buf.append(".");
        buf.append(REVISION);
        return buf.toString();
    }
}
