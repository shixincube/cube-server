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

package cube.auth;

import cell.util.Cryptology;
import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * 授权令牌。
 */
public class AuthToken implements JSONable {

    /**
     * 令牌的编码。
     */
    private String code;

    /**
     * 令牌的有效域。
     */
    private String domain;

    /**
     * App 关键字。
     */
    private String appKey;

    /**
     * 令牌指定的 Contact ID ，该参数为可选参数。
     */
    private Long cid;

    /**
     * 发布日期。
     */
    private long issues;

    /**
     * 有效期截止日期。
     */
    private long expiry;

    /**
     * 主描述。
     */
    private PrimaryDescription description;

    /**
     * 构造函数。
     *
     * @param code 令牌的编码。
     * @param domain 令牌有效域。
     * @param appKey App 键。
     * @param cid 关联的联系人 ID 。
     * @param issues 令牌发布日期。
     * @param expiry 令牌有效期。
     * @param description 令牌携带的描述。
     */
    public AuthToken(String code, String domain, String appKey, Long cid, Date issues, Date expiry, PrimaryDescription description) {
        this.code = code;
        this.domain = domain;
        this.appKey = appKey;
        this.cid = cid;
        this.issues = issues.getTime();
        this.expiry = expiry.getTime();
        this.description = description;
    }

    /**
     * 构造函数。
     *
     * @param code 令牌的编码。
     * @param domain 令牌有效域。
     * @param appKey App 键。
     * @param cid 关联的联系人 ID 。
     * @param issues 令牌发布日期。
     * @param expiry 令牌有效期。
     */
    public AuthToken(String code, String domain, String appKey, Long cid, long issues, long expiry) {
        this.code = code;
        this.domain = domain;
        this.appKey = appKey;
        this.cid = cid;
        this.issues = issues;
        this.expiry = expiry;
    }

    /**
     * 构造函数。
     *
     * @param json 描述令牌的 JSON 对象。
     */
    public AuthToken(JSONObject json) {
        try {
            this.code = json.getString("code");
            this.domain = json.getString("domain");
            this.appKey = json.getString("appKey");
            this.cid = json.getLong("cid");
            this.issues = json.getLong("issues");
            this.expiry = json.getLong("expiry");
            if (json.has("description")) {
                this.description = new PrimaryDescription(json.getJSONObject("description"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取令牌编码。
     *
     * @return 返回令牌编码。
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 获取域名称。
     *
     * @return 返回域名称。
     */
    public String getDomain() {
        return this.domain;
    }

    /**
     * 获取超期时间。
     *
     * @return 返回超期时间。
     */
    public long getExpiry() {
        return this.expiry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("code", this.code);
            json.put("domain", this.domain);
            json.put("appKey", this.appKey);
            json.put("cid", this.cid.longValue());
            json.put("issues", this.issues);
            json.put("expiry", this.expiry);
            if (null != this.description) {
                json.put("description", this.description.toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 生成 Base64 字符串。
     *
     * @return 返回 Base64 数据的字符串。
     */
    public String toBase64() {
        JSONObject json = this.toJSON();
        String string = json.toString();
        byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(string.getBytes(), new byte[]{'S','X','c','u','b','e','3','0'});
        String base64 = Cryptology.getInstance().encodeBase64(ciphertext);
        return base64;
    }
}
