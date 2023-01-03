/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.auth;

import cell.core.net.Endpoint;
import cell.util.Utils;
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.common.action.ClientAction;
import cube.common.entity.AuthDomain;
import cube.common.entity.IceServer;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权服务。
 */
public class AuthService extends AbstractModule {

    public static String NAME = "Auth";

    private boolean useFile = false;

    private AuthServicePluginSystem pluginSystem;

    private AuthDomainFile authDomainFile;

    private PrimaryContentFile primaryContentFile;

    private AuthStorage authStorage;

    private Cache tokenCache;

    private ConcurrentHashMap<String, AuthToken> authTokenMap;

    private ConcurrentHashMap<String, AuthDomainSet> authDomainMap;

    public AuthService() {
        this.authTokenMap = new ConcurrentHashMap<>();
        this.authDomainMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        // 获取令牌池缓存器
        this.tokenCache = this.getCache("TokenPool");

        if (this.useFile) {
            // 文件数据库，该库仅用于测试
            this.authDomainFile = new AuthDomainFile("config/domain.json");

            // 存储主内容的文件
            this.primaryContentFile = new PrimaryContentFile("config/primary-content.json");
        }
        else {
            // 读取存储配置
            JSONObject config = ConfigUtils.readStorageConfig();
            if (config.has(AuthService.NAME)) {
                config = config.getJSONObject(AuthService.NAME);
                if (config.getString("type").equalsIgnoreCase("SQLite")) {
                    this.authStorage = new AuthStorage(StorageType.SQLite, config);
                }
                else {
                    this.authStorage = new AuthStorage(StorageType.MySQL, config);
                }
            }
            else {
                config.put("file", "storage/AuthService.db");
                this.authStorage = new AuthStorage(StorageType.SQLite, config);
            }
            // 开启存储
            this.authStorage.open();
            this.authStorage.execSelfChecking(null);
        }

        // 创建插件系统
        this.pluginSystem = new AuthServicePluginSystem();
    }

    @Override
    public void stop() {
        if (null != this.authStorage) {
            this.authStorage.close();
        }
    }

    @Override
    public AuthServicePluginSystem getPluginSystem() {
        return this.pluginSystem;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        // Nothing
    }

    /**
     * 返回所有域的清单。
     *
     * @return 返回所有域的清单。
     */
    public List<String> getDomainList() {
        if (this.useFile) {
            List<String> result = new ArrayList<>();
            for (AuthDomain authDomain : this.authDomainFile.getAuthDomainList()) {
                result.add(authDomain.domainName);
            }
            return result;
        }
        else {
            int count = 5;
            while (null == this.authStorage) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (--count <= 0) {
                    break;
                }
            }

            if (null == this.authStorage) {
                return null;
            }

            List<String> result = this.authStorage.listDomains();
            if (null == result) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                result = this.authStorage.listDomains();
            }
            return result;
        }
    }

    /**
     * 申请令牌。
     *
     * @param domain 指定域名称。
     * @param appKey 指定 App Key 值。
     * @param cid 指定绑定的 CID 。
     * @return 返回令牌。
     */
    public AuthToken applyToken(String domain, String appKey, Long cid) {
        return this.applyToken(domain, appKey, cid, 7L * 24 * 60 * 60 * 1000);
    }

    /**
     * 申请令牌。
     *
     * @param domain 指定域名称。
     * @param appKey 指定 App Key 值。
     * @param cid 指定绑定的 CID 。
     * @param durationInMillis 指定令牌的有效时长，单位：毫秒。
     * @return 返回令牌。
     */
    public AuthToken applyToken(String domain, String appKey, long cid, long durationInMillis) {
        AuthToken token = null;

        if (this.useFile) {
            AuthDomain authDomain = this.authDomainFile.queryDomain(domain, appKey);
            if (null != authDomain) {
                String code = Utils.randomString(32);

                Date now = new Date();
                Date expiry = new Date(now.getTime() + durationInMillis);

                // 创建描述
                PrimaryDescription description = new PrimaryDescription("127.0.0.1", this.primaryContentFile.getContent(domain));

                token = new AuthToken(code, domain, appKey, cid, now, expiry, description, authDomain.ferry);

                // 本地缓存
                this.authTokenMap.put(code, token);

                // 将 Code 写入令牌池
                this.tokenCache.put(new CacheKey(code), new CacheValue(token.toJSON()));

                // 更新文件数据库
                authDomain.tokens.put(code, token);
                this.authDomainFile.update();
            }
        }
        else {
            // 读取该 App 的访问域
            AuthDomain authDomain = this.authStorage.getDomain(domain, appKey);
            if (null != authDomain) {
                if (cid > 0) {
                    // 查询令牌
                    token = this.authStorage.queryToken(cid);

                    if (null != token) {
                        if (token.getExpiry() < System.currentTimeMillis()) {
                            // 已过期的令牌
                            token = null;
                        }
                    }
                }

                if (null == token) {
                    String code = Utils.randomString(32);

                    Date now = new Date();
                    Date expiry = new Date(now.getTime() + durationInMillis);

                    // 创建描述
                    PrimaryDescription description = authDomain.getPrimaryDescription();

                    // 创建令牌
                    token = new AuthToken(code, domain, appKey, cid, now, expiry, description, authDomain.ferry);

                    // 本地缓存
                    this.authTokenMap.put(code, token);

                    // 将 Code 写入令牌池
                    this.tokenCache.put(new CacheKey(code), new CacheValue(token.toJSON()));

                    // 更新存储
                    this.authStorage.writeToken(token);
                }
            }
        }

        return token;
    }

    /**
     * 将令牌里的 CID 进行绑定。
     *
     * @param token
     * @return
     */
    public boolean bindToken(AuthToken token) {
        if (token.getContactId() == 0) {
            return false;
        }

        this.authStorage.updateToken(token);
        return true;
    }

    /**
     * 注入令牌。
     *
     * @param token
     * @return
     */
    public void injectToken(AuthToken token) {
        if (this.authStorage.existsToken(token.getCode())) {
            this.authStorage.updateToken(token);
        }
        else {
            this.authStorage.writeToken(token);
        }
    }

    /**
     * 通过编码获取令牌。
     *
     * @param code 指定令牌码。
     * @return 返回令牌码对应的令牌。
     */
    public AuthToken getToken(String code) {
        AuthToken token = this.authTokenMap.get(code);
        if (null != token) {
            return token;
        }

        CacheValue value = this.tokenCache.get(new CacheKey(code));
        if (null != value) {
            token = new AuthToken(value.get());
            return token;
        }

        if (this.useFile) {
            token = this.authDomainFile.queryToken(code);
        }
        else {
            token = this.authStorage.readToken(code);
        }

        if (null != token) {
            this.authTokenMap.put(code, token);
        }

        return token;
    }

    /**
     * 获取授权域。
     *
     * @param domain 指定域名称。
     * @return 返回授权域实例。
     */
    public AuthDomain getAuthDomain(String domain) {
        return this.authStorage.getDomain(domain, null);
    }

    /**
     * 获取授权域。
     *
     * @param domain 指定域名称。
     * @param appKey 指定域的 App Key 。
     * @return 返回授权域实例。
     */
    public AuthDomain getAuthDomain(String domain, String appKey) {
        return this.authStorage.getDomain(domain, appKey);
    }

    /**
     * 获取指定域的主描述。
     *
     * @param domain 指定域名称。
     * @param appKey 指定 App Key 。
     * @return 返回指定域的主描述。
     */
    public PrimaryDescription getPrimaryDescription(String domain, String appKey) {
        if (this.useFile) {
             return new PrimaryDescription("127.0.0.1", this.primaryContentFile.getContent(domain));
        }
        else {
            AuthDomainSet authDomainSet = this.authDomainMap.get(domain);
            if (null == authDomainSet) {
                authDomainSet = new AuthDomainSet(domain);
                this.authDomainMap.put(domain, authDomainSet);
            }

            if (null == appKey) {
                // 兼容之前不使用 App Key 查询域的调用方式
                AuthDomain authDomain = this.authStorage.getDomain(domain, null);
                if (null == authDomain) {
                    return null;
                }
                return authDomain.getPrimaryDescription();
            }

            AuthDomain authDomain = authDomainSet.getAuthDomain(appKey);
            if (null == authDomain) {
                authDomain = this.authStorage.getDomain(domain, appKey);
                if (null != authDomain) {
                    authDomainSet.addAuthDomain(authDomain);
                }
            }

            if (null != authDomain) {
                return authDomain.getPrimaryDescription();
            }

            return null;
        }
    }

    /**
     * 通过联系人 ID 查找授权令牌。
     *
     * @param contactId
     * @return
     */
    public AuthToken queryAuthTokenByContactId(Long contactId) {
        return this.authStorage.queryToken(contactId);
    }

    @Override
    public Object notify(Object data) {
        JSONObject json = (JSONObject) data;
        String action = json.getString("action");

        if (ClientAction.GetAuthToken.name.equals(action)) {
            String tokenCode = json.getString("token");
            AuthToken authToken = this.getToken(tokenCode);
            if (null != authToken) {
                return authToken.toJSON();
            }
        }

        return null;
    }

    /**
     * 是否存在指定的域。
     *
     * @param domainName
     * @return
     */
    public boolean hasDomain(String domainName) {
        return this.authStorage.existsDomain(domainName);
    }

    /**
     * 创建新的访问域。
     *
     * @param domainName 指定域名称。
     * @param appKey 指定 App Key 。
     * @param appId 指定 App ID 。
     * @param mainEndpoint 指定主接入点。
     * @param httpEndpoint 指定 HTTP 接入点。
     * @param httpsEndpoint 指定 HTTPS 接入点。
     * @param iceServers 指定 ICE 服务器列表。
     * @param ferry 指定是否是摆渡域。
     * @return 返回创建的域。如果域重复或者创建失败返回 {@code null} 值。
     */
    public AuthDomain createDomainApp(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                                      Endpoint httpEndpoint, Endpoint httpsEndpoint, List<IceServer> iceServers,
                                      boolean ferry) {
        // 判断是否有重复的 App
        AuthDomain authDomain = this.authStorage.getDomain(domainName, appKey);
        if (null != authDomain) {
            // 已经存在指定的 App
            return null;
        }

        if (null == iceServers) {
            iceServers = new ArrayList<>();
            iceServers.add(new IceServer("turn:52.83.195.35:3478", "cube", "cube887"));
        }

        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < iceServers.size(); ++i) {
            jsonArray.put(iceServers.get(i).toJSON());
        }

        // 新增数据库记录
        this.authStorage.addDomainApp(domainName, appId, appKey, mainEndpoint,
                httpEndpoint, httpsEndpoint, jsonArray, ferry);

        authDomain = new AuthDomain(domainName, appKey, appId, mainEndpoint,
                httpEndpoint, httpsEndpoint, jsonArray, ferry);

        // Hook
        AuthServiceHook hook = this.pluginSystem.getCreateDomainAppHook();
        hook.apply(new AuthPluginContext(authDomain));

        return authDomain;
    }

    /**
     * 更新指定域的接入点数据。
     *
     * @param domainName
     * @param mainEndpoint
     * @param httpEndpoint
     * @param httpsEndpoint
     * @return
     */
    public AuthDomain updateDomain(String domainName, Endpoint mainEndpoint,
                                Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        if (this.authStorage.updateDomain(domainName, mainEndpoint, httpEndpoint, httpsEndpoint)) {
            return this.getAuthDomain(domainName);
        }
        else {
            return null;
        }
    }

    private JSONObject createPrimaryContent() {
        JSONObject result = new JSONObject();

        try {
            JSONObject fileStorageConfig = new JSONObject();
            fileStorageConfig.put("fileURL", "http://127.0.0.1:7010/filestorage/file/");
            fileStorageConfig.put("fileSecureURL", "https://127.0.0.1:7017/filestorage/file/");

            result.put("FileStorage", fileStorageConfig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
