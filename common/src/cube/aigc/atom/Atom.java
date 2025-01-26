/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    public int value;

    public int currentLabelMatchingNum;

    public Atom(long sn, String label, String year, String month, String date, int value) {
        this.sn = sn;
        this.label = label;
        this.year = year;
        this.month = month;
        this.date = date;
        this.value = value;
    }

    public Atom(JSONObject json) {
        this.sn = 0;
        this.label = json.getString("label");
        this.year = json.getString("year");
        this.month = json.getString("month");
        this.date = json.getString("date");
        this.value = json.has("value") ? json.getInt("value") : 0;
    }

    public int getYear() {
        try {
            return Integer.parseInt(this.year.replace("年", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getMonth() {
        try {
            return Integer.parseInt(this.month.replace("月", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getDate() {
        try {
            return Integer.parseInt(this.date.replace("日", "").replace("号", ""));
        } catch (Exception e) {
            return 0;
        }
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
    public boolean equals(Object obj) {
        if (obj instanceof Atom) {
            Atom other = (Atom) obj;
            if (other.label.equals(this.label)) {
                boolean year = false;
                if (null != other.year && null != this.year
                    && other.year.equals(this.year)) {
                    year = true;
                }
                else if (null == other.year && null == this.year) {
                    year = true;
                }

                boolean month = false;
                if (null != other.month && null != this.month
                    && other.month.equals(this.month)) {
                    month = true;
                }
                else if (null == other.month && null == this.month) {
                    month = true;
                }

                boolean date = false;
                if (null != other.date && null != this.date
                    && other.date.equals(this.date)) {
                    date = true;
                }
                else if (null == other.date && null == this.date) {
                    date = true;
                }

                return year && month && date;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int code = this.label.hashCode();
        if (null != this.year) {
            code += this.year.hashCode() * 7;
        }
        if (null != this.month) {
            code += this.month.hashCode() * 7;
        }
        if (null != this.date) {
            code += this.date.hashCode() * 7;
        }
        return code;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        json.put("value", this.value);
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
