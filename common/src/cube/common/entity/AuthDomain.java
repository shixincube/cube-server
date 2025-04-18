/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    /**
     * 域名称。
     */
    public final String domainName;

    /**
     * App Key 。
     */
    public final String appKey;

    /**
     * App ID 。
     */
    public final String appId;

    /**
     * 主接入点。
     */
    public final Endpoint mainEndpoint;

    public final Endpoint httpEndpoint;

    public final Endpoint httpsEndpoint;

    public final JSONArray iceServers;

    /**
     * 是否工作在摆渡数据模式。
     */
    public final boolean ferry;

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
     * @param ferry
     */
    public AuthDomain(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                      Endpoint httpEndpoint, Endpoint httpsEndpoint, JSONArray iceServers,
                      boolean ferry) {
        this.domainName = domainName;
        this.appKey = appKey;
        this.appId = appId;
        this.mainEndpoint = mainEndpoint;
        this.httpEndpoint = httpEndpoint;
        this.httpsEndpoint = httpsEndpoint;
        this.iceServers = iceServers;
        this.ferry = ferry;
        this.tokens = null;
    }

    /**
     * 构造函数。
     *
     * @param domainName
     * @param appKey
     * @param array
     * @param ferry
     */
    public AuthDomain(String domainName, String appKey, JSONArray array, boolean ferry) {
        this.domainName = domainName;
        this.appKey = appKey;
        this.appId = null;
        this.mainEndpoint = null;
        this.httpEndpoint = null;
        this.httpsEndpoint = null;
        this.iceServers = null;
        this.ferry = ferry;
        this.tokens = new ConcurrentHashMap<>();

        try {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                String code = json.getString("code");
                long cid = json.getLong("cid");
                long issue = json.getLong("issue");
                long expiry = json.getLong("expiry");

                AuthToken token = new AuthToken(code, domainName, appKey, cid, issue, expiry, ferry);
                this.tokens.put(code, token);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public AuthDomain(JSONObject json) {
        this.domainName = json.getString("domainName");
        this.appKey = json.getString("appKey");
        this.appId = json.has("appId") ? json.getString("appId") : null;
        this.mainEndpoint = json.has("mainEndpoint") ? new Endpoint(json.getJSONObject("mainEndpoint")) : null;
        this.httpEndpoint = json.has("httpEndpoint") ? new Endpoint(json.getJSONObject("httpEndpoint")) : null;
        this.httpsEndpoint = json.has("httpsEndpoint") ? new Endpoint(json.getJSONObject("httpsEndpoint")) : null;
        this.iceServers = json.has("iceServers") ? json.getJSONArray("iceServers") : null;
        this.ferry = json.has("ferry") && json.getBoolean("ferry");
    }

    public Endpoint getMainEndpoint() {
        return this.mainEndpoint;
    }

    public Endpoint getHttpEndpoint() {
        return this.httpEndpoint;
    }

    public Endpoint getHttpsEndpoint() {
        return this.httpsEndpoint;
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

    public boolean isFerry() {
        return this.ferry;
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
        json.put("ferry", this.ferry);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
