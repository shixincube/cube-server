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

(function ($, g) {
    'use strict'

    var that = null;

    var dispatcherList = [];
    var serviceList = [];

    var console = new Console();
    $.console = console;

    // 检查是否合法
    console.checkUser(function(valid) {
        if (!valid) {
            window.location.href = 'index.html';
        }
        else {
            g.common.updateUserPanel(console);
            g.dashboard.launch();
        }
    });

    g.dashboard = {
        launch: function() {
            var dispatcherRunning = 0;
            var serviceRunning = 0;

            console.getDispatchers(function(list) {
                dispatcherList = list;

                dispatcherList.forEach(function(value) {
                    if (value.running) {
                        ++dispatcherRunning;
                    }
                });

                that.updateDispatcherBox(list.length, dispatcherRunning);
            });

            console.getServices(function(list) {
                serviceList = list;

                serviceList.forEach(function(value) {
                    if (value.running) {
                        ++serviceRunning;
                    }
                });

                that.updateServiceBox(list.length, serviceRunning);
            });
        },

        updateDispatcherBox: function(num, numRunning) {
            var el = $('#dispatcher-box');
            el.find('h3').text(num);
            el.find('.box-desc').find('b').text(numRunning);
        },

        updateServiceBox: function(num, numRunning) {
            var el = $('#service-box');
            el.find('h3').text(num);
            el.find('.box-desc').find('b').text(numRunning);
        },

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
        },

        buildJVMChartDataTemplate: function() {
            var labels = g.util.makeTimeLineArray(Date.now(), 8);
            var data = {
                labels : labels,
                datasets: [{
                    label               : 'Total Memory (MB)',
                    backgroundColor     : 'rgba(60,141,188, 0.9)',
                    borderColor         : 'rgba(60,141,188, 0.8)',
                    pointRadius          : false,
                    pointColor          : '#3b8bba',
                    pointStrokeColor    : 'rgba(60,141,188, 1)',
                    pointHighlightFill  : '#fff',
                    pointHighlightStroke: 'rgba(60,141,188, 1)',
                    data                : [0, 0, 0, 0, 0, 0, 0, 0]
                }, {
                    label               : 'Free Memory (MB)',
                    backgroundColor     : 'rgba(210,214,222, 1)',
                    borderColor         : 'rgba(210,214,222, 1)',
                    pointRadius         : false,
                    pointColor          : 'rgba(210,214,222, 1)',
                    pointStrokeColor    : '#c1c7d1',
                    pointHighlightFill  : '#fff',
                    pointHighlightStroke: 'rgba(220,220,220, 1)',
                    data                : [0, 0, 0, 0, 0, 0, 0, 0]
                }]
            }
            return data;
        },

        buildChart: function(el, data) {
            var options = {
                maintainAspectRatio : false,
                responsive : true,
                legend: {
                    display: false
                },
                scales: {
                    xAxes: [{
                        stacked: true,
                        gridLines : {
                            display : false,
                        }
                    }],
                    yAxes: [{
                        stacked: true,
                        ticks: {
                            min: 0,
                            beginAtZero: true,
                            precision: 1
                        },
                        gridLines : {
                            display : false,
                        }
                    }]
                }
            };

            var canvas = el.get(0).getContext('2d');
            var chart = new Chart(canvas, {
                type: 'bar',
                data: data,
                options: options
            });
            return chart;
        },

        updateChart: function(chart, labels, datasets) {
            chart.data.labels = labels;
            for (var i = 0; i < datasets.length; ++i) {
                chart.data.datasets[i].data = datasets[i];
            }
            chart.update();
        }
    }

    that = g.dashboard;

    /*
    var chartDispatcher = null;
    var chartService = null;

    var consoleLogTime = 0;
    var serverDataMap = {};

    var maxLogLine = 60;

    // 获取服务器信息
    console.getServers(function(data) {
        $('#dispatcher-box').find('h3').text(data.dispatchers.length);
        $('#service-box').find('h3').text(data.services.length);

        var now = Date.now();

        // 分配标签
        var tagIndex = 1;
        for (var i = 0; i < data.services.length; ++i) {
            var svr = data.services[i];

            var elTab = $('#log-tabs-server' + tagIndex + '-tab');
            elTab.css('visibility', 'visible');
            elTab.text('服务 ' + svr.name);

            var el = $('#log-tabs-server' + tagIndex).find('.log-view');

            serverDataMap[svr.name] = {
                "tabEl": el,
                "logTime": now - 300000,    // 日志时间
                "logTotal": 0               // 日志总行数
            };

            ++tagIndex;
        }
        for (var i = 0; i < data.dispatchers.length; ++i) {
            var svr = data.dispatchers[i];

            var elTab = $('#log-tabs-server' + tagIndex + '-tab');
            elTab.css('visibility', 'visible');
            elTab.text('调度 ' + svr.name);

            var el = $('#log-tabs-server' + tagIndex).find('.log-view');

            serverDataMap[svr.name] = {
                "tabEl": el,
                "logTime": now - 300000,    // 日志时间
                "logTotal": 0               // 日志总行数
            };

            ++tagIndex;
        }

        // 请求报告
        setTimeout(function() { reportTask(); }, 1000);
    });

    // 初始化 Chart
    chartDispatcher = dashboard.buildChart($('#dispatcher-chart'), dashboard.buildJVMChartDataTemplate());
    chartService = dashboard.buildChart($('#service-chart'), dashboard.buildJVMChartDataTemplate());

    var reportTask = function() {
        var dispatchers = console.dispatchers;
        var svr = dispatchers[0];
        console.queryJVMReport(svr.name, 8, function(data) {
            var labels = [];
            var dataset0 = [];
            var dataset1 = [];
            for (var i = 0; i < data.list.length; ++i) {
                var report = data.list[i];
                labels.push(ui.formatTimeHHMM(report.timestamp));
                dataset0.push(report.totalMemory);
                dataset1.push(report.freeMemory);
            }

            var t = setTimeout(function() {
                clearTimeout(t);
                dashboard.updateChart(chartDispatcher, labels, [dataset0, dataset1]);
            }, 100);
        });

        var services = console.services;
        var svr = services[0];
        console.queryJVMReport(svr.name, 8, function(data) {
            var labels = [];
            var dataset0 = [];
            var dataset1 = [];
            for (var i = 0; i < data.list.length; ++i) {
                var report = data.list[i];
                labels.push(ui.formatTimeHHMM(report.timestamp));
                dataset0.push(report.totalMemory);
                dataset1.push(report.freeMemory);
            }

            var t = setTimeout(function() {
                clearTimeout(t);
                dashboard.updateChart(chartService, labels, [dataset0, dataset1]);
            }, 100);
        });
    };

    // 报告任务
    setInterval(function() { reportTask(); }, 120000);

    // 日志定时任务
    setInterval(function() {
        console.queryConsoleLog(consoleLogTime, function(data) {
            if (data.lines.length == 0) {
                return;
            }

            var el = $('#log-tabs-console').find('.log-view');
            for (var i = 0; i < data.lines.length; ++i) {
                dashboard.appendLog(el, data.lines[i]);
            }
            consoleLogTime = data.last;

            // 滚动条控制
            var offset = parseInt(el.prop('scrollHeight'));
            el.scrollTop(offset);

            // 控制总条目数
            var total = el.children('p').length;
            var d = total - maxLogLine;
            if (d > 0) {
                dashboard.removeLog(el, d);
            }
        });

        if (null != console.services) {
            var serverList = [];
            serverList = serverList.concat(console.services, console.dispatchers);

            for (var i = 0; i < serverList.length; ++i) {
                var svr = serverList[i];
                console.queryLog(svr.name, serverDataMap[svr.name].logTime, function(data) {
                    if (data.lines.length == 0) {
                        return;
                    }

                    var total = serverDataMap[data.name].logTotal + data.lines.length;

                    for (var i = 0; i < data.lines.length; ++i) {
                        dashboard.appendLog(serverDataMap[data.name].tabEl, data.lines[i]);
                    }
                    // 更新日志时间
                    serverDataMap[data.name].logTime = data.last;

                    // 更新总数
                    serverDataMap[data.name].logTotal = total;

                    // 滚动条控制
                    var offset = parseInt(serverDataMap[data.name].tabEl.prop('scrollHeight'));
                    serverDataMap[data.name].tabEl.scrollTop(offset);

                    var d = total - maxLogLine;
                    if (d > 0) {
                        dashboard.removeLog(serverDataMap[data.name].tabEl, d);
                        serverDataMap[data.name].logTotal = total - d;
                    }
                });
            }
        }
    }, 10000);
    */
})(jQuery, window);
