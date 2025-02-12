/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.common.Domain;
import cube.common.UniqueKey;
import cube.util.pinyin.ForwardLongestSelector;
import cube.util.pinyin.PYEngine;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽象联系人。
 */
public abstract class AbstractContact extends Entity {

    private final static Pattern sPattern = Pattern.compile("\\s*|\t|\r|\n");

    /**
     * 联系人显示名。
     */
    private String name;

    /**
     * 联系人携带的上下文 JSON 数据。
     * Cube 使用 JSON 格式的上下文数据来扩展联系人的数据。
     * 例如，头像、徽标、等级等信息都应当使用该数据字段进行存储。
     */
    private JSONObject context;

    /**
     * 外部 ID 。
     */
    private String externalId;

    /**
     * 构造函数。
     */
    public AbstractContact() {
        super(Utils.generateSerialNumber());
    }

    /**
     * 构造函数。
     *
     * @param externalId 外部描述 ID 。
     */
    public AbstractContact(String externalId) {
        super(Utils.generateSerialNumber(), "");
        this.externalId = externalId;
        this.name = externalId;
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domainName 域名称。
     * @param name 联系人名称。
     */
    public AbstractContact(Long id, String domainName, String name) {
        super(id, domainName);
        this.name = name;
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domainName 域名称。
     * @param name 联系人名称。
     * @param timestamp 数据时间戳。
     */
    public AbstractContact(Long id, String domainName, String name, long timestamp) {
        super(id, domainName, timestamp);
        this.name = name;
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domain 域。
     * @param name 联系人名称。
     */
    public AbstractContact(Long id, Domain domain, String name) {
        super(id, domain);
        this.name = name;
    }

    /**
     * 构造函数。
     *
     * @param json JSON 数据。
     * @param domain 域名称。
     */
    public AbstractContact(JSONObject json, String domain) {
        super(json);

        this.name = json.getString("name");
        this.domain = (null != domain && domain.length() > 1) ? new Domain(domain)
                : new Domain(json.has("domain") ? json.getString("domain") : "");
        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }

        if (json.has("externalId")) {
            this.externalId = json.getString("externalId");
        }
    }

    /**
     * 重置 ID 。
     *
     * @param newId 新的联系人 ID 。
     */
    public void resetId(Long newId) {
        this.id = newId;
        this.uniqueKey = UniqueKey.make(newId, this.domain);
    }

    /**
     * 获取外部 ID 。
     *
     * @return 返回外部 ID 。
     */
    public String getExternalId() {
        return this.externalId;
    }

    /**
     * 设置外部 ID 。
     *
     * @param externalId 字符串形式的外部 ID 。
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
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
        if (null != this.name && this.name.equals(name)) {
            return false;
        }

        Matcher matcher = sPattern.matcher(name);
        this.name = matcher.replaceAll("");
        return true;
    }

    /**
     * 获取联系人上下文数据。
     * Cube 使用 JSON 格式的上下文数据来扩展联系人的数据。
     * 例如，头像、徽标、等级等信息都应当使用该数据字段进行存储。
     *
     * @return 返回联系人上下文数据。
     */
    public JSONObject getContext() {
        return this.context;
    }

    /**
     * 设置联系人上下文数据。
     * Cube 使用 JSON 格式的上下文数据来扩展联系人的数据。
     * 例如，头像、徽标、等级等信息都应当使用该数据字段进行存储。
     *
     * @param context 联系人上下文数据。
     */
    public void setContext(JSONObject context) {
        this.context = context;
    }

    /**
     * 返回名称的拼音。
     *
     * @return 返回名称的拼音。
     */
    public String getNamePinYin() {
        return PYEngine.toPinyin(this.name, null, null, ",", new ForwardLongestSelector());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        json.put("namePY", this.getNamePinYin());
        if (null != this.context) {
            json.put("context", this.context);
        }
        if (null != this.externalId) {
            json.put("externalId", this.externalId);
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("name", this.name);
        json.put("namePY", this.getNamePinYin());
        if (null != this.context) {
            json.put("context", this.context);
        }
        if (null != this.externalId) {
            json.put("externalId", this.externalId);
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
