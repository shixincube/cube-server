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

package cube.util;

/**
 * 时间操作相关的辅助函数库。
 */
public final class TimeUtils {

    private final static long TD_SECOND = 1000L;

    private final static long TD_MINUTE = 60 * TD_SECOND;

    private final static long TD_HOUR = 60 * TD_MINUTE;

    private final static long TD_DAY = 24 * TD_HOUR;

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
}
