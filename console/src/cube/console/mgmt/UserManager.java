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

package cube.console.mgmt;

import cell.util.Utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器。
 */
public class UserManager {

    private ConcurrentHashMap<String, User> tokenMap;

    public UserManager() {
        this.tokenMap = new ConcurrentHashMap<>();
    }

    public String signin(String userName, String password) {
        User user = getUser(userName);
        if (null == user) {
            return null;
        }

        if (!user.password.equalsIgnoreCase(password)) {
            return null;
        }

        String token = Utils.randomString(32);
        user.addToken(token);
        this.tokenMap.put(token, user);

        return token;
    }

    protected User getUser(String userName) {
        if (userName.equals("cube")) {
            // 密码：shixincube
            return new User("cube", "c7af98d321febe62e04d45e8806852e0");
        }

        return null;
    }
}
