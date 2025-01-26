/*
 * This file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

public class IPEntry {

    private final String ip;
    private String mainInfo = "";
    private String subInfo = "";

    public IPEntry(final String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public String getMainInfo() {
        return mainInfo;
    }

    public String getSubInfo() {
        return subInfo;
    }

    public void setMainInfo(final String info) {
        this.mainInfo = info;
    }

    public void setSubInfo(final String info) {
        this.subInfo = info;
    }

    @Override
    public String toString() {
        return new StringBuilder(mainInfo).append(subInfo).toString();
    }
}
