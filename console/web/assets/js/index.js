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

(function($) {

    var usernameEl = $('#signin-form').find('input[name="username"]');
    var passwordEl = $('#signin-form').find('input[name="password"]');

    usernameEl.val('');
    passwordEl.val('');

    var btn = $('#signin');
    btn.on('click', function(event) {
        var event = event || window.event;

        var username = usernameEl.val();
        if (username.length < 3) {
            $(document).Toasts('create', {
                class: 'bg-danger',
                title: '账号输入错误',
                autohide: true,
                delay: 3000,
                body: '请输入正确的登录账号'
            });
            event.preventDefault();
            return;
        }

        var password = passwordEl.val();
        if (password.length < 6) {
            $(document).Toasts('create', {
                class: 'bg-danger',
                title: '密码输入错误',
                autohide: true,
                delay: 3000,
                body: '请输入至少6位密码'
            });
            event.preventDefault();
            return;
        }

        // 散列密码
        var hash = md5(password);
        passwordEl.val(hash);
    });
})(jQuery);
