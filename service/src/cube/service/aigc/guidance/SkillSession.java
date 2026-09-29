/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话级技能绑定。
 *
 * <p>一个会话（频道）一旦启用过某些技能，后续轮次即使用户不再显式指定，也会继续携带这些技能——
 * 因为技能指令**不进入多轮历史**（历史只保存问答正文），若每轮重新依赖调用方传参，第二轮起技能就会失效。</p>
 *
 * <p>绑定的键是频道的 {@code channelCode}，因此同一频道内的多轮对话共享同一份技能集合。
 * 记录同时带上 {@code domain} 与 {@code contactId} 以便审计。</p>
 */
public class SkillSession {

    /**
     * 绑定集合中的清空指令：本次绑定操作清空已有全部绑定。
     */
    public final static String DIRECTIVE_CLEAR = "*";

    /**
     * 绑定集合中的解除指令前缀：{@code -name} 表示解绑 name。
     */
    public final static String DIRECTIVE_UNBIND_PREFIX = "-";

    public final String channelCode;

    public String domain;

    public long contactId;

    /**
     * 已绑定的技能名称，去重且保持稳定顺序。
     */
    public final List<String> skills = new ArrayList<>();

    public long updated;

    public SkillSession(String channelCode) {
        this.channelCode = (null != channelCode) ? channelCode : "-";
        this.updated = System.currentTimeMillis();
    }

    public SkillSession(JSONObject json) {
        this.channelCode = json.optString("channel", json.optString("channelCode", "-"));
        this.domain = json.optString("domain", null);
        this.contactId = json.optLong("contactId", 0L);
        this.updated = json.optLong("updated", System.currentTimeMillis());

        if (json.has("skills")) {
            parseSkills(json.getJSONArray("skills"), this.skills);
        }
    }

    public boolean isEmpty() {
        return this.skills.isEmpty();
    }

    /**
     * 合并一组技能名称：去重、忽略空值、保持稳定顺序。
     *
     * @param target 目标列表，直接修改。
     * @param names 待加入的名称。
     * @return 返回是否有变更。
     */
    public static boolean merge(List<String> target, List<String> names) {
        if (null == names || names.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (String name : names) {
            String key = SkillMeta.normalizeName(name);
            if (key.isEmpty() || target.contains(key)) {
                continue;
            }
            target.add(key);
            changed = true;
        }

        return changed;
    }

    /**
     * 从列表中移除一组技能名称。
     *
     * @param target 目标列表，直接修改。
     * @param names 待移除的名称。
     * @return 返回是否有变更。
     */
    public static boolean remove(List<String> target, List<String> names) {
        if (null == names || names.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (String name : names) {
            String key = SkillMeta.normalizeName(name);
            if (key.isEmpty()) {
                continue;
            }
            if (target.remove(key)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * 应用调用方传入的绑定指令，返回**普通技能名**列表。
     *
     * <p>指令约定：</p>
     * <ul>
     *     <li>{@code *}：清空已有绑定；</li>
     *     <li>{@code -name}：解除对 name 的绑定；</li>
     *     <li>{@code name}：绑定 name。</li>
     * </ul>
     *
     * @param target 绑定列表，直接修改。
     * @param directives 指令列表。
     * @return 返回普通技能名列表（不含指令项）。
     */
    public static List<String> applyDirectives(List<String> target, List<String> directives) {
        List<String> plain = new ArrayList<>();
        if (null == directives || directives.isEmpty()) {
            return plain;
        }

        for (String directive : directives) {
            String item = (null == directive) ? null : directive.trim();
            if (null == item || item.isEmpty()) {
                continue;
            }

            if (DIRECTIVE_CLEAR.equals(item)) {
                target.clear();
                continue;
            }

            if (item.startsWith(DIRECTIVE_UNBIND_PREFIX) && item.length() > 1) {
                remove(target, java.util.Collections.singletonList(item.substring(1)));
                continue;
            }

            plain.add(item);
        }

        return plain;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("channel", this.channelCode);
        if (null != this.domain) {
            json.put("domain", this.domain);
        }
        json.put("contactId", this.contactId);

        JSONArray array = new JSONArray();
        for (String name : this.skills) {
            array.put(name);
        }
        json.put("skills", array);

        json.put("updated", this.updated);
        return json;
    }

    /**
     * 输出绑定列表的副本。
     */
    public List<String> copySkills() {
        return new ArrayList<>(this.skills);
    }

    @Override
    public String toString() {
        return "SkillSession[" + this.channelCode + ", skills=" + this.skills + "]";
    }

    private static void parseSkills(JSONArray array, List<String> target) {
        for (int i = 0; i < array.length(); ++i) {
            String name = array.optString(i, null);
            String key = SkillMeta.normalizeName(name);
            if (!key.isEmpty() && !target.contains(key)) {
                target.add(key);
            }
        }
    }
}
