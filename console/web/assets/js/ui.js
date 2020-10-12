/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

    global.dashboard = {
        appendLog: function(el, line) {
            var content = [];

            var date = new Date(line.time);
            content.push(formatNumber(date.getMonth() + 1, 2));
            content.push('-');
            content.push(formatNumber(date.getDate(), 2));
            content.push(' ');
            content.push(formatNumber(date.getHours(), 2));
            content.push(':');
            content.push(formatNumber(date.getMinutes(), 2));
            content.push(':');
            content.push(formatNumber(date.getSeconds(), 2));
            content.push('.');
            content.push(formatNumber(date.getMilliseconds(), 3));
            content.push(' ');

            var p = document.createElement('p');

            if (line.level == 1) {
                // Debug
                content.push('[DEBUG] ');
                p.setAttribute('class', 'text-muted');
            }
            else if (line.level == 2) {
                // Info
                content.push('[INFO]  ');
                p.setAttribute('class', 'text-info');
            }
            else if (line.level == 3) {
                // Warning
                content.push('[WARN]  ');
                p.setAttribute('class', 'text-warning');
            }
            else if (line.level == 4) {
                // Error
                content.push('[ERROR] ');
                p.setAttribute('class', 'text-danger');
            }

            content.push(line.tag);
            content.push(' - ');
            content.push(line.text);

            p.innerText = content.join('');
            el.append(p);
        },

        removeLog: function(el, num) {
            var list = el.find('p');
            for (var i = 0; i < list.length && i < num; ++i) {
                var c = list[i];
                $(c).remove();
            }
        }
    }

})(window);
