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

import cell.util.Utils;
import cell.util.log.Logger;
import cube.app.server.util.LuckyNumbers;
import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器。
 */
public class AccountManager extends TimerTask {

    private final static AccountManager instance = new AccountManager();

    private final static long WEB_TIMEOUT = 5L * 60L * 1000L;

    private final static long TOKEN_DURATION = 10 * 365 * 24 * 3600 * 1000L;

    private final boolean useLuckyNumberId = true;

    private boolean initializing = false;

    private Timer timer;

    private int tickCount = 0;

    private AccountStorage accountStorage;

    private Map<Long, OnlineAccount> onlineIdMap;

    private Map<String, OnlineAccount> onlineTokenMap;

    private AccountCache accountCache;

    private AccountManager() {
        this.onlineIdMap = new ConcurrentHashMap<>();
        this.onlineTokenMap = new ConcurrentHashMap<>();
    }

    public final static AccountManager getInstance() {
        return AccountManager.instance;
    }

    public void start() {
        this.loadWithConfig();

        this.initializing = true;

        this.accountCache = new AccountCache(this.accountStorage);

        this.timer = new Timer();
        this.timer.schedule(this, 30 * 1000, 60 * 1000);

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

        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        if (null != this.accountStorage) {
            this.accountStorage.close();
        }
    }

    /**
     * 使用令牌进行登录。
     *
     * @param tokenCode
     * @return
     */
    public Token login(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        Token token = this.accountStorage.readToken(tokenCode);
        if (null == token) {
            return null;
        }

        if (token.expire < System.currentTimeMillis()) {
            // 已过期
            return null;
        }

        Account account = this.accountStorage.readAccount(token.accountId);
        if (null == account) {
            return null;
        }

        this.addOnlineAccount(account, token.device, token);

        return token;
    }

    /**
     * 使用账号密码进行登录。
     *
     * @param accountName
     * @param password
     * @param device
     * @return
     */
    public Token loginWithAccount(String accountName, String password, String device) {
        Account account = this.accountStorage.readAccount(accountName, password);
        if (null == account) {
            return null;
        }

        long now = System.currentTimeMillis();
        Token token = new Token(account.id, Utils.randomString(32), device,
                now, now + TOKEN_DURATION);

        // 写入令牌
        token = this.accountStorage.writeToken(token);

        this.addOnlineAccount(account, device, token);

        return token;
    }

    /**
     * 使用电话号码和密码登录。
     *
     * @param phoneNumber
     * @param password
     * @param device
     * @return
     */
    public Token loginWithPhoneNumber(String phoneNumber, String password, String device) {
        Account account = this.accountStorage.readAccountByPhoneNumber(phoneNumber);
        if (null == account) {
            // 没有账号
            return null;
        }

        if (!account.password.equalsIgnoreCase(password)) {
            // 密码不一致
            return null;
        }

        long now = System.currentTimeMillis();
        Token token = new Token(account.id, Utils.randomString(32), device,
                now, now + TOKEN_DURATION);

        // 写入令牌
        token = this.accountStorage.writeToken(token);

        this.addOnlineAccount(account, device, token);

        return token;
    }

    /**
     * 账号登出。
     *
     * @param token
     * @param device
     * @return
     */
    public StateCode logout(String token, String device) {
        OnlineAccount account = this.onlineTokenMap.remove(token);
        if (null == account) {
            return StateCode.NotFindToken;
        }

        account.removeDevice(device);

        if (0 == account.numDevices()) {
            // 移除在线的账号
            this.onlineIdMap.remove(account.id);
        }

        // TODO 修改 Token 的失效时间

        return StateCode.Success;
    }

    /**
     * 使用账号名进行注册。
     *
     * @param accountName
     * @param password
     * @param nickname
     * @param avatar
     * @return
     */
    public Account registerWithAccountName(String accountName, String password, String nickname, String avatar) {
        Long accountId = this.useLuckyNumberId ? LuckyNumbers.make() : (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        while (this.accountStorage.existsAccountId(accountId)) {
            accountId = (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        }

        Account account = this.accountStorage.writeAccount(accountId, accountName, password, nickname, avatar);
        return account;
    }

    /**
     * 使用手机号码注册。
     *
     * @param phoneNumber
     * @param password
     * @return
     */
    public Account registerWithPhoneNumber(String phoneNumber, String password) {
        Long accountId = this.useLuckyNumberId ? LuckyNumbers.make() : (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        while (this.accountStorage.existsAccountId(accountId)) {
            accountId = (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        }

        String avatar = "default";

        Account account = this.accountStorage.writeAccount(accountId, phoneNumber, password, avatar);
        return account;
    }

    /**
     * 心跳。
     *
     * @param tokenCode
     * @return
     */
    public boolean heartbeat(String tokenCode) {
        OnlineAccount account = this.onlineTokenMap.get(tokenCode);
        Token token = account.getToken(tokenCode);
        if (null == token) {
            return false;
        }

        token.timestamp = System.currentTimeMillis();
        return true;
    }

    protected void addOnlineAccount(Account account, String device, Token token) {
        OnlineAccount current = this.onlineIdMap.get(account.id);
        if (null == current) {
            current = new OnlineAccount(account, device, token);
            this.onlineIdMap.put(account.id, current);
        }
        else {
            current.addDevice(device, token);
        }

        this.onlineTokenMap.put(token.code, current);
    }

    /**
     * 获取指定令牌的在线联系人。
     *
     * @param tokenCode
     * @return
     */
    public OnlineAccount getOnlineAccount(String tokenCode) {
        OnlineAccount onlineAccount = this.onlineTokenMap.get(tokenCode);
        if (null != onlineAccount) {
            return onlineAccount;
        }

        // 使用令牌登录
        Token token = login(tokenCode);
        if (null == token) {
            return null;
        }

        return this.onlineTokenMap.get(tokenCode);
    }

    public boolean isOnlineToken(String tokenCode) {
        return this.onlineTokenMap.containsKey(tokenCode);
    }

    public Account getAccount(Long accountId) {
        return this.accountCache.getAccount(accountId);
    }

    public Account updateAccount(Long accountId, String newName, String newAvatar) {
        Account account = this.getAccount(accountId);
        // TODO
        return account;
    }

    @Override
    public void run() {
        this.accountCache.onTick();

        ++this.tickCount;
        if (this.tickCount >= 3) {
            this.tickCount = 0;

            final ArrayList<String> tokenList = new ArrayList<>();
            final ArrayList<String> deviceList = new ArrayList<>();

            long now = System.currentTimeMillis();

            // 检测 Web 端是否离线
            Iterator<Map.Entry<String, OnlineAccount>> iter = this.onlineTokenMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, OnlineAccount> entry = iter.next();
                String tokenCode = entry.getKey();
                OnlineAccount account = entry.getValue();

                // 选出 Web 设备
                String device = account.getDevice(tokenCode);
                if (null != device && device.startsWith("Web/")) {
                    Token token = account.getToken(tokenCode);
                    if (null != token && (now - token.timestamp >= WEB_TIMEOUT)) {
                        // 超时
                        tokenList.add(tokenCode);
                        deviceList.add(device);
                    }
                }
            }

            if (!tokenList.isEmpty()) {
                (new Thread() {
                    @Override
                    public void run() {
                        for (int i = 0; i < tokenList.size(); ++i) {
                            Logger.i(AccountManager.class, "Account timeout: " + tokenList.get(i));
                            logout(tokenList.get(i), deviceList.get(i));
                        }
                    }
                }).start();
            }
        }
    }
}
