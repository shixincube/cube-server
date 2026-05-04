/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cube.common.Language;
import cube.util.lunar.LunarCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间操作相关的辅助函数库。
 */
public final class TimeUtils {

    private final static long TD_SECOND = 1000L;

    private final static long TD_MINUTE = 60 * TD_SECOND;

    private final static long TD_HOUR = 60 * TD_MINUTE;

    private final static long TD_DAY = 24 * TD_HOUR;

    private final static SimpleDateFormat gsDateFormatPathSymbol = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private final static SimpleDateFormat sDateFormatForChinese =
            new SimpleDateFormat("yyyy年MM月dd日，HH时mm分ss秒", Locale.CHINESE);

    private final static SimpleDateFormat sDateFormatForEnglish =
            new SimpleDateFormat("MMMM d, yyyy, hh:mm:ss", Locale.ENGLISH);

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

    /**
     * 格式化输出今天日期的全部信息。<br/>
     * 例如：2025年8月24日，星期日，农历二〇二五年七月初二 乙巳年 蛇
     *
     * @return
     */
    public static String formatTodayFullDate() {
        Calendar today = Calendar.getInstance();
        LunarCalendar lunar = LunarCalendar.solar2Lunar(today);

        StringBuilder result = new StringBuilder();
        result.append(today.get(Calendar.YEAR)).append("年");
        result.append(today.get(Calendar.MONTH) + 1).append("月");
        result.append(today.get(Calendar.DATE)).append("日，");
        result.append(convertDayOfWeek(today.get(Calendar.DAY_OF_WEEK))).append("，");
        result.append("农历");
        result.append(lunar.getFullLunarName());
        return result.toString();
    }

    public static String convertDayOfWeek(int value) {
        switch (value) {
            case Calendar.SUNDAY:
                return "星期日";
            case Calendar.MONDAY:
                return "星期一";
            case Calendar.TUESDAY:
                return "星期二";
            case Calendar.WEDNESDAY:
                return "星期三";
            case Calendar.THURSDAY:
                return "星期四";
            case Calendar.FRIDAY:
                return "星期五";
            case Calendar.SATURDAY:
                return "星期六";
            default:
                return "";
        }
    }

    public static String formatDateString(long time, Language language) {
        Date date = new Date();
        date.setTime(time);
        if (language.isChinese()) {
            return sDateFormatForChinese.format(date);
        }
        else {
            return sDateFormatForEnglish.format(date);
        }
    }

    public static String formatDateYMD(long time) {
        Date date = new Date();
        date.setTime(time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE);
        return format.format(date);
    }

    public static String formatDateYMDH(long time) {
        Date date = new Date();
        date.setTime(time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日，HH时", Locale.CHINESE);
        return format.format(date);
    }

    /**
     * 提取文本里的日期数据。
     *
     * @param text
     * @return
     */
    public static Date extractDate(String text) {
        int year = 0;
        int month = 0;
        int date = 0;
        int hour = 0;

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() <= 2) {
                continue;
            }

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < line.length(); ++i) {
                String word = line.substring(i, i + 1).trim();
                if (word.length() == 0) {
                    continue;
                }

                if (TextUtils.isNumeric(word)) {
                    buf.append(word);
                    continue;
                }

                if (i == 0) {
                    continue;
                }

                try {
                    if (buf.length() > 0) {
                        if (word.equals("年")) {
                            year = Integer.parseInt(buf.toString());
                            buf.delete(0, buf.length());
                        }
                        else if (word.equals("月")) {
                            month = Integer.parseInt(buf.toString());
                            buf.delete(0, buf.length());
                        }
                        else if (word.equals("日") || word.equals("号")) {
                            date = Integer.parseInt(buf.toString());
                            buf.delete(0, buf.length());
                        }
                        else if (word.equals("时") || word.equals("点")) {
                            hour = Integer.parseInt(buf.toString());
                            buf.delete(0, buf.length());
                        }
                        else {
                            buf.delete(0, buf.length());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (date == 0) {
            // 没有日期数据
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        if (0 == year) {
            year = calendar.get(Calendar.YEAR);
        }
        if (0 == month) {
            month = calendar.get(Calendar.MONTH) + 1;
        }

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DATE, date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
