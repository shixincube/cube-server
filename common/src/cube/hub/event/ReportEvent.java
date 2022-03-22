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
    private Map<String, String> channelCodeMap;

    public ReportEvent(int totalAppNum, int idleAppNum, List<Contact> managedAccounts) {
        super(NAME);
        this.totalAppNum = totalAppNum;
        this.idleAppNum = idleAppNum;
        this.managedAccounts = managedAccounts;
    }

    public ReportEvent(JSONObject json) {
        super(json);
        json.put("total", this.totalAppNum);
        json.put("idle", this.idleAppNum);

        this.managedAccounts = new ArrayList<>();
        JSONArray array = json.getJSONArray("accounts");
        for (int i = 0; i < array.length(); ++i) {
            Contact account = new Contact(array.getJSONObject(i));
            this.managedAccounts.add(account);
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

        return json;
    }
}
