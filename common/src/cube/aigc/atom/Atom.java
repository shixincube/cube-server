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

package cube.aigc.atom;

import cube.common.JSONable;
import org.json.JSONObject;

import java.util.List;

public class Atom implements JSONable {

    public final long sn;

    public String label;

    public String year;
    public String month;
    public String date;

    public int value1;
    public int value2;

    public int currentLabelMatchingNum;

    public Atom(long sn, String label, String year, String month, String date, int value1, int value2) {
        this.sn = sn;
        this.label = label;
        this.year = year;
        this.month = month;
        this.date = date;
        this.value1 = value1;
        this.value2 = value2;
    }

    public Atom(JSONObject json) {
        this.sn = 0;
        this.label = json.getString("label");
        this.year = json.getString("year");
        this.month = json.getString("month");
        this.date = json.getString("date");
        this.value1 = json.has("value") ? json.getInt("value") : json.getInt("value1");
        this.value2 = json.has("value2") ? json.getInt("value2") : 0;
    }

    public String serializeDate() {
        StringBuilder buf = new StringBuilder();
        if (null != this.year) {
            buf.append(this.year);
        }
        if (null != this.month) {
            buf.append(this.month);
        }
        if (null != this.date) {
            buf.append(this.date);
        }
        return buf.toString();
    }

    public String formatDate() {
        StringBuilder buf = new StringBuilder();
        if (null != this.year) {
            buf.append(this.year);
            if (!this.year.endsWith("年")) {
                buf.append("年");
            }
        }
        if (null != this.month) {
            buf.append(this.month);
            if (!this.month.endsWith("月")) {
                buf.append("月");
            }
        }
        if (null != this.date) {
            buf.append(this.date);
            if (!this.date.endsWith("日")) {
                buf.append("日");
            }
        }
        return buf.toString();
    }

    public String formatSimpleDate() {
        StringBuilder buf = new StringBuilder();
        if (null != this.year) {
            buf.append(this.year.replace("年", ""));
        }
        if (null != this.month) {
            buf.append("-");
            buf.append(this.month.replace("月", ""));
        }
        if (null != this.date) {
            buf.append("-");
            buf.append(this.date.replace("日", ""));
        }
        return buf.toString();
    }

    /**
     * 计算有多少个输入词与标签匹配。
     *
     * @param words
     * @return
     */
    public int numMatchingLabels(List<String> words) {
        int num = 0;
        String[] labels = this.label.split(",");

        for (String label : labels) {
            for (String word : words) {
                if (word.equals(label.trim())) {
                    ++num;
                }
            }
        }

        this.currentLabelMatchingNum = num;
        return num;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        json.put("value", this.value1);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public static boolean checkLabel(Atom atom) {
        String[] array = atom.label.split(",");
        if (array.length < 3) {
            return false;
        }

        return true;
    }
}
