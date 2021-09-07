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

package cube.app.server.account;

import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器。
 */
public class AccountManager {

    private final static AccountManager instance = new AccountManager();

    private boolean initializing = false;

    private AccountStorage accountStorage;

    private Map<Long, OnlineAccount> onlineMap;

    private AccountManager() {
        this.onlineMap = new ConcurrentHashMap<>();
    }

    public final static AccountManager getInstance() {
        return AccountManager.instance;
    }

    public void start() {
        this.loadWithConfig();

        this.initializing = true;

        (new Thread() {
            @Override
            public void run() {
                if (null != accountStorage) {
                    accountStorage.open();
                }

                initializing = false;
            }
        }).start();
    }

    private void loadWithConfig() {
        String[] configFiles = new String[] {
                "server_dev.properties",
                "server.properties"
        };

        String configFile = null;
        for (String filename : configFiles) {
            File file = new File(filename);
            if (file.exists()) {
                configFile = filename;
                break;
            }
        }

        if (null != configFile) {
            try {
                Properties properties = ConfigUtils.readProperties(configFile);
                this.accountStorage = new AccountStorage(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        while (this.initializing) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (null != this.accountStorage) {
            this.accountStorage.close();
        }
    }

    public StateCode login(String tokenCode) {
        Token token = this.accountStorage.readToken(tokenCode);
        if (null == token) {
            return StateCode.NotFindToken;
        }

        if (token.expire < System.currentTimeMillis()) {
            // 已过期
            return StateCode.InvalidToken;
        }

        Account account = this.accountStorage.readAccount(token.accountId);
        if (null == account) {
            return StateCode.NotFindAccount;
        }

        this.addOnlineAccount(account, token.device);

        return StateCode.Success;
    }

    public void addOnlineAccount(Account account, String device) {
        OnlineAccount current = this.onlineMap.get(account.id);
        if (null == current) {
            this.onlineMap.put(account.id, new OnlineAccount(account, device));
        }
        else {
            current.addDevice(device);
        }
    }
}
