/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

import cell.util.Utils;
import cube.common.entity.Entity;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

/**
 * 域信息。
 */
public class DomainInfo extends Entity {

    /**
     * 一般状态。
     */
    public final static int STATE_NORMAL = 0;

    /**
     * 禁用状态。
     */
    public final static int STATE_DISABLED = 1;

    private long beginning;

    private long duration;

    private int limit;

    private String address;

    private String invitationCode;

    private FileLabel qrCodeFileLabel;

    private int state;

    private int flag;

    public DomainInfo(String domainName, long beginning, long duration, int limit,
                      FileLabel qrCodeFileLabel, int state, int flag) {
        super(Utils.generateSerialNumber(), domainName);
        this.beginning = beginning;
        this.duration = duration;
        this.limit = limit;
        this.qrCodeFileLabel = qrCodeFileLabel;
        this.state = state;
        this.flag = flag;
    }

    public DomainInfo(JSONObject json) {
        super(json);
        this.beginning = json.getLong("beginning");
        this.duration = json.getLong("duration");
        this.limit = json.getInt("limit");

        if (json.has("address")) {
            this.address = json.getString("address");
        }

        if (json.has("invitationCode")) {
            this.invitationCode = json.getString("invitationCode");
        }

        if (json.has("qrCodeFileLabel")) {
            this.qrCodeFileLabel = new FileLabel(json.getJSONObject("qrCodeFileLabel"));
        }

        if (json.has("state")) {
            this.state = json.getInt("state");
        }
        else {
            this.state = STATE_NORMAL;
        }

        if (json.has("flag")) {
            this.flag = json.getInt("flag");
        }
        else {
            this.flag = FerryHouseFlag.STANDARD;
        }
    }

    public long getBeginning() {
        return this.beginning;
    }

    public void setBeginning(long value) {
        this.beginning = value;
    }

    public long getDuration() {
        return this.duration;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getInvitationCode() {
        return this.invitationCode;
    }

    public void setQRCodeFileLabel(FileLabel fileLabel) {
        this.qrCodeFileLabel = fileLabel;
    }

    public FileLabel getQRCodeFileLabel() {
        return this.qrCodeFileLabel;
    }

    public int getState() {
        return this.state;
    }

    public int getFlag() {
        return this.flag;
    }

    public JSONObject toLicence() {
        JSONObject data = new JSONObject();
        data.put("domain", this.domain.getName());
        data.put("beginning", this.beginning);
        data.put("duration", this.duration);
        data.put("limit", this.limit);
        return data;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("beginning", this.beginning);
        json.put("duration", this.duration);
        json.put("limit", this.limit);

        if (null != this.address) {
            json.put("address", this.address);
        }

        if (null != this.invitationCode) {
            json.put("invitationCode", this.invitationCode);
        }

        if (null != this.qrCodeFileLabel) {
            json.put("qrCodeFileLabel", this.qrCodeFileLabel.toJSON());
        }

        json.put("state", this.state);

        json.put("flag", this.flag);

        return json;
    }
}
