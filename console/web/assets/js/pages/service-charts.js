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
    'use strict';

    var avgRespTimeChart = null;

    function build() {
        var chartData = {
            labels: ['', '', '', '', '', '', '', '', '', ''],
            datasets: [{
                type: 'line',
                label: 'Task 1',
                borderColor: g.util.chartColors.red,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: 'Task 2',
                borderColor: g.util.chartColors.blue,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: 'Task 3',
                borderColor: g.util.chartColors.green,
                borderWidth: 2,
                fill: false,
                data: []
            }]
        };

        /*
        {
            type: 'bar',
            label: 'JVM',
            backgroundColor: g.util.chartColors.grey,
            borderColor: 'white',
            borderWidth: 2,
            data: []
        }
        */

        var chartOptions = {
            responsive: false,
            title: {
                display: true,
                text: '任务平均应答时间'
            },
            tooltips: {
                mode: 'index',
                intersect: true
            }
        };

        var ctx = $('#avg_resp_chart').get(0).getContext('2d');
        avgRespTimeChart = new Chart(ctx, {
            type: 'line',
            data: chartData,
            options: chartOptions
        });
    }

    g.service.charts = {
        build: function() {
            build();
        },

        updateChart: function(server) {
            // server.perfCache;
            // server.jvmCache;
        }
    };

})(jQuery, window);
