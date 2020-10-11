// dashboard.js

(function ($) {
    'use strict'

    var console = new Console();
    $.console = console;

    var consoleLogTime = 0;
    var serverTagMap = {};

    console.getServers(function(data) {
        $('#dispatcher-box').find('h3').text(data.dispatchers.length);
        $('#service-box').find('h3').text(data.services.length);

        // 分配标签
        var tagIndex = 1;
        for (var i = 0; i < data.services.length; ++i) {
            var svr = data.services[i];
            // 填写数据
            svr.logTime = 0;

            var elTab = $('#log-tabs-server' + tagIndex + '-tab');
            elTab.css('visibility', 'visible');
            elTab.text('服务 ' + svr.name);

            var el = $('#log-tabs-server' + tagIndex).find('.log-view');
            serverTagMap[svr.name] = el;
            ++tagIndex;
        }
    });

    function appendLog(el, line) {
        var content = [];

        var date = new Date(line.time);
        content.push(date.getMonth() + 1);
        content.push('-');
        content.push(date.getDate());
        content.push(' ');
        content.push(date.getHours());
        content.push(':');
        content.push(date.getMinutes());
        content.push(':');
        content.push(date.getSeconds());
        content.push('.');
        content.push(date.getMilliseconds());
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
    }

    // 定时任务
    setInterval(function() {
        console.queryConsoleLog(consoleLogTime, function(data) {
            if (data.lines.length == 0) {
                return;
            }

            var el = $('#log-tabs-console').find('.log-view');
            for (var i = 0; i < data.lines.length; ++i) {
                var line = data.lines[i];
                appendLog(el, line);
            }
            consoleLogTime = data.last;
        });

        if (null != console.services) {
            for (var i = 0; i < console.services.length; ++i) {
                var svr = console.services[i];
                console.queryLog(svr.name, svr.logTime, function(data) {
                    if (data.lines.length == 0) {
                        return;
                    }

                    svr.logTime = data.last;
                });
            }
        }
    }, 10000);
})(jQuery);
