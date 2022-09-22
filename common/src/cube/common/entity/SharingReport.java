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

package cube.common.entity;

import org.json.JSONObject;

/**
 * 分享数据报告。
 */
public class SharingReport extends Entity {

    public final static String CountRecord = "CountRecord";

    private String name;

    public int totalSharingTag = -1;

    public int totalEventView = -1;

    public int totalEventExtract = -1;

    public int totalEventShare = -1;

    public SharingReport(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public SharingReport merge(SharingReport other) {
        if (other.totalSharingTag != -1) {
            this.totalSharingTag = other.totalSharingTag;
        }
        if (other.totalEventView != -1) {
            this.totalEventView = other.totalEventView;
        }
        if (other.totalEventExtract != -1) {
            this.totalEventExtract = other.totalEventExtract;
        }
        if (other.totalEventShare != -1) {
            this.totalEventShare = other.totalEventShare;
        }

        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("totalSharingTag", this.totalSharingTag);
        json.put("totalEventView", this.totalEventView);
        json.put("totalEventExtract", this.totalEventExtract);
        json.put("totalEventShare", this.totalEventShare);
        return json;
    }
}
