/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

(function(global) {

    global.ui = {};

    function formatNumber(num, length) {
        if (length == 2) {
            if (num < 10) {
                return '0' + num;
            }
        }
        else if (length == 3) {
            if (num < 10) {
                return '00' + num;
            }
            else if (num < 100) {
                return '0' + num;
            }
        }
        else if (length == 4) {
            if (num < 10) {
                return '000' + num;
            }
            else if (num < 100) {
                return '00' + num;
            }
            else if (num < 1000) {
                return '0' + num;
            }
        }

        return '' + num;
    }
    global.ui.formatNumber = formatNumber;

    function formatTimeHHMM(time) {
        var date = new Date(time);
        return formatNumber(date.getHours(), 2) + ':' + formatNumber(date.getMinutes(), 2);
    }
    global.ui.formatTimeHHMM = formatTimeHHMM;

    function makeTimeLineArray(start, num) {
        var array = [];
        var time = start;
        for (var i = 0; i < num; ++i) {
            var date = new Date(time);
            array[i] = formatNumber(date.getHours(), 2) + ':' + formatNumber(date.getMinutes(), 2);
            time += 60000;
        }
        return array;
    }

    global.dashboard = {
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
            var labels = makeTimeLineArray(Date.now(), 8);
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
})(window);
