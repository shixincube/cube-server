/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 报告基类。
 */
public abstract class Report implements JSONable {

    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHH");

    public final long sn;

    public final long contactId;

    public final long timestamp;

    protected ReportPermission permission;

    protected String name;

    protected Attribute attribute;

    protected String summary = "";

    public Report(long contactId, Attribute attribute) {
        this.sn = Utils.generateSerialNumber();
        this.contactId = contactId;
        this.permission = ReportPermission.createAllPermissions(this.contactId, this.sn);
        this.timestamp = System.currentTimeMillis();
        this.name = "AXL-" + sDateFormat.format(new Date(this.timestamp)) +
                String.format("%04d", Utils.randomInt(1, 9999));
        this.attribute = attribute;
    }

    public Report(long sn, long contactId, long timestamp, Attribute attribute) {
        this.sn = sn;
        this.contactId = contactId;
        this.permission = ReportPermission.createAllPermissions(this.contactId, this.sn);
        this.timestamp = timestamp;
        this.name = "AXL-" + sDateFormat.format(new Date(this.timestamp)) +
                String.format("%04d", Utils.randomInt(1, 9999));
        this.attribute = attribute;
    }

    public Report(long sn, long contactId, long timestamp, String name, Attribute attribute) {
        this.sn = sn;
        this.contactId = contactId;
        this.permission = ReportPermission.createAllPermissions(this.contactId, this.sn);
        this.timestamp = timestamp;
        this.name = name;
        this.attribute = attribute;
    }

    public Report(JSONObject json) {
        this.sn = json.getLong("sn");
        this.contactId = json.getLong("contactId");
        this.permission = json.has("permission") ?
                new ReportPermission(json.getJSONObject("permission")) :
                ReportPermission.createAllPermissions(this.contactId, this.sn);
        this.timestamp = json.getLong("timestamp");
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        if (json.has("summary")) {
            this.summary = json.getString("summary");
        }
    }

    public String getName() {
        return this.name;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setPermission(ReportPermission permission) {
        this.permission = permission;
    }

    public ReportPermission getPermission() {
        return this.permission;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("timestamp", this.timestamp);
        json.put("name", this.name);
        json.put("permission", this.permission.toJSON());
        json.put("attribute", this.attribute.toJSON());
        if (null != this.summary) {
            json.put("summary", this.summary);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("timestamp", this.timestamp);
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());
        if (null != this.summary) {
            json.put("summary", this.summary);
        }
        return json;
    }
}
