/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import cube.app.server.applet.WeChatApplet;
import cube.app.server.util.LuckyNumbers;
import cube.util.ConfigUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户管理器。
 */
public class AccountManager extends TimerTask {

    private final static AccountManager instance = new AccountManager();

    private final static long WEB_TIMEOUT = 5 * 60 * 1000;

    private final static long TOKEN_DURATION = 10 * 365 * 24 * 3600 * 1000L;

    /**
     * 是否优先生成幸运号作为 ID 。
     */
    private final boolean useLuckyNumberId = false;

    private boolean initializing = false;

    private Timer timer;

    private int tickCount = 0;

    private AccountStorage accountStorage;

    private Map<Long, OnlineAccount> onlineIdMap;

    private Map<String, OnlineAccount> onlineTokenMap;

    private AccountCache accountCache;

    private ExecutorService executor;

    /**
     * 验证码队列。
     */
    private Queue<Captcha> captchaQueue;

    /**
     * 被激活使用的验证码。
     */
    private List<String> activeCaptchaList;

    private AccountManager() {
        this.onlineIdMap = new ConcurrentHashMap<>();
        this.onlineTokenMap = new ConcurrentHashMap<>();
        this.captchaQueue = new ConcurrentLinkedQueue<>();
        this.activeCaptchaList = new ArrayList<>();
    }

    public final static AccountManager getInstance() {
        return AccountManager.instance;
    }

    public void start() {
        this.executor = Executors.newFixedThreadPool(2);

        this.loadConfig();

        this.initializing = true;

        this.accountCache = new AccountCache(this.accountStorage);

        this.timer = new Timer();
        this.timer.schedule(this, 30 * 1000, 60 * 1000);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (null != accountStorage) {
                    accountStorage.open();

                    BuildInData buildInData = new BuildInData();
                    for (Account account : buildInData.accountList) {
                        accountStorage.writeAccount(account);
                    }
                    buildInData = null;
                }

                // 预先创建验证码文件
                for (int i = 0; i < 10; ++i) {
                    Captcha captcha = createCaptcha();
                    captchaQueue.offer(captcha);
                }

                initializing = false;
            }
        });
    }

    private void loadConfig() {
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

        // 删除不使用的验证码图片
        for (Captcha captcha : this.captchaQueue) {
            captcha.file.delete();
        }

        this.executor.shutdown();
    }

    /**
     * 校验号码是否可用，并可设置是否给该号码发送验证码。
     *
     * @param regionCode
     * @param phoneNumber
     * @param verificationCodeRequired
     * @return
     */
    public boolean checkPhoneAvailable(String regionCode, String phoneNumber, boolean verificationCodeRequired) {
        Account account = this.accountStorage.readAccountByPhoneNumber(phoneNumber);
        if (null != account) {
            // 号码已注册
            return false;
        }

        // TODO 发送验证码
        return true;
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
     * 使用小程序 JS Code 进行登录。
     *
     * @param jsCode
     * @param device
     * @return
     */
    public Token loginWithAppletJSCode(String jsCode, String device) {
        long accountId = WeChatApplet.getInstance().checkAccount(jsCode);
        if (accountId <= 0) {
            return null;
        }

        Account account = getAccount(accountId);

        long now = System.currentTimeMillis();
        Token token = new Token(accountId, Utils.randomString(32), device,
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
     * 注册账号。
     *
     * @param account
     * @return
     */
    public Account register(Account account) {
        return this.accountStorage.writeAccount(account);
    }

    /**
     * 注册账号。
     *
     * @param domain
     * @param accountName
     * @param phoneNumber
     * @param password
     * @param nickname
     * @param avatar
     * @return
     */
    public Account register(String domain, String accountName, String phoneNumber, String password, String nickname, String avatar) {
        if (null == nickname || nickname.length() == 0) {
            nickname = "Cube-" + Utils.randomString(8);
        }

        long accountId = this.useLuckyNumberId ? LuckyNumbers.make() :
                (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        Account account = new Account(accountId, domain, accountName, phoneNumber, password, nickname, avatar,
                Account.STATE_NORMAL);
        account = this.accountStorage.writeAccount(account);

        return account;
    }

    /**
     * 使用账号名进行注册。
     *
     * @param domain
     * @param accountName
     * @param password
     * @param nickname
     * @param avatar
     * @return
     */
    public Account registerWithAccountName(String domain, String accountName, String password, String nickname, String avatar) {
        Long accountId = this.useLuckyNumberId ? LuckyNumbers.make() :
                (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        while (this.accountStorage.existsAccountId(accountId)) {
            accountId = (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        }

        Account account = this.accountStorage.writeAccountWithAccountName(accountId, domain,
                accountName, password, nickname, avatar);
        return account;
    }

    /**
     * 使用手机号码注册。
     *
     * @param domain
     * @param phoneNumber
     * @param password
     * @param nickname
     * @param avatar
     * @return
     */
    public Account registerWithPhoneNumber(String domain, String phoneNumber, String password, String nickname, String avatar) {
        Long accountId = this.useLuckyNumberId ? LuckyNumbers.make() :
                (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        while (this.accountStorage.existsAccountId(accountId)) {
            accountId = (long) Utils.randomInt(20000000, Integer.MAX_VALUE - 1);
        }

        String avatarName = (null != avatar) ? avatar : "default";

        Account account = this.accountStorage.writeAccountWithPhoneNumber(accountId, domain, phoneNumber,
                password, nickname, avatarName);
        return account;
    }

    /**
     * 绑定小程序账号。
     *
     * @param jsCode
     * @param phoneNumber
     * @param accountName
     * @param device
     * @return
     */
    public Account bindAppletAccount(String jsCode, String phoneNumber, String accountName, String device) {
        Account account = null;
        if (null != phoneNumber) {
            account = this.accountStorage.readAccountByPhoneNumber(phoneNumber);
        }
        else if (null != accountName) {
            account = this.accountStorage.readAccountByAccountName(accountName);
        }

        if (null == account) {
            // 账号不存在
            Logger.w(this.getClass(), "#bindAppletAccount - Can NOT find account");
            return null;
        }

        if (WeChatApplet.getInstance().bind(jsCode, account, device)) {
            return account;
        }
        else {
            Logger.e(this.getClass(), "#bindAppletAccount - Bind applet account failed");
            return null;
        }
    }

    /**
     * 搜索指定账号 ID 的账号。
     *
     * @param accountId
     * @return
     */
    public Account searchAccountWithId(Long accountId) {
        return this.accountStorage.readAccount(accountId);
    }

    /**
     * 搜索指定电话号码的账号。
     *
     * @param phoneNumber
     * @return
     */
    public Account searchAccountWithPhoneNumber(String phoneNumber) {
        return this.accountStorage.readAccountByPhoneNumber(phoneNumber);
    }

    /**
     * 获取有效的验证码图片文件。
     *
     * @return
     */
    public File consumeCaptcha() {
        if (this.captchaQueue.size() <= 3) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    while (captchaQueue.size() < 10) {
                        captchaQueue.offer(createCaptcha());
                    }
                }
            });
        }

        Captcha captcha = this.captchaQueue.poll();
        if (null == captcha) {
            return null;
        }

        synchronized (this.activeCaptchaList) {
            this.activeCaptchaList.add(captcha.code);
        }
        return captcha.file;
    }

    /**
     * 校验验证码。
     *
     * @param code
     * @return
     */
    public boolean checkCaptchaCode(String code) {
        synchronized (this.activeCaptchaList) {
            for (int i = 0; i < this.activeCaptchaList.size(); ++i) {
                String cc = this.activeCaptchaList.get(i);
                if (cc.equalsIgnoreCase(code)) {
                    this.activeCaptchaList.remove(i);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 创建验证码。
     *
     * @return
     */
    private Captcha createCaptcha() {
        DefaultKaptcha dk = new DefaultKaptcha();
        Properties properties = new Properties();

        // 图片边框
        properties.setProperty("kaptcha.border", "yes");
        // 边框颜色
        properties.setProperty("kaptcha.border.color", "110,110,110");
        // 字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        // 背景颜色，渐变开始颜色
        properties.setProperty("kaptcha.background.clear.from", "210,210,210");
        // 背景颜色，渐变结束颜色
        properties.setProperty("kaptcha.background.clear.to", "white");
        // 图片宽
        properties.setProperty("kaptcha.image.width", "140");
        // 图片高
        properties.setProperty("kaptcha.image.height", "31");
        // 字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        // Session key
        properties.setProperty("kaptcha.session.key", "code");
        // 验证码长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 字体
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");
        // 加鱼眼效果
        //properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.FishEyeGimpy");
        // 加水纹效果
        //properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");
        // 加阴影效果
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");
        // 配置
        Config config = new Config(properties);
        dk.setConfig(config);

        String text = Utils.randomNumberString(4);
        BufferedImage image = dk.createImage(text);

        File output = new File("data/captcha_" + text + ".jpg");
        try {
            ImageIO.write(image, "jpg", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Captcha(text, output);
    }

    /**
     * 心跳。
     *
     * @param tokenCode
     * @return
     */
    public boolean heartbeat(String tokenCode) {
        OnlineAccount account = this.onlineTokenMap.get(tokenCode);
        Token token = null;

        if (null == account) {
            token = this.login(tokenCode);
        }
        else {
            token = account.getToken(tokenCode);
        }

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

    /**
     * 是否是有效的会话。
     *
     * @param tokenCode
     * @return
     */
    public boolean isValidToken(String tokenCode) {
        if (this.onlineTokenMap.containsKey(tokenCode)) {
            return true;
        }

        Token token = this.login(tokenCode);
        if (null == token) {
            return false;
        }

        return true;
    }

    /**
     * 获取账号。
     *
     * @param accountId
     * @return
     */
    public Account getAccount(Long accountId) {
        return this.accountCache.getAccount(accountId);
    }

    public Account updateAccount(Long accountId, String newName, String newAvatar) {
        Account account = this.getAccount(accountId);

        if (null != newName) {
            account.name = newName;
        }
        if (null != newAvatar) {
            account.avatar = newAvatar;
        }

        this.accountStorage.updateAccount(accountId, account.name, account.avatar);

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
