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
                            '<button type="button" class="btn btn-primary btn-sm" onclick="javascript:dispatcher.showDetails(\'', value.deployPath, '\');">',
                                '<i class="fas fa-tasks"></i> 详情',
                            '</button>',
                            '<button type="button" class="btn btn-info btn-sm" onclick="javascript:;">',
                                '<i class="fas fa-cog"></i> 配置',
                            '</button>',
                            '<button type="button" class="btn btn-success btn-sm" onclick="javascript:;">',
                                '<i class="fas fa-play"></i> 启动',
                            '</button>',
                        '</td>',
                    '</tr>'
                ];

                body.append($(html.join('')));
            });
        },

        showDetails: function(deployPath) {
            var el = $('#modal_details');

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
