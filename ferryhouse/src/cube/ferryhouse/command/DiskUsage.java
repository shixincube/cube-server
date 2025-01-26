/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse.command;

import cube.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 磁盘使用情况命令。
 */
public class DiskUsage extends Command {

    private List<String> output;

    private int total = 0;

    private String totalUnit;

    private int used = 0;

    private String usedUnit;

    private int avail = 0;

    private String availUnit;

    public DiskUsage() {
        super();
        this.output = new ArrayList<>();
    }

    public boolean execute() {
        List<String> params = new ArrayList<>();
        params.add("-h");
        return this.execute("df", params, this.output);
    }

    public int getTotal() {
        if (0 == this.total) {
            this.parse();
        }
        return this.total;
    }

    public String getTotalUnit() {
        return this.totalUnit;
    }

    public long getTotalInBytes() {
        long size = this.getTotal();
        if (0 == size) {
            return 0;
        }

        String unit = this.getTotalUnit();
        if (unit.equalsIgnoreCase("G")) {
            size *= FileUtils.GB;
        }
        else if (unit.equalsIgnoreCase("T")) {
            size *= FileUtils.TB;
        }
        return size;
    }

    public int getUsed() {
        if (0 == this.used) {
            this.parse();
        }
        return this.used;
    }

    public String getUsedUnit() {
        return this.usedUnit;
    }

    public long getUsedInBytes() {
        long size = this.getUsed();
        if (0 == size) {
            return 0;
        }

        String unit = this.getUsedUnit();
        if (unit.equalsIgnoreCase("K")) {
            size *= FileUtils.KB;
        }
        else if (unit.equalsIgnoreCase("M")) {
            size *= FileUtils.MB;
        }
        else if (unit.equalsIgnoreCase("G")) {
            size *= FileUtils.GB;
        }
        else if (unit.equalsIgnoreCase("T")) {
            size *= FileUtils.TB;
        }
        return size;
    }

    public int getAvail() {
        if (0 == this.avail) {
            this.parse();
        }
        return this.avail;
    }

    public String getAvailUnit() {
        return this.availUnit;
    }

    public long getAvailInBytes() {
        long size = this.getAvail();
        if (0 == size) {
            return 0;
        }

        String unit = this.getAvailUnit();
        if (unit.equalsIgnoreCase("K")) {
            size *= FileUtils.KB;
        }
        else if (unit.equalsIgnoreCase("M")) {
            size *= FileUtils.MB;
        }
        else if (unit.equalsIgnoreCase("G")) {
            size *= FileUtils.GB;
        }
        else if (unit.equalsIgnoreCase("T")) {
            size *= FileUtils.TB;
        }

        return size;
    }

    private void parse() {
        List<String> segmentList = new ArrayList<>();
        for (String line : this.output) {
            System.out.println("L: " + line);
            segmentList.clear();

            String[] segments = line.split(" ");
            boolean rootMounted = false;

            for (String segment : segments) {
                if (segment.length() == 0) {
                    continue;
                }

                segmentList.add(segment);
                if (segment.trim().equals("/")) {
                    rootMounted = true;
                    break;
                }
            }

            if (rootMounted) {
                // 总空间
                String size = segmentList.get(1);
                this.totalUnit = "G";
                int index = size.indexOf(this.totalUnit);
                if (index < 0) {
                    this.totalUnit = "T";
                    index = size.indexOf(this.totalUnit);
                    if (index < 0) {
                        // 无单位描述
                        this.totalUnit = "B";
                        index = size.length();
                    }
                }
                this.total = Integer.parseInt(size.substring(0, index));

                // 已使用
                size = segmentList.get(2);
                this.usedUnit = "K";
                index = size.indexOf(this.usedUnit);
                if (index < 0) {
                    this.usedUnit = "M";
                    index = size.indexOf(this.usedUnit);
                    if (index < 0) {
                        this.usedUnit = "G";
                        index = size.indexOf(this.usedUnit);
                        if (index < 0) {
                            this.usedUnit = "T";
                            index = size.indexOf(this.usedUnit);
                            if (index < 0) {
                                // 无单位描述
                                this.usedUnit = "B";
                                index = size.length();
                            }
                        }
                    }
                }
                this.used = Integer.parseInt(size.substring(0, index));

                // 可用
                size = segmentList.get(3);
                this.availUnit = "K";
                index = size.indexOf(this.availUnit);
                if (index < 0) {
                    this.availUnit = "M";
                    index = size.indexOf(this.availUnit);
                    if (index < 0) {
                        this.availUnit = "G";
                        index = size.indexOf(this.availUnit);
                        if (index < 0) {
                            this.availUnit = "T";
                            index = size.indexOf(this.availUnit);
                            if (index < 0) {
                                // 无单位描述
                                this.availUnit = "B";
                                index = size.length();
                            }
                        }
                    }
                }
                this.avail = Integer.parseInt(size.substring(0, index));

                break;
            }
        }
    }
}
