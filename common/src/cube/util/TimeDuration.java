/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * 时间长度计量。
 */
public class TimeDuration {

    public final int days;

    public final int hours;

    public final int minutes;

    public final int seconds;

    public final int milliseconds;

    public TimeDuration(int hours, int minutes, int seconds) {
        this(0, hours, minutes, seconds, 0);
    }

    public TimeDuration(int hours, int minutes, int seconds, int milliseconds) {
        this(0, hours, minutes, seconds, milliseconds);
    }

    public TimeDuration(int days, int hours, int minutes, int seconds, int milliseconds) {
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
    }

    public TimeDuration(JSONObject json) {
        this.days = json.getInt("days");
        this.hours = json.getInt("hours");
        this.minutes = json.getInt("minutes");
        this.seconds = json.getInt("seconds");
        this.milliseconds = json.getInt("milliseconds");
    }

    /**
     *
     * @param value
     * @param unit
     * @return
     * @see java.util.Calendar
     */
    public TimeDuration increment(int value, int unit) {
        int days = this.days;
        int hours = this.hours;
        int minutes = this.minutes;
        int seconds = this.seconds;
        int milliseconds = this.milliseconds;

        if (unit == Calendar.SECOND) {
            seconds += value;
        }
        else if (unit == Calendar.MINUTE) {
            minutes += value;
        }
        else if (unit == Calendar.HOUR) {
            hours += value;
        }
        else if (unit == Calendar.MILLISECOND) {
            milliseconds += value;
        }
        else if (unit == Calendar.DATE) {
            days += value;
        }

        if (milliseconds >= 1000) {
            int quotient = (int) Math.floor((double)milliseconds / 1000.0);
            seconds += quotient;
            milliseconds = milliseconds % 1000;
        }

        if (seconds >= 60) {
            int quotient = (int) Math.floor((double)seconds / 60.0);
            minutes += quotient;
            seconds = seconds % 60;
        }

        if (minutes >= 60) {
            int quotient = (int) Math.floor((double)minutes / 60.0);
            hours += quotient;
            minutes = minutes % 60;
        }

        if (hours >= 24) {
            int quotient = (int) Math.floor((double)hours / 24.0);
            days += quotient;
            hours = hours % 24;
        }

        return new TimeDuration(days, hours, minutes, seconds, milliseconds);
    }

    /**
     * 是否所有数据均为 0 值。
     *
     * @return
     */
    public boolean isZero() {
        return (this.days == 0 && this.hours == 0 && this.minutes == 0
                && this.seconds == 0 && this.milliseconds == 0);
    }

    public String formatHMS() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.hours < 10 ? "0" + this.hours : this.hours);
        buf.append(":");
        buf.append(this.minutes < 10 ? "0" + this.minutes : this.minutes);
        buf.append(":");
        buf.append(this.seconds < 10 ? "0" + this.seconds : this.seconds);
        return buf.toString();
    }

    public String formatHMSMs() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.hours < 10 ? "0" + this.hours : this.hours);
        buf.append(":");
        buf.append(this.minutes < 10 ? "0" + this.minutes : this.minutes);
        buf.append(":");
        buf.append(this.seconds < 10 ? "0" + this.seconds : this.seconds);
        buf.append(".");
        buf.append(this.milliseconds);
        return buf.toString();
    }

    public String formatMSMs() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.minutes < 10 ? "0" + this.minutes : this.minutes);
        buf.append(":");
        buf.append(this.seconds < 10 ? "0" + this.seconds : this.seconds);
        buf.append(".");
        buf.append(this.milliseconds);
        return buf.toString();
    }

    public String toHumanString() {
        StringBuilder buf = new StringBuilder();
        if (this.days > 0) {
            buf.append(this.days).append("天");
        }
        if (this.hours > 0) {
            buf.append(this.hours).append("时");
        }
        if (this.minutes > 0) {
            buf.append(this.minutes).append("分");
        }
        buf.append(this.seconds).append("秒");
        buf.append(this.milliseconds).append("毫秒");
        return buf.toString();
    }

    public String toHumanStringDHMS() {
        StringBuilder buf = new StringBuilder();
        if (this.days > 0) {
            buf.append(this.days).append("天");
        }
        if (this.hours > 0) {
            buf.append(this.hours).append("时");
        }
        if (this.minutes > 0) {
            buf.append(this.minutes).append("分");
        }
        buf.append(this.seconds).append("秒");
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.days).append("天 ");
        buf.append(this.hours).append("时 ");
        buf.append(this.minutes).append("分 ");
        buf.append(this.seconds).append("秒 ");
        buf.append(this.milliseconds).append("毫秒");
        return buf.toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("days", this.days);
        json.put("hours", this.hours);
        json.put("minutes", this.minutes);
        json.put("seconds", this.seconds);
        json.put("milliseconds", this.milliseconds);
        return json;
    }
}
