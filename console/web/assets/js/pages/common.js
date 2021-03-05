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

(function($, g) {

    g.Toast = {
        Success: 'success',
        Info: 'info',
        Error: 'error',
        Warning: 'warning',
        Question: 'question'
    };

    var toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3500
    });

    g.common = {
        /**
         * 显示吐司提示。
         * @param {string} type 
         * @param {string} text 
         */
        toast: function(type, text) {
            toast.fire({
                icon: type,
                title: text
            });
        },

        showUserProfile: function() {
            if (null == $.console.user) {
                return;
            }

            var user = $.console.user;

            var el = $('#modal_user_profile');

            var role = user.role == 1 ? '超级管理员' : '管理员';

            el.find('.profile-name').text(user.displayName);
            el.find('.user-name').text(user.name);
            el.find('.user-role').text(role);
            el.find('.user-group').text(user.group);
            el.find('.avatar').attr('src', user.avatar);

            el.modal('show');
        },

        signOut: function() {
            if (confirm('您确定要退出登录吗？')) {
                $.ajax({
                    type: 'post',
                    url: '/signout/'
                }).done(function(response) {
                    window.location.href = '/index.html';
                }).fail(function() {
                });

                $('#modal_user_profile').modal('hide');
            }
        },

        updateUserPanel: function(console) {
            var userPanel = $('.user-panel');
            userPanel.find('img[data-target="avatar"]').attr('src', console.user.avatar);
            userPanel.find('a[data-target="name"]').text(console.user.displayName);
        },

        updateMainNav: function(numDispatchers, numServices) {
            var el = $('nav .nav');
            el.find('span[data-target="dispatcher-num"]').text(numDispatchers);
            el.find('span[data-target="service-num"]').text(numServices);
        }
    };

})(jQuery, window);
