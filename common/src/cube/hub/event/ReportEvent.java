/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 状态报告。
 */
public class ReportEvent extends WeChatEvent {

    public final static String NAME = "Report";

    /**
     * 可管理的 App 总数。
     */
    private int totalAppNum;

    /**
     * 空闲的 App 数量。
     */
    private int idleAppNum;

    /**
     * 已管理的账号。
     */
    private List<Contact> managedAccounts;

    /**
     * 频道码映射。
     */
    private Map<String, Contact> channelCodeMap;

    public ReportEvent(int totalAppNum, int idleAppNum, List<Contact> managedAccounts) {
        super(NAME);
        this.totalAppNum = totalAppNum;
        this.idleAppNum = idleAppNum;
        this.managedAccounts = managedAccounts;
        this.channelCodeMap = new HashMap<>();
    }

    public ReportEvent(JSONObject json) {
        super(json);
        this.totalAppNum = json.getInt("total");
        this.idleAppNum = json.getInt("idle");

        this.managedAccounts = new ArrayList<>();
        JSONArray array = json.getJSONArray("accounts");
        for (int i = 0; i < array.length(); ++i) {
            Contact account = new Contact(array.getJSONObject(i));
            this.managedAccounts.add(account);
        }

        this.channelCodeMap = new HashMap<>();
        if (json.has("channelCodes")) {
            JSONObject map = json.getJSONObject("channelCodes");
            for (String key : map.keySet()) {
                JSONObject value = map.getJSONObject(key);
                this.channelCodeMap.put(key, new Contact(value));
            }
        }
    }

    public int getTotalAppNum() {
        return this.totalAppNum;
    }

    public int getIdleAppNum() {
        return this.idleAppNum;
    }

    public List<Contact> getManagedAccounts() {
        return this.managedAccounts;
    }

    public void addManagedAccount(Contact account) {
        this.managedAccounts.add(account);
    }

    public void removeManagedAccount(Contact account) {
        synchronized (this) {
            for (int i = 0; i < this.managedAccounts.size(); ++i) {
                Contact current = this.managedAccounts.get(i);
                if (account.getExternalId().equals(current.getExternalId())) {
                    this.managedAccounts.remove(i);
                    // 空闲 App 数量增加
                    ++this.idleAppNum;
                    break;
                }
            }
        }
    }

    public boolean hasManagedAccount(Contact account) {
        synchronized (this) {
            for (int i = 0; i < this.managedAccounts.size(); ++i) {
                Contact current = this.managedAccounts.get(i);
                if (account.getExternalId().equals(current.getExternalId())) {
                    return true;
                }
            }

            return false;
        }
    }

    public void putChannelCode(String channelCode, Contact account) {
        this.channelCodeMap.put(channelCode, account);
    }

    public Contact removeChannelCode(String channelCode) {
        return this.channelCodeMap.remove(channelCode);
    }

    public Contact getAccount(String channelCode) {
        return this.channelCodeMap.get(channelCode);
    }

    public boolean hasChannelCode(String channelCode) {
        return this.channelCodeMap.containsKey(channelCode);
    }

    public Map<String, Contact> getChannelCodes() {
        return this.channelCodeMap;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("total", this.totalAppNum);
        json.put("idle", this.idleAppNum);

        JSONArray array = new JSONArray();
        for (Contact account : this.managedAccounts) {
            array.put(account.toCompactJSON());
        }
        json.put("accounts", array);

        if (!this.channelCodeMap.isEmpty()) {
            JSONObject map = new JSONObject();
            for (Map.Entry<String, Contact> entry : this.channelCodeMap.entrySet()) {
                map.put(entry.getKey(), entry.getValue().toCompactJSON());
            }
            json.put("channelCodes", map);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("total", this.totalAppNum);
        json.put("idle", this.idleAppNum);

        JSONArray array = new JSONArray();
        for (Contact account : this.managedAccounts) {
            array.put(account.toCompactJSON());
        }
        json.put("accounts", array);
        return json;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n");
        buf.append("---------------- Report ----------------\n");

        if (null != this.getDescription()) {
            buf.append("Pretender : ");
            buf.append(this.getDescription().getPretender().getId());
            buf.append("\n");
        }

        buf.append("Statistics: ");
        buf.append(this.totalAppNum - this.idleAppNum);
        buf.append(" / ");
        buf.append(this.totalAppNum);
        buf.append("\n");

        if (!this.channelCodeMap.isEmpty()) {
            for (Map.Entry<String, Contact> entry : this.channelCodeMap.entrySet()) {
                buf.append(entry.getKey());
                buf.append(" -> ");
                buf.append(entry.getValue().getName());
                buf.append("\n");
            }
        }

        buf.append("----------------------------------------\n");
        return buf.toString();
    }
}
