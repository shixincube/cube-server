/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间操作相关的辅助函数库。
 */
public final class TimeUtils {

    private final static long TD_SECOND = 1000L;

    private final static long TD_MINUTE = 60 * TD_SECOND;

    private final static long TD_HOUR = 60 * TD_MINUTE;

    private final static long TD_DAY = 24 * TD_HOUR;

    private final static SimpleDateFormat gsDateFormatPathSymbol = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private TimeUtils() {
    }

    public static TimeDuration calcTimeDuration(final long durationInMillisecond) {
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        int milliseconds = 0;

        long duration = durationInMillisecond;

        if (duration >= TD_DAY) {
            days = (int) Math.floor(duration / TD_DAY);
            duration -= (TD_DAY * days);
        }

        if (duration >= TD_HOUR) {
            hours = (int) Math.floor(duration / TD_HOUR);
            duration -= (TD_HOUR * hours);
        }

        if (duration >= TD_MINUTE) {
            minutes = (int) Math.floor(duration / TD_MINUTE);
            duration -= (TD_MINUTE * minutes);
        }

        if (duration >= TD_SECOND) {
            seconds = (int) Math.floor(duration / TD_SECOND);
            duration -= (TD_SECOND * seconds);
        }

        milliseconds = (int) duration;

        return new TimeDuration(days, hours, minutes, seconds, milliseconds);
    }

    /**
     * 格式化日期。
     *
     * @param timestamp
     * @return
     */
    public static String formatDateForPathSymbol(long timestamp) {
        return gsDateFormatPathSymbol.format(new Date(timestamp));
    }

    /**
     * 格式化时间转时间戳。
     *
     * @param dateString
     * @return
     */
    public static long unformatDate(String dateString) {
        String[] array = dateString.split(" ");
        if (array.length != 2) {
            array = dateString.split("_");
            if (array.length != 2) {
                return -1;
            }
        }

        String[] ymdArray = array[0].split("-");
        if (ymdArray.length != 3) {
            return -1;
        }

        String[] hmsArray = array[1].split(":");
        if (hmsArray.length != 3) {
            hmsArray = array[1].split("-");
            if (hmsArray.length != 3) {
                return -1;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(ymdArray[0]), Integer.parseInt(ymdArray[1]) - 1, Integer.parseInt(ymdArray[2]),
                Integer.parseInt(hmsArray[0]), Integer.parseInt(hmsArray[1]), Integer.parseInt(hmsArray[2]));
        return calendar.getTimeInMillis();
    }
}
