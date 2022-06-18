/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
        if (unit.equalsIgnoreCase("M")) {
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
                }
                this.total = Integer.parseInt(size.substring(0, index));

                // 已使用
                size = segmentList.get(2);
                this.usedUnit = "M";
                index = size.indexOf(this.usedUnit);
                if (index < 0) {
                    this.usedUnit = "G";
                    index = size.indexOf(this.usedUnit);
                    if (index < 0) {
                        this.usedUnit = "T";
                        index = size.indexOf(this.usedUnit);
                    }
                }
                this.used = Integer.parseInt(size.substring(0, index));

                break;
            }
        }
    }
}
