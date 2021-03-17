/**
 * This source file is part of Cube.
 * https://shixincube.com
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

(function(g) {

    var KB = 1024;
    var MB = 1024 * KB;
    var GB = 1024 * MB;
    var TB = 1024 * GB;

    g.util = {};

    var chartColors = {
        red: 'rgb(255, 99, 132)',
        orange: 'rgb(255, 159, 64)',
        yellow: 'rgb(255, 205, 86)',
        green: 'rgb(75, 192, 192)',
        blue: 'rgb(54, 162, 235)',
        purple: 'rgb(153, 102, 255)',
        grey: 'rgb(201, 203, 207)'
    };
    g.util.chartColors = chartColors;

    /**
     * 按指定位数格式化数字。
     * @param {*} num 
     * @param {*} length 
     */
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
    g.util.formatNumber = formatNumber;

    function formatTimeHHMM(time) {
        var date = new Date(time);
        return formatNumber(date.getHours(), 2) + ':' + formatNumber(date.getMinutes(), 2);
    }
    g.util.formatTimeHHMM = formatTimeHHMM;

    function formatTimeHHMMSS(time) {
        var date = new Date(time);
        var format = [
            formatNumber(date.getHours(), 2),
            ':',
            formatNumber(date.getMinutes(), 2),
            ':',
            formatNumber(date.getSeconds(), 2)
        ];
        return format.join('');
    }
    g.util.formatTimeHHMMSS = formatTimeHHMMSS;

    function formatTimeMDHMS(time) {
        var date = new Date(time);
        var format = [
            formatNumber(date.getMonth() + 1, 2),
            '-',
            formatNumber(date.getDate(), 2),
            ' ',
            formatNumber(date.getHours(), 2),
            ':',
            formatNumber(date.getMinutes(), 2),
            ':',
            formatNumber(date.getSeconds(), 2)
        ];
        return format.join('');
    }
    g.util.formatTimeMDHMS = formatTimeMDHMS;

    function formatFullTime(time) {
        var date = new Date(time);
        var format = [
            date.getFullYear(),
            '-',
            formatNumber(date.getMonth() + 1, 2),
            '-',
            formatNumber(date.getDate(), 2),
            ' ',
            formatNumber(date.getHours(), 2),
            ':',
            formatNumber(date.getMinutes(), 2),
            ':',
            formatNumber(date.getSeconds(), 2)
        ];
        return format.join('');
    }
    g.util.formatFullTime = formatFullTime;

    /**
     * 将以字节为单元的数据转为合适的千字节或兆字节等单位。
     * @param {number} size 
     * @returns 
     */
    function formatSize(size) {
        if (size < KB) {
            return size + ' B';
        }
        else if (size >= KB && size < MB) {
            return ((size / KB).toFixed(2)) + ' KB';
        }
        else if (size >= MB && size < GB) {
            return ((size / MB).toFixed(2)) + ' MB';
        }
        else if (size >= GB && size < TB) {
            return ((size / GB).toFixed(2)) + ' GB';
        }
        else {
            return size;
        }
    }
    g.util.formatSize = formatSize;

    function filterText(text) {
        var result = text.replace(/\n/g, '<br/>');
        result = result.replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;');
        return result;
    }
    g.util.filterText = filterText;

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
    g.util.makeTimeLineArray = makeTimeLineArray;

    /**
     * 判断是否是合法的 IPv4
     * @param {string} ip 
     * @returns 
     */
    g.util.isIPv4 = function(ip) {
        var reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
        return reg.test(ip);
    }

    /**
     * 判断是否是合法的 IPv6
     * @param {string} ip 
     * @returns 
     */
    g.util.isIPv6 = function(ip) {
        return /:/.test(ip) && ip.match(/:/g).length < 8 &&
            /::/.test(ip) ? (ip.match(/::/g).length == 1 && /^::$|^(::)?([\da-f]{1,4}(:|::))*[\da-f]{1,4}(:|::)?$/i.test(ip))
                : /^([\da-f]{1,4}:){7}[\da-f]{1,4}$/i.test(ip);
    }

    /**
     * 判断是否是无符号整数。
     * @param {string} str 
     * @returns 
     */
    g.util.isUnsigned = function(str) {
        return (/(^[1-9]\d*$)/.test(str));
    }

    /**
     * 毫秒换算为小时。
     * @param {number} ms 
     * @returns 
     */
    g.util.convMillisToHours = function(ms) {
        return ms / 3600000;
    }

    /**
     * 字节换算为兆字节。
     * @param {number} bytes 
     * @returns 
     */
    g.util.convBToMB = function(bytes) {
        return bytes / MB;
    }

    /**
     * 随机数字。
     * @param {*} min 
     * @param {*} max 
     * @returns 
     */
    g.util.randomNumber = function(min, max) {
        if (min === undefined) {
            min = -32768;
        }
        if (max === undefined) {
            max = 32767;
        }

        return Math.floor(Math.random() * (max - min)) + min;
    }
})(window);
