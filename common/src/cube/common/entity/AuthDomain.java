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

package cube.common.entity;

import cell.core.net.Endpoint;
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储域对应的授权信息的类。
 */
public class AuthDomain implements JSONable {

    public final String domainName;

    public final String appKey;

    public final String appId;

    public final Endpoint mainEndpoint;

    public final Endpoint httpEndpoint;

    public final Endpoint httpsEndpoint;

    public final JSONArray iceServers;

    public ConcurrentHashMap<String, AuthToken> tokens;

    private PrimaryDescription description;

    /**
     * 构造函数。
     *
     * @param domainName
     * @param appKey
     * @param appId
     * @param mainEndpoint
     * @param httpEndpoint
     * @param httpsEndpoint
     * @param iceServers
     */
    public AuthDomain(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                      Endpoint httpEndpoint, Endpoint httpsEndpoint, JSONArray iceServers) {
        this.domainName = domainName;
        this.appKey = appKey;
        this.appId = appId;
        this.mainEndpoint = mainEndpoint;
        this.httpEndpoint = httpEndpoint;
        this.httpsEndpoint = httpsEndpoint;
        this.iceServers = iceServers;
        this.tokens = null;
    }

    /**
     * 构造函数。
     *
     * @param domainName
     * @param appKey
     * @param array
     */
    public AuthDomain(String domainName, String appKey, JSONArray array) {
        this.domainName = domainName;
        this.appKey = appKey;
        this.appId = null;
        this.mainEndpoint = null;
        this.httpEndpoint = null;
        this.httpsEndpoint = null;
        this.iceServers = null;
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

    public PrimaryDescription getPrimaryDescription() {
        if (null != this.description) {
            return this.description;
        }

        JSONObject content = new JSONObject();

        // FileStorage
        JSONObject fileStorage = new JSONObject();
        fileStorage.put("fileURL", "http://" + this.httpEndpoint.getHost() + ":" + this.httpEndpoint.getPort()
                + "/filestorage/file/");
        fileStorage.put("fileSecureURL", "https://" + this.httpsEndpoint.getHost() + ":" + this.httpsEndpoint.getPort()
                + "/filestorage/file/");
        content.put("FileStorage", fileStorage);

        // MultipointComm
        JSONObject multiComm = new JSONObject();
        multiComm.put("iceServers", this.iceServers);
        content.put("MultipointComm", multiComm);

        this.description = new PrimaryDescription(this.mainEndpoint.getHost(), content);
        return this.description;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("domainName", this.domainName);
        json.put("appKey", this.appKey);
        if (null != this.appId) {
            json.put("appId", this.appId);
        }
        if (null != this.mainEndpoint) {
            json.put("mainEndpoint", this.mainEndpoint.toJSON());
        }
        if (null != this.httpEndpoint) {
            json.put("httpEndpoint", this.httpEndpoint.toJSON());
        }
        if (null != this.httpsEndpoint) {
            json.put("httpsEndpoint", this.httpsEndpoint.toJSON());
        }
        if (null != this.iceServers) {
            json.put("iceServers", this.iceServers);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
