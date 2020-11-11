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
import cube.core.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权服务。
 */
public class AuthService extends AbstractModule {

    public static String NAME = "Auth";

    private AuthDomainFile authDomainFile;

    private PrimaryContentFile primaryContentFile;

    private Cache tokenCache;

    private ConcurrentHashMap<String, AuthToken> authTokenMap;

    public AuthService() {
        this.authTokenMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        this.tokenCache = this.getCache("TokenPool");

        // 文件数据库，该库仅用于测试
        this.authDomainFile = new AuthDomainFile("config/domain.json");

        // 存储主内容的文件
        this.primaryContentFile = new PrimaryContentFile("config/primary-content.json");
    }

    @Override
    public void stop() {
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    /**
     * 返回所有域的清单。
     *
     * @return
     */
    public List<String> getDomainList() {
        List<String> result = new ArrayList<>();
        for (AuthDomain authDomain : this.authDomainFile.getAuthDomainList()) {
            result.add(authDomain.domainName);
        }
        return result;
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

        AuthDomain authDomain = this.authDomainFile.queryDomain(domain, appKey);
        if (null != authDomain) {
            String code = Utils.randomString(32);

            Date now = new Date();
            Date expiry = new Date(now.getTime() + 7L * 24L * 60L * 60L * 1000L);

            // 创建描述
            PrimaryDescription description = new PrimaryDescription("127.0.0.1", this.primaryContentFile.getContent(domain));

            token = new AuthToken(code, domain, appKey, cid, now, expiry, description);

            // 本地缓存
            this.authTokenMap.put(code, token);

            // 将 Code 写入令牌池
            this.tokenCache.put(new CacheKey(code), new CacheValue(token.toJSON()));

            // 更新文件数据库
            authDomain.tokens.put(code, token);
            this.authDomainFile.update();
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

        CacheValue value = this.tokenCache.get(new CacheKey(code));
        if (null != value) {
            token = new AuthToken(value.get());
            return token;
        }

        token = this.authDomainFile.queryToken(code);
        if (null != token) {
            this.authTokenMap.put(code, token);
        }

        return token;
    }

    /**
     * 返回指定域的主描述内容。
     *
     * @param domain
     * @return
     */
    public JSONObject getPrimaryContent(String domain) {
        return this.primaryContentFile.getContent(domain);
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
