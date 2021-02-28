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

    var chartViewMap = {};

    var tabContentEl = null;
    var currentLogTabEl = null;
    var serverLogViewMap = {};

    var consoleLogTime = 0;
    var maxLogLine = 80;

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
            var logEl = $('.log-container');
            currentLogTabEl = logEl.find('#log-tabs-console-tab');
            tabContentEl = logEl.find('.tab-content');
            logEl.find('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                currentLogTabEl = $(e.target);
                var offset = parseInt(tabContentEl.prop('scrollHeight'));
                tabContentEl.scrollTop(offset);
            });

            var dispatcherRunning = 0;
            var serviceRunning = 0;

            var gotDispatcher = false;
            var gotService = false;

            console.getDispatchers(function(list) {
                dispatcherList = list;

                dispatcherList.forEach(function(value) {
                    if (value.running) {
                        ++dispatcherRunning;
                    }
                });

                that.updateDispatcherBox(list.length, dispatcherRunning);

                gotDispatcher = true;
                if (gotService) {
                    that.updateChartTab();
                    that.updateLogTab();
                }
            });

            console.getServices(function(list) {
                serviceList = list;

                serviceList.forEach(function(value) {
                    if (value.running) {
                        ++serviceRunning;
                    }
                });

                that.updateServiceBox(list.length, serviceRunning);

                gotService = true;
                if (gotDispatcher) {
                    that.updateChartTab();
                    that.updateLogTab();
                }
            });

            // 启动 JVM 报表
            setTimeout(function() {
                that.startReportTask();
            }, 10000);

            // 启动打印控制台日志
            that.startPrintLog();
        },

        updateChartTab: function() {
            var tabIndex = 1;

            dispatcherList.forEach(function(value) {
                $('#dispatcher-chart-tabs-tab-' + tabIndex).text('调度机#' + value.server.port);

                var tabEl = $('#dispatcher-chart-tab-' + tabIndex);
                var canvas = tabEl.find('.dispatcher-chart');
                chartViewMap[value.name] = {
                    "tabEl": tabEl,
                    "canvas": canvas,
                    "chart": that.buildChart(canvas, that.buildJVMChartDataTemplate())
                };

                ++tabIndex;
            });

            tabIndex = 1;
            serviceList.forEach(function(value) {
                $('#service-chart-tabs-tab-' + tabIndex).text('服务单元#' + value.server.port);

                var tabEl = $('#service-chart-tab-' + tabIndex);
                var canvas = tabEl.find('.service-chart');
                chartViewMap[value.name] = {
                    "tabEl": tabEl,
                    "canvas": canvas,
                    "chart": that.buildChart(canvas, that.buildJVMChartDataTemplate())
                };

                ++tabIndex;
            });
        },

        updateLogTab: function() {
            var now = Date.now();

            var tabIndex = 1;

            dispatcherList.forEach(function(value) {
                var elTab = $('#log-tabs-server' + tabIndex + '-tab');
                elTab.css('visibility', 'visible');
                elTab.text('调度机#' + value.server.port);

                var el = $('#log-tabs-server' + tabIndex).find('.log-view');

                serverLogViewMap[value.name] = {
                    "tabEl": el,
                    "logTime": now - 300000,    // 日志时间
                    "logTotal": 0               // 日志总行数
                };

                ++tabIndex;
            });

            serviceList.forEach(function(value) {
                var elTab = $('#log-tabs-server' + tabIndex + '-tab');
                elTab.css('visibility', 'visible');
                elTab.text('服务单元#' + value.server.port);

                var el = $('#log-tabs-server' + tabIndex).find('.log-view');

                serverLogViewMap[value.name] = {
                    "tabEl": el,
                    "logTime": now - 300000,    // 日志时间
                    "logTotal": 0               // 日志总行数
                };

                ++tabIndex;
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

        startReportTask: function() {
            var reportTask = function() {
                var serverList = [];
                serverList = serverList.concat(dispatcherList, serviceList);

                for (var n = 0; n < serverList.length; ++n) {
                    var server = serverList[n];
                    var view = chartViewMap[server.name];
                    if (undefined === view) {
                        continue;
                    }

                    console.queryJVMReport(server.name, 8, function(data) {
                        var labels = [];
                        var dataset0 = [];
                        var dataset1 = [];
                        for (var i = 0; i < data.list.length; ++i) {
                            var report = data.list[i];
                            labels.push(util.formatTimeHHMM(report.timestamp));
                            dataset0.push(report.totalMemory);
                            dataset1.push(report.freeMemory);
                        }

                        that.updateChart(chartViewMap[data.name].chart, labels, [dataset0, dataset1]);
                    });
                }
            };

            // 报告定时任务
            setInterval(function() { reportTask(); }, 60 * 1000);

            reportTask();
        },

        startPrintLog: function() {
            var processConsole = function() {
                console.queryConsoleLog(consoleLogTime, function(data) {
                    if (data.lines.length == 0) {
                        return;
                    }

                    var el = $('#log-tabs-console').find('.log-view');
                    for (var i = 0; i < data.lines.length; ++i) {
                        that.appendLog(el, data.lines[i]);
                    }
                    consoleLogTime = data.last;

                    // 滚动条控制
                    var tabId = currentLogTabEl.attr('aria-controls');
                    var content = tabContentEl.find('#' + tabId);
                    if (content.css('display') == 'block') {
                        var offset = parseInt(tabContentEl.prop('scrollHeight'));
                        tabContentEl.scrollTop(offset);
                    }

                    // 控制总条目数
                    var total = el.children('p').length;
                    var d = total - maxLogLine;
                    if (d > 0) {
                        that.removeLog(el, d);
                    }
                });

                var serverList = [];
                serverList = serverList.concat(dispatcherList, serviceList);

                for (var i = 0; i < serverList.length; ++i) {
                    var svr = serverList[i];
                    console.queryLog(svr.name, serverLogViewMap[svr.name].logTime, function(data) {
                        if (data.lines.length == 0) {
                            return;
                        }

                        var total = serverLogViewMap[data.name].logTotal + data.lines.length;

                        for (var i = 0; i < data.lines.length; ++i) {
                            that.appendLog(serverLogViewMap[data.name].tabEl, data.lines[i]);
                        }
                        // 更新日志时间
                        serverLogViewMap[data.name].logTime = data.last;

                        // 更新总数
                        serverLogViewMap[data.name].logTotal = total;

                        // 滚动条控制
                        var tabId = currentLogTabEl.attr('aria-controls');
                        var content = tabContentEl.find('#' + tabId);
                        if (content.css('display') == 'block') {
                            var offset = parseInt(tabContentEl.prop('scrollHeight'));
                            tabContentEl.scrollTop(offset);
                        }

                        var d = total - maxLogLine;
                        if (d > 0) {
                            that.removeLog(serverLogViewMap[data.name].tabEl, d);
                            serverLogViewMap[data.name].logTotal = total - d;
                        }
                    });
                }
            }

            // 日志定时任务
            setInterval(function() {
                processConsole();
            }, 10000);

            processConsole();
        },

        appendLog: function(el, line) {
            var content = [];

            var date = new Date(line.time);
            content.push(g.util.formatNumber(date.getMonth() + 1, 2));
            content.push('-');
            content.push(g.util.formatNumber(date.getDate(), 2));
            content.push(' ');
            content.push(g.util.formatNumber(date.getHours(), 2));
            content.push(':');
            content.push(g.util.formatNumber(date.getMinutes(), 2));
            content.push(':');
            content.push(g.util.formatNumber(date.getSeconds(), 2));
            content.push('.');
            content.push(g.util.formatNumber(date.getMilliseconds(), 3));
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

})(jQuery, window);
