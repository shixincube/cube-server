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

    var that = null;

    var console = new Console();
    $.console = console;

    var btnNewDeploy = null;

    var modalDetails = null;

    var tableEl = null;
    var monitorEl = null;

    var defaultDeploy = null;

    var dispatcherList = [];

    // 是否处于编辑状态
    var editable = false;

    // 当前详情的服务器
    var currentServer = null;
    // 当前正在修改的调度机的 Cellet 列表
    var currentServerCellets = null;
    // 当前选择的 Director
    var currentDirector = null;
    // 删除的 Director 配置
    var removedDirectors = [];
    // 新增的 Director 配置
    var addedDirectors = [];

    // 监视器当前服务器
    var current = null;
    var currentChart = null;
    var autoTimer = 0;      // 自动刷新数据定时器

    // 检查是否合法
    console.checkUser(function(valid) {
        if (!valid) {
            window.location.href = 'index.html';
        }
        else {
            g.common.updateUserPanel(console);
            g.dispatcher.launch();
        }
    });

    function findDispatcher(tag, deployPath) {
        for (var i = 0; i < dispatcherList.length; ++i) {
            var value = dispatcherList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                return value;
            }
        }
        return null;
    }

    function findDispatcherByName(name) {
        for (var i = 0; i < dispatcherList.length; ++i) {
            var value = dispatcherList[i];
            if (value.name == name) {
                return value;
            }
        }
        return null;
    }

    function updateDispatcher(dispatcher) {
        var tag = dispatcher.tag;
        var deployPath = dispatcher.deployPath;

        for (var i = 0; i < dispatcherList.length; ++i) {
            var value = dispatcherList[i];
            if (value.tag == tag && value.deployPath == deployPath) {
                dispatcherList[i] = dispatcher;
                break;
            }
        }
    }

    function buildMonitorChart(labels, data) {
        var chartData = {
            labels  : labels,
            datasets: [{
                label               : '执行计数',
                backgroundColor     : 'rgba(60,141,188,0.9)',
                borderColor         : 'rgba(60,141,188,0.8)',
                pointRadius         : false,
                pointColor          : '#3b8bba',
                pointStrokeColor    : 'rgba(60,141,188,1)',
                pointHighlightFill  : '#fff',
                pointHighlightStroke: 'rgba(60,141,188,1)',
                data                : data
            }]
        }

        var chartCanvas = monitorEl.find('#monitor_chart').get(0).getContext('2d');
        var chartOptions = {
            responsive : true,
            maintainAspectRatio : false,
            datasetFill : false,
            legend: {
                display: false
            },
            scales: {
                yAxes: [{
                    stacked: true,
                    position: 'left',
                    gridLines: {
                        drawBorder: false
                    },
                    ticks: {
                        min: 0
                        //maxTicksLimit: 1
                    }
                }]
            }
        };

        var chart = new Chart(chartCanvas, {
            type: 'bar', 
            data: chartData,
            options: chartOptions
        });
        return chart;
    }

    function isRemovedDirector(director) {
        for (var i = 0; i < removedDirectors.length; ++i) {
            var d = removedDirectors[i];
            if (d.address == director.address && d.port == director.port) {
                return true;
            }
        }
        return false;
    }

    function isAddedDirector(director) {
        for (var i = 0; i < addedDirectors.length; ++i) {
            var d = addedDirectors[i];
            if (d.address == director.address && d.port == director.port) {
                return true;
            }
        }
        return false;
    }

    function updateDirectorSelector(server) {
        var tag = server.tag;
        var deployPath = server.deployPath;
        var selEl = modalDetails.find('.director-list');
        var html = [];

        var list = server.directors.concat(addedDirectors);
        list.forEach(function(value, index) {
            if (isRemovedDirector(value)) {
                return;
            }

            // 将 Cellet 复制到修改缓存
            value.celletCache = value.cellets.concat();
            html.push(['<option data="', index, '~', tag, '~', deployPath, '">#', (index + 1),
                ' - ', value.address, ':', value.port, '</option>'].join(''));
        });
        selEl.html(html.join(''));
    }

    /**
     * 切换选择 Director 
     * @param {*} index 
     * @param {*} address 
     * @param {*} port 
     * @returns 
     */
    function selectDirector(index, address, port) {
        var selEl = modalDetails.find('.director-list');

        if (undefined === address) {
            var options = selEl.find('option');
            if (options.length == 0) {
                return;
            }

            var value = options.eq(index).text();
            selEl.val(value);
        }
        else {
            selEl.val(['#', (index + 1), ' - ', address, ':', port].join(''));
        }
        
        selEl.change();
    }

    /**
     * 详情对话框里切换 Director 的 select 改变回调。
     * @param {number|*} index 
     * @param {string} tag 
     * @param {string} deployPath 
     * @returns 
     */
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

        // 计算有效的列表
        var list = dispatcher.directors.concat(addedDirectors);
        for (var i = 0; i < removedDirectors.length; ++i) {
            var removed = removedDirectors[i];
            for (var n = 0; n < list.length; ++n) {
                var d = list[n];
                if (d.address == removed.address && d.port == removed.port) {
                    list.splice(n, 1);
                    break;
                }
            }
        }

        var el = modalDetails;

        if (list.length == 0) {
            return;
        }

        var director = list[index];
        currentDirector = director;

        if (editable) {
            el.find('.director-address').html('<input type="text" class="form-control form-control-sm input-host" value="' + director.address + '" ' + 
                'onkeyup="javascript:dispatcher.onDirectorAddressKeyup();" />');
            el.find('.director-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + director.port + '" ' +
                'onkeyup="javascript:dispatcher.onDirectorPortKeyup();" />');
            el.find('.director-weight').html('<input type="text" class="form-control form-control-sm input-port" value="' + director.weight + '" />');
            updateCelletsInService(director.celletCache);
        }
        else {
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
    }

    /**
     * 更新在编辑状态下的 Dispatcher 里的 Cellet 列表。
     */
    function updateCelletsInDispatcher() {
        var el = modalDetails;
        // Cellets
        var html = [];
        currentServerCellets.forEach(function(value, index) {
            html.push([
                '<div class="cellet">',
                    '<span>', value, '</span>',
                    '<a href="javascript:dispatcher.onRemoveCelletFromDispatcher(\'', value, '\');"><span class="badge badge-danger">', '&times;', '</span></a>',
                '</div>'].join(''));
        });
        // 添加按钮
        html.push(
            '<div class="input-group input-group-sm">',
                '<input type="text" class="form-control" data-target="new-cellet">',
                '<span class="input-group-append">',
                    '<button type="button" class="btn btn-primary btn-flat" onclick="javascript:dispatcher.onAddCelletToDispatcher();"><i class="fas fa-edit"></i></button>',
                '</span>',
            '</div>');
        el.find('.cellets').html(html.join(''));
    }

    /**
     * 更新在编辑状态下的服务单元里的 Cellet 列表。
     * @param {*} director 
     */
    function updateCelletsInService(cellets) {
        var el = modalDetails;
        var html = [];
        cellets.forEach(function(value, index) {
            html.push([
                '<div class="cellet">',
                    '<span>', value, '</span>',
                    '<a href="javascript:dispatcher.onRemoveCelletFromService(\'', value, '\');"><span class="badge badge-danger">', '&times;', '</span></a>',
                '</div>'].join(''));
        });
        // 添加按钮
        html.push(
            '<div class="input-group input-group-sm">',
                '<input type="text" class="form-control" data-target="new-service-cellet">',
                '<span class="input-group-append">',
                    '<button type="button" class="btn btn-primary btn-flat" onclick="javascript:dispatcher.onAddCelletToService();"><i class="fas fa-edit"></i></button>',
                '</span>',
            '</div>');
        el.find('.director-cellets').html(html.join(''));
    }

    /**
     * 切换密码框是否显示密码原文。
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

    /**
     * dispatcher
     */
    g.dispatcher = {
        /**
         * 启动程序。
         */
        launch: function() {
            btnNewDeploy = $('#btn_new_deploy');
            btnNewDeploy.click(function() {
                that.showNewDeployDialog();
            });

            modalDetails = $('#modal_details');
            modalDetails.on('hidden.bs.modal', function() {
                that.closeDetailsEditor();
            });
            // 编辑配置按钮
            modalDetails.btnEdit = modalDetails.find('button[data-target="edit"]');
            modalDetails.btnEdit.on('click', function() {
                that.openDetailsEditor();
            });
            // 提交配置按钮
            modalDetails.btnSubmit = modalDetails.find('button[data-target="submit"]');
            modalDetails.btnSubmit.on('click', function() {
                that.submitDetails();
            });
            modalDetails.btnSubmit.css('display', 'none');
            // 放弃修改按钮
            modalDetails.btnDiscard = modalDetails.find('button[data-target="discard"]');
            modalDetails.btnDiscard.on('click', function() {
                that.closeDetailsEditor();
            });
            modalDetails.btnDiscard.css('display', 'none');
            // 删除 Director 配置按钮
            modalDetails.btnRemoveDirector = modalDetails.find('button[data-target="remove-service"]');
            modalDetails.btnRemoveDirector.on('click', function() {
                that.removeCurrentDirector();
            });
            // 新增 Director 配置按钮
            modalDetails.btnAddDirector = modalDetails.find('button[data-target="add-service"]');
            modalDetails.btnAddDirector.on('click', function() {
                that.newDirector();
            });
            // 选择 Director
            modalDetails.find('.director-list').change(onDetailDirectorChange);

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
            that.fillEmptyChart();

            // 获取默认部署数据
            console.getDispatcherDefaultDeploy(function(data) {
                if (undefined !== data.deployPath) {
                    defaultDeploy = data;
                }
                else {
                    $('.header-tip').html('控制台没有找到部署文件，请参考 <a href="https://gitee.com/shixinhulian/cube-manual/blob/master/QuickStart.md" target="_blank">快速开始</a> 进行操作。');
                }
            });

            console.getDispatchers(function(list) {
                dispatcherList = list;
                that.refreshServerTable();
            });
        },

        /**
         * 刷新服务器表格。
         */
        refreshServerTable: function() {
            var body = tableEl.find('tbody');
            body.empty();

            var selectServerEl = monitorEl.find('div[aria-labelledby="monitor_server"]');
            selectServerEl.empty();

            dispatcherList.forEach(function(value, index) {
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
                        '<td class="server-state">',
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
                                ' btn-sm" onclick="javascript:dispatcher.toggleServer(', index, ');">',
                                value.running ? 
                                    '<i class="fas fa-stop"></i> 停止' :
                                    '<i class="fas fa-play"></i> 启动',
                            '</button>',
                            '<button type="button" class="btn btn-primary btn-sm" onclick="javascript:dispatcher.showDetails(', index, ');">',
                                '<i class="fas fa-tasks"></i> 详情',
                            '</button>',
                            '<button type="button" class="btn btn-warning btn-sm" onclick="javascript:dispatcher.runMockTest(', index, ');">',
                                '<i class="fas fa-bolt"></i> 拨测',
                            '</button>',
                        '</td>',
                    '</tr>'
                ];

                body.append($(html.join('')));

                if (value.running) {
                    that.refreshPerformance(value);
                }

                // 更新选择菜单
                html = [
                    '<a class="dropdown-item" href="javascript:dispatcher.onMonitorChange(\'', value.name, '\');">', value.name, '</a>'
                ];
                selectServerEl.append($(html.join('')));
            });
        },

        /**
         * 从服务器刷新性能数据。
         * @param {*} server 
         */
        refreshPerformance: function(server) {
            console.queryPerformanceReport(server.name, function(data) {
                if (undefined === data.report) {
                    // 没有报告数据
                    return;
                }

                server.perf = data.report;

                // 计算综合负载
                var loadRate = console.calcDispatcherLoad(server.perf);

                // 更新表格
                var tr = tableEl.find('tr[data-target="' + server.name + '"]');
                var el = tr.find('.server-capacity');
                var bar = el.find('.progress-bar');
                bar.attr('aria-volumenow', loadRate);
                bar.css('width', loadRate + '%');
                el.find('small').text(loadRate + '%');
                // 启动时间
                tr.find('.server-starttime').html(['<span>', g.util.formatTimeMDHMS(server.perf.systemStartTime), '</span>'].join(''));

                if (current == server) {
                    // 更新监视器
                    that.updateMonitor(server);
                }
            });
        },

        requestDispatcherStatus: function(tag, path, success, error) {
            $.ajax({
                type: 'POST',
                url: '/dispatcher/status',
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

        startDispatcher: function(tag, path, password, success, error) {
            $.ajax({
                type: 'POST',
                url: '/dispatcher/start',
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

        stopDispatcher: function(tag, path, password, success, error) {
            $.ajax({
                type: 'POST',
                url: '/dispatcher/stop',
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

        /**
         * 开关服务器。
         * @param {*} index 
         */
        toggleServer: function(index) {
            var dispatcher = dispatcherList[index];

            var el = $('#modal_toggle_server');

            var tipEl = el.find('.tip-content');
            var tip = null;
            if (dispatcher.running) {
                tip = '您确定要<span class="text-danger"><b>关停</b></span>调度机服务器吗？';
            }
            else {
                tip = '您确定要<span class="text-danger"><b>启动</b></span>调度机服务器吗？';
            }
            tipEl.html(tip);

            el.find('#input_tag').val(dispatcher.tag);
            el.find('#input_path').val(dispatcher.deployPath);
            el.find('#input_path').attr('title', dispatcher.deployPath);
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
            var dispatcher = findDispatcher(tag, deployPath);

            // 通过判断状态决定操作
            if (!dispatcher.running) {
                // 启动
                this.startDispatcher(tag, deployPath, password, function(dispatcher) {
                    if (dispatcher.running) {
                        complete();
                        return;
                    }

                    var count = 0;
                    timer = setInterval(function() {
                        ++count;

                        that.requestDispatcherStatus(tag, deployPath, function(server) {
                            var cur = findDispatcher(tag, deployPath);
                            if (!cur.running) {
                                if (server.running) {
                                    updateDispatcher(server);
                                    complete();
                                    g.common.toast(Toast.Success, '启动服务器成功');
                                }
                            }
                        }, function(status) {
                            console.log('#requestDispatcherStatus - ' + status);
                        });

                        if (count >= 5) {
                            complete();
                            g.common.toast(Toast.Error, '未能更新到服务器状态');
                        }
                    }, 2000);
                }, function(status) {
                    el.find('.overlay').css('visibility', 'hidden');
                    alert('操作失败，请检查您的管理密码是否输入正确。');
                });
            }
            else {
                // 关停
                this.stopDispatcher(tag, deployPath, password, function(dispatcher) {
                    if (!dispatcher.running) {
                        complete();
                        return;
                    }

                    var count = 0;
                    timer = setInterval(function() {
                        ++count;

                        that.requestDispatcherStatus(tag, deployPath, function(server) {
                            var cur = findDispatcher(tag, deployPath);
                            if (cur.running) {
                                if (!server.running) {
                                    updateDispatcher(server);
                                    complete();
                                    g.common.toast(Toast.Success, '关停服务器成功');
                                }
                            }
                        }, function(status) {
                            console.log('#requestDispatcherStatus - ' + status);
                        });

                        if (count >= 5) {
                            complete();
                            g.common.toast(Toast.Error, '未能更新到服务器状态');
                        }
                    }, 2000);
                }, function(status) {
                    el.find('.overlay').css('visibility', 'hidden');
                    alert('操作失败，请检查您的管理密码是否输入正确。');
                });
            }

            return true;
        },

        /**
         * 显示服务器详情。
         * @param {number} index 
         */
        showDetails: function(index) {
            var server = dispatcherList[index];
            if (null == server) {
                return;
            }

            // 设置当前详情服务器
            currentServer = server;

            var tag = server.tag;
            var deployPath = server.deployPath;

            var el = modalDetails;
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
            el.find('.log-level').text(server.logLevel);

            // Cellets
            var html = [];
            server.cellets.forEach(function(value, index) {
                html.push(['<span class="badge badge-info">', value, '</span>'].join(''));
            });
            el.find('.cellets').html(html.join(''));

            // 服务器单元
            updateDirectorSelector(server);
            selectDirector(0);

            el.modal('show');
        },

        /**
         * 详解界面切换到编辑状态。
         */
        openDetailsEditor: function() {
            editable = true;

            var el = modalDetails;

            el.btnEdit.css('display', 'none');
            el.btnSubmit.css('display', 'block');
            el.btnDiscard.css('display', 'block');
            el.btnRemoveDirector.css('visibility', 'visible');
            el.btnAddDirector.css('visibility', 'visible');
            el.find('.server-detail-group-append').each(function() {
                $(this).css('margin-left', '70px');
            });

            var server = currentServer;

            el.find('.ap-host').html('<input type="text" class="form-control form-control-sm input-host" value="' + server.server.host + '" />');
            el.find('.ap-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + server.server.port + '" />');
            el.find('.ap-maxconn').html('<input type="text" class="form-control form-control-sm input-number" value="' + server.server.maxConnection + '" />');
            el.find('.wsap-host').html('<input type="text" class="form-control form-control-sm input-host" value="' + server.wsServer.host + '" />');
            el.find('.wsap-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + server.wsServer.port + '" />');
            el.find('.wsap-maxconn').html('<input type="text" class="form-control form-control-sm input-number" value="' + server.wsServer.maxConnection + '" />');
            el.find('.wssap-host').html('<input type="text" class="form-control form-control-sm input-host" value="' + server.wssServer.host + '" />');
            el.find('.wssap-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + server.wssServer.port + '" />');
            el.find('.wssap-maxconn').html('<input type="text" class="form-control form-control-sm input-number" value="' + server.wssServer.maxConnection + '" />');
            el.find('.http-host').html('<input type="text" class="form-control form-control-sm input-host" value="' + server.http.host + '" />');
            el.find('.http-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + server.http.port + '" />');
            el.find('.https-host').html('<input type="text" class="form-control form-control-sm input-host" value="' + server.https.host + '" />');
            el.find('.https-port').html('<input type="text" class="form-control form-control-sm input-port" value="' + server.https.port + '" />');

            var logLevelSelect = [
                '<select class="custom-select">',
                    '<option>', 'DEBUG', '</option>',
                    '<option>', 'INFO', '</option>',
                    '<option>', 'WARNING', '</option>',
                    '<option>', 'ERROR', '</option>',
                '</select>'
            ];
            el.find('.log-level').html(logLevelSelect.join(''));
            el.find('.log-level').find('select').val(server.logLevel);

            // Cellets
            currentServerCellets = server.cellets.concat();
            updateCelletsInDispatcher();

            // 服务单元
            selectDirector(0);
        },

        /**
         * 从编辑状态退出。
         */
        closeDetailsEditor: function() {
            editable = false;

            var el = modalDetails;

            el.btnEdit.css('display', 'block');
            el.btnSubmit.css('display', 'none');
            el.btnDiscard.css('display', 'none');
            el.btnRemoveDirector.css('visibility', 'hidden');
            el.btnAddDirector.css('visibility', 'hidden');
            el.find('.server-detail-group-append').each(function() {
                $(this).css('margin-left', '120px');
            });

            var server = currentServer;

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
            el.find('.log-level').text(server.logLevel);

            // Cellets
            var html = [];
            server.cellets.forEach(function(value, index) {
                html.push(['<span class="badge badge-info">', value, '</span>'].join(''));
            });
            el.find('.cellets').html(html.join(''));

            // 重置数据
            removedDirectors = [];
            addedDirectors = [];

            // 服务单元
            updateDirectorSelector(server);
            selectDirector(0);
        },

        /**
         * 提交详情。
         */
        submitDetails: function() {
            var el = modalDetails;
            var serverHost = el.find('.ap-host').find('input').val();

        },

        /**
         * 删除当前 Director
         */
        removeCurrentDirector: function() {
            if (null == currentDirector) {
                return;
            }

            var selEl = modalDetails.find('.director-list');
            if (selEl.find('option').length <= 1) {
                g.common.toast(g.Toast.Warning, '操作无效，当前仅有一条服务单元记录');
                return;
            }

            if (!isRemovedDirector(currentDirector)) {
                // 记录删除的数据
                removedDirectors.push(currentDirector);
            }

            var server = currentServer;

            currentDirector = null;

            updateDirectorSelector(server);
            selectDirector(0);
        },

        /**
         * 新建 Director 配置界面
         */
        newDirector: function() {
            var director = {
                address: '192.168.' + g.util.randomNumber(1, 254) + '.' + g.util.randomNumber(1, 254),
                port: '6000',
                cellets: currentDirector.cellets.concat(),
                weight: currentDirector.weight
            };

            addedDirectors.push(director);

            var selEl = modalDetails.find('.director-list');
            var index = selEl.find('option').length;
            updateDirectorSelector(currentServer);
            selectDirector(index, director.address, director.port);
        },

        onRemoveCelletFromDispatcher: function(cellet) {
            var index = currentServerCellets.indexOf(cellet);
            if (index >= 0) {
                currentServerCellets.splice(index, 1);
                updateCelletsInDispatcher();
            }
        },

        onAddCelletToDispatcher: function() {
            var el = modalDetails;
            var newCellet = el.find('input[data-target="new-cellet"]').val();
            newCellet = newCellet.trim();
            if (newCellet.length == 0) {
                return;
            }

            var index = currentServerCellets.indexOf(newCellet);
            if (index >= 0) {
                return;
            }

            currentServerCellets.push(newCellet);

            updateCelletsInDispatcher();
        },

        onRemoveCelletFromService: function(cellet) {
            var cellets = currentDirector.celletCache;
            var index = cellets.indexOf(cellet);
            if (index >= 0) {
                cellets.splice(index, 1);
                updateCelletsInService(cellets);
            }
        },

        onAddCelletToService: function(cellet) {
            var el = modalDetails;
            var newCellet = el.find('input[data-target="new-service-cellet"]').val();
            newCellet = newCellet.trim();
            if (newCellet.length == 0) {
                return;
            }

            var cellets = currentDirector.celletCache;
            var index = cellets.indexOf(newCellet);
            if (index >= 0) {
                return;
            }

            currentDirector.celletCache.push(newCellet);

            updateCelletsInService(currentDirector.celletCache);
        },

        onDirectorAddressKeyup: function(e) {
            var el = modalDetails.find('.director-address input');
            var option = modalDetails.find('.director-list option:selected');
            currentDirector.address = el.val().trim();

            var array = option.text().split('-');
            option.text(array[0] + '- ' + currentDirector.address + ':' + currentDirector.port);
        },
    
        onDirectorPortKeyup: function(e) {
            var el = modalDetails.find('.director-port input');
            var value = el.val().trim();
            if (!g.util.isUnsigned(value)) {
                return;
            }

            currentDirector.port = el.val().trim();

            var option = modalDetails.find('.director-list option:selected');
            var array = option.text().split('-');
            option.text(array[0] + '- ' + currentDirector.address + ':' + currentDirector.port);
        },

        /**
         * 执行服务验证测试。
         * @param {number} index 
         */
        runMockTest: function(index) {
            g.common.toast(Toast.Warning, '此功能暂不可用');
        },

        /**
         * 更新监视器界面和数据。
         * @param {*} current 
         */
        updateMonitor: function(current) {
            // 更新时间戳
            monitorEl.find('input[data-target="perf-time"]').val(g.util.formatFullTime(current.perf.timestamp));

            // Update chart data
            var benchmark = current.perf.benchmark;
            var counterMap = benchmark.counterMap;
            var labels = [];
            var data = [];
            for (var c in counterMap) {
                labels.push(c);
            }
            labels.sort();
            labels.forEach(function(value) {
                data.push(counterMap[value]);
            });

            if (null == currentChart) {
                currentChart = buildMonitorChart(labels, data);
            }
            else {
                currentChart.data.labels = labels;
                currentChart.data.datasets[0].data = data;
                currentChart.update();
            }

            // 更新负载
            var connNums = current.perf.connNums;
            for (var i = 0; i < connNums.length; ++i) {
                var value = connNums[i];
                if (value.port == current.server.port) {
                    monitorEl.find('.shm-rt').text(value.realtime);
                    monitorEl.find('.shm-max').text(value.max);
                    monitorEl.find('.shm-rate').css('width', Math.floor(value.realtime / value.max) + '%');
                }
                else if (value.port == current.wsServer.port) {
                    monitorEl.find('.ws-rt').text(value.realtime);
                    monitorEl.find('.ws-max').text(value.max);
                    monitorEl.find('.ws-rate').css('width', Math.floor(value.realtime / value.max) + '%');
                }
                else if (value.port == current.wssServer.port) {
                    monitorEl.find('.wss-rt').text(value.realtime);
                    monitorEl.find('.wss-max').text(value.max);
                    monitorEl.find('.wss-rate').css('width', Math.floor(value.realtime / value.max) + '%');
                }
            }
            var loadRate = console.calcDispatcherLoad(current.perf) + '%';
            monitorEl.find('.avg-load').text(loadRate);
            monitorEl.find('.load-rate').css('width', loadRate);

            var avgTimeList = console.arrangeAvgResponseTime(current.perf);
            for (var i = 0; i < 6; ++i) {
                var value = i < avgTimeList.length ? avgTimeList[i] : null;

                var el = monitorEl.find('div[data-target="slot-' + (i + 1) + '"]');

                if (null == value) {
                    el.find('.description-header').text('-- ms');
                    el.find('.description-text').text('--');
                    el.find('.description-percentage').html('<span class="text-primary"><i class="fas fa-caret-left"></i> 0</span>');
                }
                else {
                    el.find('.description-header').text(value.data.value + ' ms');
                    el.find('.description-text').text(value.action);
                    if (value.data.delta < 0) {
                        el.find('.description-percentage').html('<span class="text-success"><i class="fas fa-caret-up"></i> '
                            + Math.abs(value.data.delta) + '</span>');
                    }
                    else if (value.data.delta > 0) {
                        el.find('.description-percentage').html('<span class="text-danger"><i class="fas fa-caret-down"></i> '
                            + Math.abs(value.data.delta) + '</span>');
                    }
                    else {
                        el.find('.description-percentage').html('<span class="text-primary"><i class="fas fa-caret-left"></i> 0</span>');
                    }
                }
            }
        },

        fillEmptyChart: function() {
            if (null != currentChart) {
                return;
            }

            var labels = ['', '', '', '', '', ''];
            var data = [0, 0, 0, 0, 0, 0];
            currentChart = buildMonitorChart(labels, data);
        },

        onMonitorChange: function(name) {
            var server = findDispatcherByName(name);
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

        showNewDeployDialog: function() {
            if (g.common.toast) {
                g.common.toast(Toast.Warning, '此功能暂不可用');
                return;
            }

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
