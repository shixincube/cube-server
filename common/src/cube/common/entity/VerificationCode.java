/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Cryptology;
import cube.common.JSONable;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * 验证码。
 */
public class VerificationCode implements JSONable {

    public final String token;

    public final String dialCode;

    public final String isoCode;

    public final String phoneNumber;

    public final long timestamp;

    public final long duration;

    private String code;

    private String codeMD5;

    public VerificationCode(String token, String dialCode, String isoCode, String phoneNumber,
                            long timestamp, long duration) {
        this.token = token;
        this.dialCode = dialCode;
        this.isoCode = isoCode;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public VerificationCode(JSONObject json) {
        this.token = json.getString("token");
        this.dialCode = json.getString("dialCode");
        this.isoCode = json.getString("isoCode");
        this.phoneNumber = json.getString("phoneNumber");
        this.timestamp = json.getLong("timestamp");
        this.duration = json.getLong("duration");
        if (json.has("code")) {
            this.code = json.getString("code");
        }
        if (json.has("codeMD5")) {
            this.codeMD5 = json.getString("codeMD5");
        }
    }

    public void setCode(String code) {
        this.code = code;
        this.codeMD5 = Cryptology.getInstance().hashWithMD5AsString(code.getBytes(StandardCharsets.UTF_8));
    }

    public String getCode() {
        return this.code;
    }

    public String getCodeMD5() {
        return this.codeMD5;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("token", this.token);
        json.put("dialCode", this.dialCode);
        json.put("isoCode", this.isoCode);
        json.put("phoneNumber", this.phoneNumber);
        json.put("timestamp", this.timestamp);
        json.put("duration", this.duration);
        if (null != this.code) {
            json.put("code", this.code);
        }
        if (null != this.codeMD5) {
            json.put("codeMD5", this.codeMD5);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("token", this.token);
        json.put("dialCode", this.dialCode);
        json.put("isoCode", this.isoCode);
        json.put("phoneNumber", this.phoneNumber);
        json.put("timestamp", this.timestamp);
        json.put("duration", this.duration);
        if (null != this.codeMD5) {
            json.put("codeMD5", this.codeMD5);
        }
        return json;
    }
}
