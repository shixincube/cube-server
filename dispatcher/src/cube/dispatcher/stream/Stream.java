/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.stream;

public class Stream {

    public final String type;

    public final String name;

    public final int index;

    public final byte[] data;

    public final long timestamp = System.currentTimeMillis();

    public Stream(String type, String name, int index, byte[] data) {
        this.type = type;
        this.name = name;
        this.index = index;
        this.data = data;
    }

    public StreamType getType() {
        return StreamType.parse(this.type);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.type);
        buf.append("|");
        buf.append(this.name);
        buf.append("|");
        buf.append(this.index);
        buf.append("|");
        buf.append("(");
        buf.append(this.data.length);
        buf.append(")...");
        return buf.toString();
    }
}
