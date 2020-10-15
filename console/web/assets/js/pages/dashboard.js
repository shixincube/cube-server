// dashboard.js

(function ($) {
    'use strict'

    var console = new Console();
    $.console = console;

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
        for (var i = 0; i < dispatchers.length; ++i) {
            var svr = dispatchers[i];
            console.queryJVMReport(svr.name, 8, function(data) {
                var labels = [];
                var dataset0 = [];
                var dataset1 = [];
                for (var i = 0; i < data.list.length; ++i) {
                    var report = data.list[i];
                    labels.push(ui.formatTimeHHMM(report.timestamp));
                    dataset0.push(report.totalMemory);
                    dataset1.push(report.freeMemory);

                    console.log(data.name + ' : ' + report.totalMemory + ', ' + report.freeMemory);
                }

                dashboard.updateChart(chartDispatcher, labels, [dataset0, dataset1]);
            });
        }

        var services = console.services;
        for (var i = 0; i < services.length; ++i) {
            var svr = services[i];
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

                dashboard.updateChart(chartService, labels, [dataset0, dataset1]);
            });
        }
    };

    // 报告任务
    setInterval(function() { reportTask(); }, 60000);

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
})(jQuery);
