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

        if (json.has("channelCodes")) {
            JSONObject map = json.getJSONObject("channelCodes");
            this.channelCodeMap = new HashMap<>();
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
        this.managedAccounts.remove(account);
    }

    public void putChannelCode(String channelCode, Contact account) {
        if (null == this.channelCodeMap) {
            this.channelCodeMap = new HashMap<>();
        }

        this.channelCodeMap.put(channelCode, account);
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

        if (null != this.channelCodeMap) {
            JSONObject map = new JSONObject();
            for (Map.Entry<String, Contact> entry : this.channelCodeMap.entrySet()) {
                map.put(entry.getKey(), entry.getValue().toCompactJSON());
            }
            json.put("channelCodes", map);
        }

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

        if (null != this.channelCodeMap) {
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
