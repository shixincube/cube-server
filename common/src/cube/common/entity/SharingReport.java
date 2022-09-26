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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分享数据报告。
 */
public class SharingReport extends Entity {

    public final static String CountRecord = "CountRecord";

    public final static String TopCountRecord = "TopCountRecord";

    private String name;

    public int totalSharingTag = -1;

    public int totalEventView = -1;

    public int totalEventExtract = -1;

    public int totalEventShare = -1;

    public Map<String, SharingTag> sharingTagMap;

    public List<SharingTagPair> topViewList;

    public List<SharingTagPair> topExtractList;

    public SharingReport(String name) {
        super();
        this.name = name;
        this.sharingTagMap = new ConcurrentHashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void addTopViewRecords(Map<String, Integer> records, int topNum) {
        if (null == this.topViewList) {
            this.topViewList = new ArrayList<>();
        }

        for (Map.Entry<String, Integer> e : records.entrySet()) {
            SharingTagPair pair = new SharingTagPair(e.getKey(), e.getValue());
            this.topViewList.add(pair);
        }

        // 排序
        this.topViewList.sort(new Comparator<SharingTagPair>() {
            @Override
            public int compare(SharingTagPair stp1, SharingTagPair stp2) {
                return stp2.total - stp1.total;
            }
        });

        if (this.topViewList.size() > topNum) {
            this.topViewList = this.topViewList.subList(0, topNum - 1);
        }
    }

    public void addTopExtractRecords(Map<String, Integer> records, int topNum) {
        if (null == this.topExtractList) {
            this.topExtractList = new ArrayList<>();
        }

        for (Map.Entry<String, Integer> e : records.entrySet()) {
            SharingTagPair pair = new SharingTagPair(e.getKey(), e.getValue());
            this.topExtractList.add(pair);
        }

        // 排序
        this.topExtractList.sort(new Comparator<SharingTagPair>() {
            @Override
            public int compare(SharingTagPair stp1, SharingTagPair stp2) {
                return stp2.total - stp1.total;
            }
        });

        if (this.topExtractList.size() > topNum) {
            this.topExtractList = this.topExtractList.subList(0, topNum - 1);
        }
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

        if (null != other.topViewList) {
            this.topViewList = other.topViewList;
        }
        if (null != other.topExtractList) {
            this.topExtractList = other.topExtractList;
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

        if (null != this.topViewList) {
            JSONArray array = new JSONArray();
            for (SharingTagPair pair : this.topViewList) {
                array.put(pair);
            }
            json.put("topView", array);
        }

        if (null != this.topExtractList) {
            JSONArray array = new JSONArray();
            for (SharingTagPair pair : this.topExtractList) {
                array.put(pair);
            }
            json.put("topExtract", array);
        }

        return json;
    }

    protected class SharingTagPair {

        protected String sharingCode;

        protected int total;

        protected SharingTagPair(String sharingCode, int total) {
            this.sharingCode = sharingCode;
            this.total = total;
        }

        protected JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("code", this.sharingCode);
            json.put("total", this.total);
            return json;
        }
    }
}
