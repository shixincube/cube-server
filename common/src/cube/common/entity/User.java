/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.auth.AuthToken;
import org.json.JSONObject;

import java.util.Calendar;

public class User extends Entity {

    public final static String KnowledgeBaseName = "mind_echo";
    public final static String KnowledgeBaseDisplayName = "MindEcho";
    public final static String KnowledgeTitle = "UserProfile";

    private String name;

    private String appAgent;

    private String displayName;

    private String avatar;

    private String phoneNumber;

    private String email;

    private String password;

    private long registerTime;

    private AuthToken authToken;

    private String channel;

    public User(long id, String name, String appAgent, String channel) {
        super(id);
        this.name = name;
        this.appAgent = appAgent;
        this.channel = channel;
        this.displayName = "";
        this.avatar = "";
        this.phoneNumber = "";
        this.email = "";
        this.password = "";
    }

    public User(JSONObject json) {
        super(json);
        this.name = json.getString("name");
        this.appAgent = json.getString("appAgent");
        this.channel = json.has("channel") ? json.getString("channel") : "Unknown";
        this.displayName = json.getString("displayName");
        this.avatar = json.getString("avatar");
        this.phoneNumber = json.has("phoneNumber") ? json.getString("phoneNumber") : "";
        this.email = json.has("email") ? json.getString("email") : "";
        this.password = json.has("password") ? json.getString("password") : "";
        this.registerTime = json.has("registerTime") ? json.getLong("registerTime") : System.currentTimeMillis();
        if (json.has("authToken")) {
            this.authToken = new AuthToken(json.getJSONObject("authToken"));
        }
    }

    public String getChannel() {
        return this.channel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setRegisterTime(long time) {
        this.registerTime = time;
    }

    public long getRegisterTime() {
        return this.registerTime;
    }

    public void setAuthToken(AuthToken authToken) {
        this.authToken = authToken;
    }

    public boolean isRegistered() {
        if (0 == this.registerTime) {
            return false;
        }

        return this.phoneNumber.length() > 8 || this.email.length() > 6;
    }

    public String markdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("# 用户“").append(this.name).append("”的信息\n\n");
        buf.append("* 用户昵称：").append(this.displayName).append("\n");
        if (this.phoneNumber.length() > 1) {
            buf.append("* 用户手机号码：").append(this.phoneNumber).append("\n");
        }
        if (this.email.length() > 1) {
            buf.append("* 用户邮箱：").append(this.email).append("\n");
        }
        // 注册日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.registerTime);
        buf.append("* 用户注册日期：");
        buf.append(calendar.get(Calendar.YEAR)).append("年");
        buf.append(calendar.get(Calendar.MONTH) + 1).append("月");
        buf.append(calendar.get(Calendar.DATE)).append("日\n");
        buf.append("\n");
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        json.put("appAgent", this.appAgent);
        json.put("channel", this.channel);
        json.put("displayName", this.displayName);
        json.put("avatar", this.avatar);
        json.put("phoneNumber", this.phoneNumber);
        json.put("email", this.email);
        json.put("password", this.password);
        json.put("registerTime", this.registerTime);
        if (null != this.authToken) {
            json.put("authToken", this.authToken.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("appAgent");
        json.remove("password");
        return json;
    }
}
