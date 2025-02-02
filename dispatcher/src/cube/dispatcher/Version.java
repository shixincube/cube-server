/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher;

/**
 * 版本信息。
 */
public final class Version {

    public final static int MAJOR = 3;

    public final static int MINOR = 0;

    public final static int REVISION = 94;

    private Version() {
    }

    /**
     * 转版本串。
     *
     * @return
     */
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
