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
import cell.util.log.Logger;
import cube.console.storage.UserStorage;
import cube.console.tool.DeployTool;
import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器。
 */
public class UserManager {

    private long tokenAge = 7L * 24L * 60L * 60L * 1000L;

    private ConcurrentHashMap<String, UserToken> tokenMap;

    private UserStorage storage;

    public UserManager() {
        this.tokenMap = new ConcurrentHashMap<>();
    }

    public void start() {
        String filepath = null;
        for (String path : DeployTool.CONSOLE_PROP_FILES) {
            File file = new File(path);
            if (file.exists()) {
                filepath = path;
                break;
            }
        }

        try {
            Properties properties = ConfigUtils.readProperties(filepath);
            this.storage = new UserStorage(properties);
            this.storage.open();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#start", e);
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }
    }

    /**
     * 检查 Token 是否有效。
     *
     * @param tokenString
     * @return
     */
    public boolean checkToken(String tokenString) {
        UserToken userToken = this.tokenMap.get(tokenString);

        if (null == userToken) {
            userToken = this.storage.readToken(tokenString);
            if (null != userToken) {
                this.tokenMap.put(userToken.token, userToken);
            }
        }

        if (null == userToken) {
            return false;
        }

        return userToken.expire > System.currentTimeMillis();
    }

    /**
     * 获取用户的 Token 。
     *
     * @param tokenString
     * @return
     */
    public UserToken getToken(String tokenString) {
        UserToken userToken = this.tokenMap.get(tokenString);

        if (null == userToken) {
            userToken = this.storage.readToken(tokenString);
            if (null != userToken) {
                this.tokenMap.put(userToken.token, userToken);
            }
        }

        if (null != userToken) {
            if (null == userToken.user) {
                User user = this.storage.readUser(userToken.userId);
                userToken.user = user;
            }
        }

        return userToken;
    }

    /**
     * 用户签入。
     *
     * @param tokenString
     * @return
     */
    public UserToken signIn(String tokenString) {
        UserToken userToken = this.tokenMap.get(tokenString);

        if (null == userToken) {
            userToken = this.storage.readToken(tokenString);
            if (null != userToken) {
                this.tokenMap.put(userToken.token, userToken);
            }
        }

        return userToken;
    }

    /**
     * 用户签入。
     *
     * @param userName
     * @param password
     * @return
     */
    public UserToken signIn(String userName, String password) {
        User user = this.storage.readUser(userName);
        if (null == user) {
            return null;
        }

        if (!user.password.equalsIgnoreCase(password)) {
            return null;
        }

        String tokenString = Utils.randomString(32);

        long time = System.currentTimeMillis();
        long expire = time + this.tokenAge;

        UserToken token = new UserToken(user.id, tokenString, time, expire);
        token.user = user;

        this.tokenMap.put(tokenString, token);

        // 写入存储
        this.storage.writeToken(token);

        return token;
    }

    /**
     * 用户签出。
     *
     * @param tokenString
     */
    public void signOut(String tokenString) {
        // 从内存里删除
        UserToken token = this.tokenMap.remove(tokenString);
        if (null == token) {
            token = this.storage.readToken(tokenString);
            if (null == token) {
                return;
            }
        }

        this.storage.deleteToken(tokenString);
    }
}
