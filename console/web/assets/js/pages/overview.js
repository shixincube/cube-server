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

(function ($, g) {
    'use strict'

    var that = null;

    var console = new Console();
    $.console = console;

    // 检查是否合法
    console.checkUser(function(valid) {
        if (!valid) {
            window.location.href = 'index.html';
        }
        else {
            g.common.updateUserPanel(console);
            that.launch();
        }
    });

    g.overview = {
        launch: function() {
            this.bindUIEvent();

            console.getDomains(function(data) {
                if (null == data) {
                    console.log('获取域列表失败');
                    return;
                }

                that.updateDomainSelector(data.list);

                setTimeout(function() {
                    that.refresh($('#domain-selector').find('option:selected').text());
                }, 1);
            });
        },

        bindUIEvent: function() {
            // 选择域
            $('#domain-selector').change(function() {
                var value = $(this).children('option:selected').val();
                that.refresh(value);
            });
        },

        updateDomainSelector: function(list) {
            var el = $('#domain-selector');
            list.forEach(function(value, index) {
                var opt = document.createElement('option');
                opt.innerText = value;
                el.append(opt);
            });
        },

        refresh: function(domain) {
            console.getRecentStatistic(domain, function(data) {
                if (null == data) {
                    alert('加载域 "' + domain + '" 统计数据错误');
                    return;
                }

                $('#recent-tnu').text(data.statistic.TNU);

            });
        }
    };

    that = g.overview;

})(jQuery, window);
