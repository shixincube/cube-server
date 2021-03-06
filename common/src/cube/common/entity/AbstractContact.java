/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

import cube.common.Domain;
import cube.common.UniqueKey;
import org.json.JSONObject;

/**
 * 抽象联系人。
 */
public abstract class AbstractContact extends Entity {

    /**
     * 联系人显示名。
     */
    private String name;

    /**
     * 联系携带的上下文 JSON 数据。
     */
    private JSONObject context;

    /**
     *
     * @param id
     * @param domainName
     * @param name
     */
    public AbstractContact(Long id, String domainName, String name) {
        super(id, domainName);
        this.name = name;
    }

    /**
     *
     * @param id
     * @param domain
     * @param name
     */
    public AbstractContact(Long id, Domain domain, String name) {
        super(id, domain);
        this.name = name;
    }

    /**
     *
     * @param json
     * @param domain
     */
    public AbstractContact(JSONObject json, String domain) {
        super();

        this.id = json.getLong("id");
        this.name = json.getString("name");
        this.domain = (null != domain && domain.length() > 1) ? new Domain(domain) : new Domain(json.getString("domain"));
        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    /**
     * 重置 ID 。
     *
     * @param newId
     */
    public void resetId(Long newId) {
        this.id = newId;
        this.uniqueKey = UniqueKey.make(newId, this.domain);
    }

    /**
     * 获取联系人名称。
     *
     * @return 返回联系人名称。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 设置联系人名称。
     *
     * @param name 联系人名称。
     */
    public boolean setName(String name) {
        if (this.name.equals(name)) {
            return false;
        }

        this.name = name;
        return true;
    }

    /**
     * 获取联系人上下文数据。
     *
     * @return 返回联系人上下文数据。
     */
    public JSONObject getContext() {
        return this.context;
    }

    /**
     * 设置联系人上下文数据。
     *
     * @param context 联系人上下文数据。
     */
    public void setContext(JSONObject context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain.getName());
        json.put("name", this.name);
        if (null != this.context) {
            json.put("context", this.context);
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain.getName());
        json.put("name", this.name);
        if (null != this.context) {
            json.put("context", this.context);
        }
        return json;
    }

    /**
     * 仅返回包含 ID 和名称的 JSON 。
     *
     * @return 仅返回包含 ID 和名称的 JSON 。
     */
    public JSONObject toBasicJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain.getName());
        json.put("name", this.name);
        return json;
    }
}
