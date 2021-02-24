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

    var console = new Console();
    $.console = console;

    var defaultDeploy = null;

    var dispatcherList = [];

    // 检查是否合法
    console.checkUser(function(valid) {
        if (!valid) {
            window.location.href = 'index.html';
        }
        else {
            g.common.updateView(console);
            g.dispatcher.launch();
        }
    });

    var btnNewDeploy = null;
    var tableEl = null;

    function findDispatcher(tag, deployPath) {
        for (var i = 0; i < dispatcherList.length; ++i) {
            var value = dispatcherList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                return value;
            }
        }
        return null;
    }

    function onDetailDirectorChange(index, tag, deployPath) {
        if (typeof index !== 'number') {
            var selected = $(this).find("option:selected");
            var data = selected.attr('data');
            var array = data.split('~');
            index = parseInt(array[0]);
            tag = array[1];
            deployPath = array[2];
        }

        var dispatcher = findDispatcher(tag, deployPath);
        if (null == dispatcher) {
            return;
        }

        var el = $('#modal_details');
        var director = dispatcher.directors[index];
        el.find('.director-address').text(director.address);
        el.find('.director-port').text(director.port);
        el.find('.director-weight').text(director.weight);

        var html = [];
        director.cellets.forEach(function(value, index) {
            html.push(['<span class="badge badge-success">', value, '</span>'].join(''));
        });
        if (html.length == 0) {
            html.push('&nbsp;');
        }
        el.find('.director-cellets').html(html.join(''));
    }


    g.dispatcher = {
        launch: function() {
            btnNewDeploy = $('#btn_new_deploy');
            btnNewDeploy.click(function() {
                that.showNewDeployDialog();
            });

            tableEl = $('#server_table');

            // 获取默认部署数据
            console.getDispatcherDefaultDeploy(function(data) {
                if (undefined !== data.deployPath) {
                    defaultDeploy = data;
                }
                else {
                    $('.header-tip').html('控制台没有找到部署文件，请参考 <a href="https://gitee.com/shixinhulian/cube-manual/blob/master/QuickStart.md" target="_blank">快速开始</a> 进行操作。');
                }
            });

            console.getDispatchers(function(tag, list) {
                dispatcherList = list;
                that.refreshServerTable();
            });
        },

        refreshServerTable: function() {
            var body = tableEl.find('tbody');
            body.empty();

            dispatcherList.forEach(function(value, index) {
                var capacityHTML = [
                    '<div class="progress progress-sm">',
                        '<div class="progress-bar bg-green" role="progressbar" aria-volumenow="',
                            value.running ? 1 : 0, '" aria-volumemin="0" aria-volumemax="100" style="width:',
                            value.running ? 1 : 0, '%"></div>',
                    '</div>',
                    '<small>', value.running ? 1 : 0, '%</small>'
                ];

                var html = [
                    '<tr>',
                        '<td>', (index + 1), '</td>',
                        '<td class="tag-display">', value.tag, '</td>',
                        '<td><span>', value.deployPath, '</span></td>',
                        '<td class="server-capacity">',
                            capacityHTML.join(''),
                        '</td>',
                        '<td class="server-state text-center">',
                            value.running ? 
                                '<span class="badge badge-success">运行中</span>' :
                                '<span class="badge badge-danger">已关闭</span>',
                        '</td>',
                        '<td class="server-actions text-right">',
                            '<button type="button" class="btn ', value.running ? 'btn-danger' : 'btn-success',
                                ' btn-sm" onclick="javascript:dispatcher.toggleServer(', index, ');">',
                                value.running ? 
                                    '<i class="fas fa-stop"></i> 停止' :
                                    '<i class="fas fa-play"></i> 启动',
                            '</button>',
                            '<button type="button" class="btn btn-primary btn-sm" onclick="javascript:dispatcher.showDetails(\'', value.tag, '\',\'', value.deployPath, '\');">',
                                '<i class="fas fa-tasks"></i> 详情',
                            '</button>',
                            '<button type="button" class="btn btn-danger btn-sm" onclick="javascript:;">',
                                '<i class="fas fa-trash"></i> 删除',
                            '</button>',
                        '</td>',
                    '</tr>'
                ];

                body.append($(html.join('')));
            });
        },

        startDispatcher: function(tag, path, password, handler) {
            $.post('/dispatcher/start', {
                "tag": tag,
                "path": path,
                "pwd": password 
            }, function(response, status, xhr) {
                handler();
            }, 'json');
        },

        stopDispatcher: function(tag, path, password) {
            $.post('/dispatcher/stop', {
                "tag": tag,
                "path": path,
                "pwd": password
            }, function(response, status, xhr) {
                handler();
            }, 'json');
        },

        toggleServer: function(index) {
            var dispatcher = dispatcherList[index];

            var el = $('#modal_toggle_server');

            var tipEl = el.find('.tip-content');
            var tip = null;
            if (dispatcher.running) {
                tip = '';
            }
            else {
                tip = '您确定要<span class="text-danger"><b>启动</b></span>调度机服务器吗？';
            }
            tipEl.html(tip);

            el.find('#input_tag').val(dispatcher.tag);
            el.find('#input_path').val(dispatcher.deployPath);

            el.modal('show');
        },

        

        showDetails: function(tag, deployPath) {
            var server = findDispatcher(tag, deployPath);
            if (null == server) {
                return;
            }

            var el = $('#modal_details');
            el.find('.tag').text(tag);
            el.find('.deploy-path').text(deployPath);
            el.find('.ap-host').text(server.server.host);
            el.find('.ap-port').text(server.server.port);
            el.find('.ap-maxconn').text(server.server.maxConnection);
            el.find('.wsap-host').text(server.wsServer.host);
            el.find('.wsap-port').text(server.wsServer.port);
            el.find('.wsap-maxconn').text(server.wsServer.maxConnection);
            el.find('.wssap-host').text(server.wssServer.host);
            el.find('.wssap-port').text(server.wssServer.port);
            el.find('.wssap-maxconn').text(server.wssServer.maxConnection);
            el.find('.http-host').text(server.http.host);
            el.find('.http-port').text(server.http.port);
            el.find('.https-host').text(server.https.host);
            el.find('.https-port').text(server.https.port);

            var html = [];
            server.cellets.forEach(function(value, index) {
                html.push(['<span class="badge badge-info">', value, '</span>'].join(''));
            });
            el.find('.cellets').html(html.join(''));

            var selEl = el.find('.director-list');
            selEl.change(onDetailDirectorChange);
            html = [];
            server.directors.forEach(function(value, index) {
                html.push(['<option data="', index, '~', tag, '~', deployPath, '">#', (index + 1),
                    ' - ',value.address, ':', value.port, '</option>'].join(''));
            });
            selEl.html(html.join(''));

            onDetailDirectorChange(0, tag, deployPath);

            el.modal('show');
        },

        showNewDeployDialog: function() {
            if (null == defaultDeploy) {
                alert('控制台没有找到部署文件');
                return;
            }

            var el = $('#modal_new_deploy');
            var serversEl = el.find('#select_server');
            serversEl.append('<option value="' + defaultDeploy.tag + '">' + defaultDeploy.tag + '</option>');

            var deployPathEl = el.find('#input_deploy_path');
            deployPathEl.val(defaultDeploy.deployPath);

            var cellConfigEl = el.find('#input_cell_config');
            cellConfigEl.val(defaultDeploy.cellConfigFile);

            var propertiesEl = el.find('#input_properties');
            propertiesEl.val(defaultDeploy.propertiesFile);

            el.modal('show');
        }
    };

    that = g.dispatcher;

})(jQuery, window);
