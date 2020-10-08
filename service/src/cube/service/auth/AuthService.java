/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.cache.SMCacheKey;
import cube.cache.SMCacheValue;
import cube.core.AbstractModule;
import cube.core.Cache;
import cube.core.CacheValue;
import cube.core.MessageQueue;

import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权服务。
 */
public class AuthService extends AbstractModule {

    public static String NAME = "Auth";

    private List<AuthDomain> authDomainList;

    private Cache tokenCache;

    private ConcurrentHashMap<String, AuthToken> authTokenMap;

    public AuthService() {
        this.authTokenMap = new ConcurrentHashMap<>();
        this.authDomainList = new Vector<>();
    }

    @Override
    public void start() {
        this.tokenCache = this.getCache("TokenPool");

        AuthDomainFile adFile = new AuthDomainFile("config/domain.json");
        this.authDomainList.addAll(adFile.getList());
    }

    @Override
    public void stop() {
    }

    /**
     * 申请令牌。
     * @param domain
     * @param appKey
     * @param cid
     * @return
     */
    public AuthToken applyToken(String domain, String appKey, Long cid) {
        AuthToken token = null;

        List<String> codes = this.findTokenCode(domain, appKey);
        if (null != codes) {
            String code = codes.get(0);

            Date now = new Date();
            Date expiry = new Date(now.getTime() + 7L * 24L * 60L * 60L * 1000L);

            // 创建描述
            PrimaryDescription description = new PrimaryDescription("127.0.0.1", createPrimaryContent());

            token = new AuthToken(code, domain, appKey, cid, now, expiry, description);

            // 本地缓存
            this.authTokenMap.put(code, token);

            // 将 Code 写入令牌池
            this.tokenCache.put(new SMCacheKey(code), new SMCacheValue(token.toJSON()));
        }
        else {
            // TODO 从数据库中查询
        }

        return token;
    }

    /**
     * 通过编码获取令牌。
     * @param code
     * @return
     */
    public AuthToken getToken(String code) {
        AuthToken token = this.authTokenMap.get(code);
        if (null != token) {
            return token;
        }

        token = this.findOrCreateToken(code);
        if (null != token) {
            return token;
        }

        CacheValue value = this.tokenCache.get(new SMCacheKey(code));
        if (null != value) {
            SMCacheValue smv = (SMCacheValue) value;
            token = new AuthToken(smv.get());
        }

        return token;
    }

    private List<String> findTokenCode(String domain, String appKey) {
        List<String> result = null;
        for (AuthDomain ad : this.authDomainList) {
            if (ad.domainName.equalsIgnoreCase(domain) && ad.appKey.equalsIgnoreCase(appKey)) {
                result = ad.codes;
                break;
            }
        }
        return result;
    }

    private AuthToken findOrCreateToken(String code) {
        AuthToken result = null;
        for (AuthDomain ad : this.authDomainList) {
            List<String> codes = ad.codes;
            if (codes.contains(code)) {
                Date now = new Date();
                Date expiry = new Date(now.getTime() + 7L * 24L * 60L * 60L * 1000L);

                // 创建描述
                PrimaryDescription description = new PrimaryDescription("127.0.0.1", createPrimaryContent());

                result = new AuthToken(code, ad.domainName, ad.appKey, 0L, now, expiry, description);

                // 本地缓存
                this.authTokenMap.put(code, result);
                break;
            }
        }
        return result;
    }

    private JSONObject createPrimaryContent() {
        JSONObject result = new JSONObject();

        try {
            JSONObject fileStorageConfig = new JSONObject();
            fileStorageConfig.put("uploadURL", "http://127.0.0.1:9090/api/v1/upload");

            result.put("FileStorage", fileStorageConfig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
