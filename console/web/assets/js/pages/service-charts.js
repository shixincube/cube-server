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
    var jvmChart = null;

    function build() {
        // 任务平均时间
        var chartData = {
            labels: ['', '', '', '', '', '', '', '', '', ''],
            datasets: [{
                type: 'line',
                label: '--',
                borderColor: g.util.chartColors.red,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: '--',
                borderColor: g.util.chartColors.purple,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: '--',
                borderColor: g.util.chartColors.orange,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: '--',
                borderColor: g.util.chartColors.yellow,
                borderWidth: 2,
                fill: false,
                data: []
            }, {
                type: 'line',
                label: '--',
                borderColor: g.util.chartColors.green,
                borderWidth: 2,
                fill: false,
                data: []
            }]
        };

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

        // JVM 内存信息
        chartData = {
            labels: ['', '', '', '', '', '', '', '', '', ''],
            datasets: [{
                type: 'bar',
                label: 'Total Memory',
                backgroundColor: g.util.chartColors.blue,
                borderColor: 'white',
                borderWidth: 2,
                data: []
            }, {
                type: 'bar',
                label: 'Free Memory',
                backgroundColor: g.util.chartColors.grey,
                borderColor: 'white',
                borderWidth: 2,
                data: []
            }]
        };

        chartOptions = {
            responsive: false,
            title: {
                display: true,
                text: 'JVM Memory'
            },
            tooltips: {
                mode: 'index',
                intersect: true
            }
        };

        ctx = $('#jvm_chart').get(0).getContext('2d');
        jvmChart = new Chart(ctx, {
            type: 'bar',
            data: chartData,
            options: chartOptions
        });
    }

    g.service.charts = {
        build: function() {
            build();
        },

        updateChart: function(server) {
            var labels = [];
            var dataLabels = [];
            var datasetsArray = [[], [], [], [], []];

            // 先计算平均应答时间最高的
            var perf = server.perf;
            var map = perf.benchmark.avgResponseTimeMap;
            var list = [];
            for (var cellet in map) {
                var celletMap = map[cellet];
                for (var action in celletMap) {
                    var actionAvg = celletMap[action];
                    actionAvg.service = cellet;
                    actionAvg.action = action;
                    list.push(actionAvg);
                }
            }
            // 从大到小倒序
            list.sort(function(a, b) {
                return b.value - a.value;
            });

            for (var i = 0; i < server.perfCache.length; ++i) {
                perf = server.perfCache[i];
                map = perf.benchmark.avgResponseTimeMap;

                for (var n = 0; n < list.length && n < datasetsArray.length; ++n) {
                    var v = list[n];
                    var avgValue = map[v.service][v.action];
                    if (undefined === avgValue) {
                        continue;
                    }

                    dataLabels.push(v.action);
                    datasetsArray[n].push(avgValue.value);
                }

                labels.push(g.util.formatTimeHHMMSS(perf.timestamp));
            }

            avgRespTimeChart.data.labels = labels;
            for (var i = 0; i < datasetsArray.length && i < dataLabels.length; ++i) {
                avgRespTimeChart.data.datasets[i].label = dataLabels[i];
                avgRespTimeChart.data.datasets[i].data = datasetsArray[i];
            }
            avgRespTimeChart.update();

            // JVM 数据
            labels = [];
            var totalDataset = [];
            var freeDataset = [];
            for (var i = 0; i < server.jvmCache.length; ++i) {
                var data = server.jvmCache[i];
                labels.push(g.util.formatTimeHHMMSS(data.timestamp));
                totalDataset.push(data.totalMemory);
                freeDataset.push(data.freeMemory);
            }
            jvmChart.data.labels = labels;
            jvmChart.data.datasets[0].data = totalDataset;
            jvmChart.data.datasets[1].data = freeDataset;
            jvmChart.update();
        }
    };

})(jQuery, window);
