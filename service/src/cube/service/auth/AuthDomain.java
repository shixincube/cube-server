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

package cube.service.auth;

import cube.auth.AuthToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储域对应的授权信息的类。
 */
public class AuthDomain {

    public String domainName;

    public String appKey;

    public ConcurrentHashMap<String, AuthToken> tokens;

    public AuthDomain(String domainName, String appKey, JSONArray array) {
        this.domainName = domainName;
        this.appKey = appKey;
        this.tokens = new ConcurrentHashMap<>();

        try {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                String code = json.getString("code");
                long cid = json.getLong("cid");
                long issues = json.getLong("issues");
                long expiry = json.getLong("expiry");

                AuthToken token = new AuthToken(code, domainName, appKey, cid, issues, expiry);
                this.tokens.put(code, token);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
