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

package cube.app.server.applet;

import cell.util.log.Logger;
import cube.app.server.account.Account;
import cube.util.ConfigUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序接口实现。
 */
public class WeChatApplet implements WeChatAppletAPI {

    private final static WeChatApplet instance = new WeChatApplet();

    private String appletAppId;
    private String appletAppSecret;

    private AppletStorage storage;

    private WeChatApplet() {
    }

    public static WeChatApplet getInstance() {
        return WeChatApplet.instance;
    }

    public void start() {
        this.loadConfig();

        this.storage.open();
    }

    public void destroy() {
        this.storage.close();
    }

    /**
     * 使用 js code 查询对应的账号 ID 。
     *
     * @param jsCode
     * @return
     */
    public long checkAccount(String jsCode) {
        long accountId = this.storage.queryAccountIdByJsCode(jsCode);
        if (-1 == accountId) {
            // 没有记录，从微信服务器获取 Open ID
            JSONObject sessionJson = this.code2session(jsCode);
            if (null != sessionJson && sessionJson.has("openid")) {
                this.storage.writeSessionCode(jsCode, sessionJson.getString("openid"),
                        sessionJson.getString("session_key"),
                        sessionJson.has("unionid") ? sessionJson.getString("unionid") : null);
            }
            else {
                Logger.w(this.getClass(), "#checkAccount - Get session from WeChat server failed: " + jsCode);
            }
        }

        return accountId;
    }

    /**
     * 将 js code 对应的 Open ID 与指定账号进行绑定。
     *
     * @param jsCode
     * @param account
     * @param device
     * @return
     */
    public boolean bind(String jsCode, Account account, String device) {
        long accountId = this.storage.queryAccountIdByJsCode(jsCode);
        if (-1 == accountId) {
            JSONObject sessionJson = this.code2session(jsCode);
            if (null != sessionJson && sessionJson.has("openid")) {
                // 写 Open ID
                this.storage.writeSessionCode(jsCode, sessionJson.getString("openid"),
                        sessionJson.getString("session_key"),
                        sessionJson.has("unionid") ? sessionJson.getString("unionid") : null);
                // 写账号
                this.storage.writeAccountSession(accountId, device, jsCode);
                return true;
            }
            else {
                String errmsg = sessionJson.has("errmsg") ? sessionJson.getString("errmsg") : "";
                Logger.w(this.getClass(), "#bind - WeChat API respond error: " + errmsg);
                return false;
            }
        }
        else if (0 == accountId) {
            // 已经有 Open ID 记录
            this.storage.writeAccountSession(account.id, device, jsCode);
            return true;
        }
        else {
            if (accountId == account.id) {
                // 账号一致
                return true;
            }
            else {
                Logger.w(this.getClass(), "#bind - Account id error: " + account.id + " - " + accountId);
                return false;
            }
        }
    }

    @Override
    public JSONObject code2session(String jsCode) {
        StringBuilder buf = new StringBuilder("https://api.weixin.qq.com/sns/jscode2session");
        buf.append("?appid=");
        buf.append(this.appletAppId);
        buf.append("&secret=");
        buf.append(this.appletAppSecret);
        buf.append("&js_code=");
        buf.append(jsCode);
        buf.append("&grant_type=authorization_code");

        JSONObject result = null;

        SslContextFactory sslContextFactory = new SslContextFactory.Client(true);
        HttpClient client = new HttpClient(sslContextFactory);
        try {
            client.start();
            ContentResponse response = client.newRequest(buf.toString())
                    .method(HttpMethod.GET)
                    .timeout(15, TimeUnit.SECONDS)
                    .send();

            if (HttpStatus.OK_200 == response.getStatus()) {
                result = new JSONObject(response.getContentAsString().trim());
            }
            else {
                Logger.i(this.getClass(), "#code2session - Response status: " + response.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
            }
        }

        return result;
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
                this.storage = new AppletStorage(properties);

                // 加载 Applet 信息
                this.appletAppId = properties.getProperty("applet.appId");
                this.appletAppSecret = properties.getProperty("applet.appSecret");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
