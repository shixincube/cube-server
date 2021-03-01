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

    var serviceList = [];

    // 检查是否合法
    console.checkUser(function(valid) {
        if (!valid) {
            window.location.href = 'index.html';
        }
        else {
            g.common.updateUserPanel(console);
            g.service.launch();
        }
    });

    var btnNewDeploy = null;
    var tableEl = null;

    function findService(tag, deployPath) {
        for (var i = 0; i < serviceList.length; ++i) {
            var value = serviceList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                return value;
            }
        }
        return null;
    }

    function updateService(service) {
        var tag = service.tag;
        var deployPath = service.deployPath;

        for (var i = 0; i < serviceList.length; ++i) {
            var value = serviceList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                serviceList[i] = service;
                break;
            }
        }
    }

    g.service = {
        launch: function() {
            btnNewDeploy = $('#btn_new_deploy');
            btnNewDeploy.click(function() {
                that.showNewDeployDialog();
            });

            tableEl = $('#server_table');

            // 获取默认部署数据
            console.getServiceDefaultDeploy(function(data) {
                if (undefined !== data.deployPath) {
                    defaultDeploy = data;
                }
                else {
                    $('.header-tip').html('控制台没有找到部署文件，请参考 <a href="https://gitee.com/shixinhulian/cube-manual/blob/master/QuickStart.md" target="_blank">快速开始</a> 进行操作。');
                }
            });

            console.getServices(function(list) {
                serviceList = list;
                that.refreshServerTable();
            });
        },

        refreshServerTable: function() {
            var body = tableEl.find('tbody');
            body.empty();

            serviceList.forEach(function(value, index) {
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
                                ' btn-sm" onclick="javascript:service.toggleServer(', index, ');">',
                                value.running ? 
                                    '<i class="fas fa-stop"></i> 停止' :
                                    '<i class="fas fa-play"></i> 启动',
                            '</button>',
                            '<button type="button" class="btn btn-primary btn-sm" onclick="javascript:service.showDetails(', index, ');">',
                                '<i class="fas fa-tasks"></i> 详情',
                            '</button>',
                            '<button type="button" class="btn btn-warning btn-sm" onclick="javascript:service.deleteServer(', index, ');">',
                                '<i class="fas fa-trash"></i> 删除',
                            '</button>',
                        '</td>',
                    '</tr>'
                ];

                body.append($(html.join('')));
            });
        },

        requestServiceStatus: function(tag, path, success, error) {
            $.ajax({
                type: 'POST',
                url: '/service/status',
                data: {
                    "tag": tag,
                    "path": path
                },
                success: function(response, status, xhr) {
                    success(response);
                },
                error: function(response) {
                    error(response.status);
                },
                dataType: 'json'
            });
        },

        startService: function(tag, path, password, success, error) {
            $.ajax({
                type: 'POST',
                url: '/service/start',
                data: {
                    "tag": tag,
                    "path": path,
                    "pwd": password 
                },
                success: function(response, status, xhr) {
                    success(response);
                },
                error: function(response) {
                    error(response.status);
                },
                dataType: 'json'
            });
        },

        stopService: function(tag, path, password, success, error) {
            $.ajax({
                type: 'POST',
                url: '/service/stop',
                data: {
                    "tag": tag,
                    "path": path,
                    "pwd": password 
                },
                success: function(response, status, xhr) {
                    success(response);
                },
                error: function(response) {
                    error(response.status);
                },
                dataType: 'json'
            });
        },

        toggleServer: function(index) {
            var service = serviceList[index];

            var el = $('#modal_toggle_server');

            var tipEl = el.find('.tip-content');
            var tip = null;
            if (service.running) {
                tip = '您确定要<span class="text-danger"><b>关停</b></span>服务单元服务器吗？';
            }
            else {
                tip = '您确定要<span class="text-danger"><b>启动</b></span>服务单元服务器吗？';
            }
            tipEl.html(tip);

            el.find('#input_tag').val(service.tag);
            el.find('#input_path').val(service.deployPath);
            el.find('#input_path').attr('title', service.deployPath);
            el.find('#input_config_path').val(service.configPath);
            el.find('#input_config_path').attr('title', service.configPath);
            el.find('#input_password').val('');

            el.modal('show');

            el.find('.overlay').css('visibility', 'hidden');
        },

        onToggleConfirm: function() {
            var el = $('#modal_toggle_server');
            var tag = el.find('#input_tag').val();
            var deployPath = el.find('#input_path').val();
            var password = el.find('#input_password').val();
            if (password.length < 6) {
                alert('请输入您的管理密码！');
                return;
            }
            // 计算密码的 MD5 码
            password = md5(password);

            el.find('.overlay').css('visibility', 'visible');
            el.find('#input_password').val('');

            var timer = 0;
            var complete = function() {
                clearInterval(timer);
                that.refreshServerTable();
                el.modal('hide');
            };

            // 获取服务器
            var service = findService(tag, deployPath);

            // 通过判断状态决定操作
            if (!service.running) {
                // 启动
                this.startService(tag, deployPath, password, function(service) {
                    if (service.running) {
                        complete();
                        return;
                    }

                    var count = 0;
                    timer = setInterval(function() {
                        ++count;

                        that.requestServiceStatus(tag, deployPath, function(server) {
                            var cur = findService(tag, deployPath);
                            if (!cur.running) {
                                if (server.running) {
                                    updateService(server);
                                    complete();
                                    g.common.toast(Toast.Success, '启动服务器成功');
                                }
                            }
                        }, function(status) {
                            console.log('#requestServiceStatus - ' + status);
                        });

                        if (count >= 6) {
                            complete();
                            g.common.toast(Toast.Error, '未能更新到服务器状态');
                        }
                    }, 5000);
                }, function(status) {
                    el.find('.overlay').css('visibility', 'hidden');
                    alert('操作失败，请检查您的管理密码是否输入正确。');
                });
            }
            else {
                // 关停
                this.stopService(tag, deployPath, password, function(service) {
                    if (!service.running) {
                        complete();
                        return;
                    }

                    var count = 0;
                    timer = setInterval(function() {
                        ++count;

                        that.requestServiceStatus(tag, deployPath, function(server) {
                            var cur = findService(tag, deployPath);
                            if (cur.running) {
                                if (!server.running) {
                                    updateService(server);
                                    complete();
                                    g.common.toast(Toast.Success, '关停服务器成功');
                                }
                            }
                        }, function(status) {
                            console.log('#requestServiceStatus - ' + status);
                        });

                        if (count >= 6) {
                            complete();
                            g.common.toast(Toast.Error, '未能更新到服务器状态');
                        }
                    }, 5000);
                }, function(status) {
                    el.find('.overlay').css('visibility', 'hidden');
                    alert('操作失败，请检查您的管理密码是否输入正确。');
                });
            }
        },

        showDetails: function(index) {
            var service = serviceList[index];

            var el = $('#modal_details');
            el.find('.tag').text(service.tag);
            el.find('.deploy-path').text(service.deployPath);
            el.find('.deploy-path').attr('title', service.deployPath);
            el.find('.config-path').text(service.configPath);
            el.find('.config-path').attr('title', service.configPath);
            el.find('.ap-host').text(service.server.host);
            el.find('.ap-port').text(service.server.port);
            el.find('.ap-maxconn').text(service.server.maxConnection);
            el.find('.log-level').text(service.logLevel);

            var contentEl = el.find('.cellets');

            var html = [];
            service.cellets.forEach(function(value, index) {
                var file = null;
                if (value.jar) {
                    var name = value.jar.path;
                    var size = '--';
                    var date = '--';

                    if (value.jar.name) {
                        name = value.jar.name;
                        size = g.util.formatSize(value.jar.size);
                        date = g.util.formatFullTime(value.jar.lastModified);
                    }

                    file = [
                        '<span class="info-box-number">', name, '</span>',
                        '<span class="info-box-number">', size, '</span>',
                        '<span class="info-box-number">', date, '</span>'
                    ];
                }
                else {
                    file = [
                        '<span class="info-box-number">',
                            '&nbsp;',
                        '</span>'
                    ];
                }

                var box = [
                    '<div class="info-box cellet-box">',
                        '<span class="info-box-icon bg-info"><i class="fas fa-cog"></i></span>',
                        '<div class="info-box-content">',
                            '<span class="info-box-text">', value.classes[0], '</span>',
                            file.join(''),
                        '</div>',
                    '</div>'
                ];
                html.push(box.join(''));
            });
            contentEl.html(html.join(''));

            el.modal('show');
        },

        deleteServer: function(index) {
            g.common.toast(Toast.Warning, '此功能暂不可用');
        },

        showNewDeployDialog: function() {
            if (g.common.toast) {
                g.common.toast(Toast.Warning, '此功能暂不可用');
                return;
            }
        }
    };

    that = g.service;

})(jQuery, window);
