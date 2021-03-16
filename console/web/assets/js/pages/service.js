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

    var defaultDeploy = null;

    var serviceList = [];

    var current = null;
    var autoTimer = 0;

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
    var monitorEl = null;

    /**
     * 提示输入框内数据不合法。
     * @param {jQuery} inputEl 
     * @param {string} text 
     * @param {number} delay
     */
    function tipInvalidInput(inputEl, text, delay) {
        inputEl.addClass('input-invalid');
        setTimeout(function() {
            inputEl.removeClass('input-invalid');
        }, undefined === delay ? 3000 : delay);
        g.common.toast(Toast.Error, text);
    }

    function findService(tag, deployPath) {
        for (var i = 0; i < serviceList.length; ++i) {
            var value = serviceList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                return value;
            }
        }
        return null;
    }

    function findServiceByName(name) {
        for (var i = 0; i < serviceList.length; ++i) {
            var value = serviceList[i];
            if (value.name == name) {
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

    /**
     * 切换密码输入框是否密码显示明文。
     */
    function onTogglePwdVisible() {
        var el = $('#modal_toggle_server');
        var inputEl = el.find('#input_password');
        var type = inputEl.attr('type');
        if (type == 'password') {
            inputEl.attr('type', 'text');
            $(this).html('<i class="fas fa-eye-slash"></i>');
        }
        else {
            inputEl.attr('type', 'password');
            $(this).html('<i class="fas fa-eye"></i>');
        }
    }

    /**
     * 开关服务器对话框的密码输入框键盘 Key Press 事件回调。
     * @param {*} event 
     */
    function onPasswordKeyPress(event) {
        if (event.keyCode == 13) {
            if (that.onToggleConfirm()) {
                $(this).blur();
            }
        }
    }

    g.service = {
        launch: function() {
            // 构建 Chart
            that.charts.build();

            btnNewDeploy = $('#btn_new_deploy');
            btnNewDeploy.click(function() {
                that.showNewDeployDialog();
            });

            var toggleModal = $('#modal_toggle_server');
            toggleModal.find('button[data-target="toggle"]').on('click', onTogglePwdVisible);
            toggleModal.find('#input_password').on('keypress', onPasswordKeyPress);

            var switchAuto = $("input[data-bootstrap-switch]");
            switchAuto.bootstrapSwitch({
                onText: '自动刷新',
                offText: '停止刷新',
                onSwitchChange: function(event, state) {
                    if (state) {
                        if (null == current) {
                            return false;
                        }

                        if (autoTimer > 0) {
                            clearInterval(autoTimer);
                        }
                        autoTimer = setInterval(function() {
                            that.refreshPerformance(current);
                        }, 30 * 1000);
                        that.refreshPerformance(current);
                    }
                    else {
                        clearInterval(autoTimer);
                        autoTimer = 0;
                    }
                }
            });
            switchAuto.removeAttr('checked');
            switchAuto.prop('checked', false);

            // 表格
            tableEl = $('#server_table');

            // 监视器
            monitorEl = $('.monitor');
            monitorEl.find('input[data-target="server-name"]').val('');
            monitorEl.find('input[data-target="perf-time"]').val('');

            // 配置界面
            bindStorageConfigRadioEvent();

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

            var selectServerEl = monitorEl.find('div[aria-labelledby="monitor_server"]');
            selectServerEl.empty();

            serviceList.forEach(function(value, index) {
                var capacityHTML = [
                    '<div class="progress progress-sm">',
                        '<div class="progress-bar bg-green" role="progressbar" aria-volumenow="0" aria-volumemin="0" aria-volumemax="100" style="width:0%"></div>',
                    '</div>',
                    '<small>0%</small>'
                ];

                var html = [
                    '<tr data-target="', value.name, '">',
                        '<td>', (index + 1), '</td>',
                        '<td class="tag-display">', value.tag, '</td>',
                        '<td><span>', value.deployPath, '</span></td>',
                        '<td class="server-state text-center">',
                            value.running ? 
                                '<span class="badge badge-success">运行中</span>' :
                                '<span class="badge badge-danger">已关闭</span>',
                        '</td>',
                        '<td class="server-capacity">',
                            capacityHTML.join(''),
                        '</td>',
                        '<td class="server-starttime">',
                            '<span>--</span>',
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
                            '<button type="button" class="btn btn-info btn-sm" onclick="javascript:service.showConfig(', index, ');">',
                                '<i class="fas fa-cog"></i> 配置',
                            '</button>',
                            '<button type="button" class="btn btn-warning btn-sm" onclick="javascript:service.runMockTest(', index, ');">',
                                '<i class="fas fa-bolt"></i> 拨测',
                            '</button>',
                        '</td>',
                    '</tr>'
                ];

                body.append($(html.join('')));

                if (value.running) {
                    that.refreshLastPerformance(value);
                }

                // 更新选择菜单
                html = [
                    '<a class="dropdown-item" href="javascript:service.onMonitorChange(\'', value.name, '\');">', value.name, '</a>'
                ];
                selectServerEl.append($(html.join('')));
            });
        },

        refreshLastPerformance: function(server) {
            console.queryPerformanceReport(server.name, function(data) {
                if (undefined === data.report) {
                    // 没有报告数据
                    return;
                }

                server.perf = data.report;

                // 计算综合负载
                var loadRate = console.calcServiceLoad(server.perf);

                // 更新表格
                var tr = tableEl.find('tr[data-target="' + server.name + '"]');
                var el = tr.find('.server-capacity');
                var bar = el.find('.progress-bar');
                bar.attr('aria-volumenow', loadRate);
                bar.css('width', loadRate + '%');
                el.find('small').text(loadRate + '%');
                // 启动时间
                tr.find('.server-starttime').html(['<span>', g.util.formatTimeMDHMS(server.perf.systemStartTime), '</span>'].join(''));
            }, 0, true);
        },

        refreshPerformance: function(server) {
            if (undefined === server.perf) {
                return;
            }

            var timestampArray = [];

            if (undefined === server.perfCache) {
                server.perfCache = [];
                server.jvmCache = [];

                for (var i = 0; i < 10; ++i) {
                    timestampArray.push(server.perf.timestamp - (i * 60000));
                }
                timestampArray.reverse();
            }
            else {
                var last = server.perfCache[server.perfCache.length - 1];
                timestampArray.push(last.timestamp + 60000);
            }

            this._requestPerformanceReport(server, timestampArray, function(success) {
                // 完成
                if (success) {
                    that.updateMonitor(server);
                }
                else {
                    g.common.toast(Toast.Warning, '刷新服务器性能数据错误');
                }
            });
        },

        _requestPerformanceReport: function(server, timestampArray, completeHandler) {
            if (timestampArray.length == 0) {
                if (server.perfCache.length == server.jvmCache.length) {
                    completeHandler(true);
                }
                else {
                    if (undefined === server.refreshCount) {
                        server.refreshCount = 0;
                    }
                    else {
                        server.refreshCount += 1;
                    }

                    if (server.refreshCount > 6) {
                        server.refreshCount = 0;
                        completeHandler(false);
                        return;
                    }

                    setTimeout(function() {
                        that._requestPerformanceReport(server, timestampArray, completeHandler);
                    }, 500);
                }
                return;
            }

            var timestamp = timestampArray.shift();
            console.queryPerformanceReport(server.name, function(data) {
                if (undefined === data.report) {
                    // 没有报告数据
                    that._requestPerformanceReport(server, timestampArray, completeHandler);
                    return;
                }

                server.perfCache.push(data.report);
                if (server.perfCache.length > 10) {
                    server.perfCache.shift();
                }

                if (data.report.timestamp > server.perf.timestamp) {
                    // 更新最近一次数据
                    server.perf = data.report;
                }

                that._requestPerformanceReport(server, timestampArray, completeHandler);
            }, timestamp, true);

            console.queryJVMReport(server.name, 1, function(data) {
                if (data.list.length > 0) {
                    server.jvmCache.push(data.list[0]);
                }
            }, timestamp);
        },

        updateMonitor: function(current) {
            // 更新时间戳
            monitorEl.find('input[data-target="perf-time"]').val(g.util.formatFullTime(current.perf.timestamp));

            that.charts.updateChart(current);
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
            if (password.length == 0) {
                return false;
            }

            if (password.length < 6) {
                alert('请输入您的管理密码！');
                return false;
            }
            // 计算密码的 MD5 码
            password = md5(password);

            el.find('.overlay').css('visibility', 'visible');
            el.find('#input_password').val('');

            var timer = 0;
            var complete = function() {
                clearInterval(timer);
                clearInterval(autoTimer);
                autoTimer = 0;
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

            return true;
        },

        onMonitorChange: function(name) {
            var server = findServiceByName(name);
            if (null == server) {
                return;
            }

            if (!server.running) {
                g.common.toast(Toast.Info, '服务器没有启动');
                return;
            }

            // 已选择服务器
            monitorEl.find('input[data-target="server-name"]').val(name);

            current = server;
            that.refreshPerformance(current);
        },

        /**
         * 显示详情。
         * @param {number} index 
         */
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
            el.find('.adapter-host').text(service.adapter.host);
            el.find('.adapter-port').text(service.adapter.port);
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

        /**
         * 显示配置。
         * @param {number} index 
         */
        showConfig: function(index) {
            var service = serviceList[index];

            var el = $('#modal_config_server');

            el.find('.tag').text(service.tag);
            el.find('.deploy-path').text(service.deployPath);
            el.find('.ap-host input').val(service.server.host);
            el.find('.ap-port input').val(service.server.port);
            el.find('.ap-maxconn input').val(service.server.maxConnection);
            el.find('.adapter-host input').val(service.adapter.host);
            el.find('.adapter-port input').val(service.adapter.port);
            el.find('.log-level select').val(service.logLevel);

            updateStorageConfigTab('storage-tabs-auth', 'AuthStorage', service.storage["Auth"]);
            updateStorageConfigTab('storage-tabs-contact', 'ContactStorage', service.storage["Contact"]);
            updateStorageConfigTab('storage-tabs-messaging', 'MessagingStorage', service.storage["Messaging"]);
            updateStorageConfigTab('storage-tabs-filestorage', 'FileStorage', service.storage["FileStorage"]);

            el.find('.overlay').css('visibility', 'hidden');
            el.modal('show');
        },

        runMockTest: function(index) {
            g.common.toast(Toast.Warning, '此功能暂不可用');
        },

        showNewDeployDialog: function() {
            if (g.common.toast) {
                g.common.toast(Toast.Warning, '此功能暂不可用');
                return;
            }
        }
    };

    // 管理配置界面 - 开始

    function bindStorageConfigRadioEvent() {
        var authEl = $('#storage-tabs-auth');
        authEl.find('input:radio[name="AuthStorage"]').change(function() {
            if (this.id.indexOf('sqlite') > 0) {
                selectStorageConfig(authEl, 'SQLite');
            }
            else if (this.id.indexOf('mysql') > 0) {
                selectStorageConfig(authEl, 'MySQL');
            }
        });

        var contactEl = $('#storage-tabs-contact');
        contactEl.find('input:radio[name="ContactStorage"]').change(function() {
            if (this.id.indexOf('sqlite') > 0) {
                selectStorageConfig(contactEl, 'SQLite');
            }
            else if (this.id.indexOf('mysql') > 0) {
                selectStorageConfig(contactEl, 'MySQL');
            }
        });

        var messagingEl = $('#storage-tabs-messaging');
        messagingEl.find('input:radio[name="MessagingStorage"]').change(function() {
            if (this.id.indexOf('sqlite') > 0) {
                selectStorageConfig(messagingEl, 'SQLite');
            }
            else if (this.id.indexOf('mysql') > 0) {
                selectStorageConfig(messagingEl, 'MySQL');
            }
        });

        var fileStorageEl = $('#storage-tabs-filestorage');
        fileStorageEl.find('input:radio[name="FileStorage"]').change(function() {
            if (this.id.indexOf('sqlite') > 0) {
                selectStorageConfig(fileStorageEl, 'SQLite');
            }
            else if (this.id.indexOf('mysql') > 0) {
                selectStorageConfig(fileStorageEl, 'MySQL');
            }
        });
    }

    function selectStorageConfig(el, value) {
        if (value == 'SQLite') {
            el.find('#form-sqlite').css('display', 'block');
            el.find('#form-mysql').css('display', 'none');
        }
        else if (value == 'MySQL') {
            el.find('#form-sqlite').css('display', 'none');
            el.find('#form-mysql').css('display', 'block');
        }
    }

    function updateStorageConfigTab(id, name, config) {
        var el = $('#' + id);

        if (config.type == 'SQLite') {
            el.find('input:radio[name="' + name + '"]')[0].checked = true;
            el.find('#form-mysql').css('display', 'none');
            el.find('#form-sqlite').css('display', 'block');
            el.find('#form-sqlite .file').val(config.file);
        }
        else if (config.type == 'MySQL') {
            el.find('input:radio[name="' + name + '"]')[1].checked = true;
            el.find('#form-sqlite').css('display', 'none');
            var myel = el.find('#form-mysql');
            myel.css('display', 'block');
            myel.find('.host').val(config.host);
            myel.find('.port').val(config.port);
            myel.find('.schema').val(config.schema);
            myel.find('.user').val(config.user);
            myel.find('.password').val(config.password);
        }
    }

    // 管理配置界面 - 结束

    that = g.service;

})(jQuery, window);
