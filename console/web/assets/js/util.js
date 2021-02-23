/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

(function(global) {

    global.ui = {};

    function formatNumber(num, length) {
        if (length == 2) {
            if (num < 10) {
                return '0' + num;
            }
        }
        else if (length == 3) {
            if (num < 10) {
                return '00' + num;
            }
            else if (num < 100) {
                return '0' + num;
            }
        }
        else if (length == 4) {
            if (num < 10) {
                return '000' + num;
            }
            else if (num < 100) {
                return '00' + num;
            }
            else if (num < 1000) {
                return '0' + num;
            }
        }

        return '' + num;
    }
    global.ui.formatNumber = formatNumber;

    function formatTimeHHMM(time) {
        var date = new Date(time);
        return formatNumber(date.getHours(), 2) + ':' + formatNumber(date.getMinutes(), 2);
    }
    global.ui.formatTimeHHMM = formatTimeHHMM;

    function makeTimeLineArray(start, num) {
        var array = [];
        var time = start;
        for (var i = 0; i < num; ++i) {
            var date = new Date(time);
            array[i] = formatNumber(date.getHours(), 2) + ':' + formatNumber(date.getMinutes(), 2);
            time += 60000;
        }
        return array;
    }
    global.ui.makeTimeLineArray = makeTimeLineArray;
})(window);
