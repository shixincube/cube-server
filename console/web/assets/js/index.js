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

(function($) {

    var usernameEl = $('#signin-form').find('input[name="username"]');
    var passwordEl = $('#signin-form').find('input[name="password"]');

    usernameEl.val('');
    passwordEl.val('');

    var username = null;
    var password = null;

    function submit(event) {
        var event = event || window.event;

        username = usernameEl.val();
        if (username.length < 3) {
            $(document).Toasts('create', {
                class: 'bg-danger',
                title: '账号输入错误',
                autohide: true,
                delay: 3000,
                body: '请输入正确的登录账号'
            });
            event.preventDefault();
            return false;
        }

        password = passwordEl.val();
        if (password.length < 6) {
            $(document).Toasts('create', {
                class: 'bg-danger',
                title: '密码输入错误',
                autohide: true,
                delay: 3000,
                body: '请输入至少6位密码'
            });
            event.preventDefault();
            return false;
        }

        // 散列密码
        var hash = md5(password);
        passwordEl.val(hash);
        password = hash;
        return true;
    }

    usernameEl.on('keydown', function(event) {
        if (event.keyCode == 13) {
            return false;
        }
    });

    passwordEl.on('keydown', function(event) {
        if (event.keyCode == 13) {
            return false;
        }
    });

    passwordEl.on('keyup', function(event) {
        if (event.keyCode == 13) {
            if (submit(event)) {
                $.ajax({
                    type: 'POST',
                    url: '/signin/',
                    data: {
                        "username": username,
                        "password": password
                    }
                }).done(function(response) {
                    window.location.href = 'dashboard.html';
                }).fail(function() {
                });
            }
            return false;
        }
    });

    var btn = $('#signin');
    btn.on('click', function(event) {
        submit(event);
    });

    $(document).ready(function() {
        var token = null;
        var cookie = document.cookie;
        var array = cookie.split(';');
        for (var i = 0; i < array.length; ++i) {
            var value = array[i].trim().split('=');
            if (value.length == 2) {
                if (value[0] == 'CubeConsoleToken') {
                    token = value[1].trim();
                    break;
                }
            }
        }

        if (null == token) {
            return;
        }

        $.ajax({
            type: 'POST',
            url: '/signin/'
        }).done(function(response) {
            window.location.href = 'dashboard.html';
        }).fail(function() {
            console.log('登录过期，重新登录');
        });
    });
})(jQuery);
