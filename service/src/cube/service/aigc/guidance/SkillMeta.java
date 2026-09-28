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
 * SKILL 技能的元数据与内容。
 *
 * <p>技能有两个来源：</p>
 * <ul>
 *     <li>{@link #SOURCE_DATABASE}：存储器（DB），多实例共享，是权威来源；</li>
 *     <li>{@link #SOURCE_FILE}：本地种子目录下的 {@code SKILL.md}，仅用于引导/兜底，
 *         可通过配置导入 DB。</li>
 * </ul>
 *
 * <p>本地 {@code SKILL.md} 支持 YAML 风格的 front-matter（缺失时全部使用默认值，
 * 名称取所在目录名，描述取正文首行）：</p>
 *
 * <pre>
 * ---
 * name: pdf-report
 * display_name: PDF 报告生成
 * description: 根据给定数据生成可打印的 PDF 报告
 * keywords: pdf, 报告, report
 * when_to_use: 用户要求导出可打印的报告时
 * version: 1.0
 * enabled: true
 * scope: global
 * ---
 * 技能正文……
 * </pre>
 */
public class SkillMeta {

    public final static String SOURCE_DATABASE = "db";

    public final static String SOURCE_FILE = "file";

    /**
     * 全局作用域（对所有 domain 生效）的取值。
     */
    public final static String SCOPE_GLOBAL = "global";

    /**
     * 目录行里描述的最大长度，避免目录本身占用过多预算。
     */
    private final static int MAX_DESCRIPTION_LENGTH = 200;

    public String name;

    public String displayName;

    public String description;

    public List<String> keywords = new ArrayList<>();

    public String whenToUse;

    public String version;

    public boolean enabled = true;

    /**
     * 作用域：{@code null}、空串或 {@code global} 表示全局，否则为 domain 名称。
     */
    public String scope;

    /**
     * 来源：{@link #SOURCE_DATABASE} 或 {@link #SOURCE_FILE}。
     */
    public String source = SOURCE_DATABASE;

    /**
     * 来源文件路径，仅文件来源有效。
     */
    public String path;

    public String content;

    public long created = 0L;

    public long modified = 0L;

    public SkillMeta(String name) {
        this.name = normalizeName(name);
        this.displayName = this.name;
    }

    public SkillMeta(JSONObject json) {
        this.name = normalizeName(json.optString("name", ""));
        this.displayName = json.optString("displayName", this.name);
        this.description = json.optString("description", null);
        this.whenToUse = json.optString("whenToUse", null);
        this.version = json.optString("version", null);
        this.scope = json.optString("scope", null);
        this.source = json.optString("source", SOURCE_DATABASE);
        this.path = json.optString("path", null);
        this.content = json.optString("content", null);
        this.enabled = !json.has("enabled") || json.getBoolean("enabled");
        this.created = json.optLong("created", 0L);
        this.modified = json.optLong("modified", 0L);

        if (json.has("keywords")) {
            JSONArray array = json.getJSONArray("keywords");
            for (int i = 0; i < array.length(); ++i) {
                String keyword = array.getString(i);
                if (null != keyword && !keyword.trim().isEmpty()) {
                    this.keywords.add(keyword.trim());
                }
            }
        }
    }

    /**
     * 归一化技能名称：去除首尾空白并转小写。技能名称是唯一的匹配键。
     */
    public static String normalizeName(String name) {
        return (null == name) ? "" : name.trim().toLowerCase();
    }

    /**
     * 匹配键。
     */
    public String key() {
        return normalizeName(this.name);
    }

    public boolean isGlobal() {
        return null == this.scope || this.scope.trim().isEmpty()
                || SCOPE_GLOBAL.equalsIgnoreCase(this.scope.trim());
    }

    /**
     * 判断该技能是否对指定 domain 生效。
     */
    public boolean matches(String domain) {
        if (this.isGlobal()) {
            return true;
        }
        return null != domain && this.scope.trim().equalsIgnoreCase(domain);
    }

    public int charLength() {
        return (null == this.content) ? 0 : this.content.length();
    }

    /**
     * 生成技能目录中的一行：{@code - name: description}。
     */
    public String catalogLine() {
        StringBuilder buf = new StringBuilder();
        buf.append("- ").append(this.name);
        String text = (null != this.description && !this.description.trim().isEmpty())
                ? this.description.trim() : this.displayName;
        if (null != text && !text.isEmpty()) {
            if (text.length() > MAX_DESCRIPTION_LENGTH) {
                text = text.substring(0, MAX_DESCRIPTION_LENGTH) + "…";
            }
            buf.append(": ").append(text);
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return "Skill[" + this.name + ", source=" + this.source + ", chars=" + this.charLength()
                + ", enabled=" + this.enabled + "]";
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("displayName", (null != this.displayName) ? this.displayName : this.name);
        if (null != this.description) {
            json.put("description", this.description);
        }
        JSONArray array = new JSONArray();
        for (String keyword : this.keywords) {
            array.put(keyword);
        }
        json.put("keywords", array);
        if (null != this.whenToUse) {
            json.put("whenToUse", this.whenToUse);
        }
        if (null != this.version) {
            json.put("version", this.version);
        }
        json.put("enabled", this.enabled);
        json.put("scope", this.isGlobal() ? SCOPE_GLOBAL : this.scope);
        json.put("source", this.source);
        if (null != this.path) {
            json.put("path", this.path);
        }
        json.put("created", this.created);
        json.put("modified", this.modified);
        return json;
    }

    /**
     * 解析 {@code SKILL.md} 文本，支持 YAML 风格的 front-matter。
     *
     * @param raw 文件全文。
     * @param fallbackName front-matter 未指定 name 时使用的名称，通常是所在目录名。
     * @param path 文件路径，用于留痕。
     * @return 返回解析结果，永不返回 {@code null}。
     */
    public static SkillMeta parse(String raw, String fallbackName, String path) {
        SkillMeta skill = new SkillMeta(fallbackName);
        skill.source = SOURCE_FILE;
        skill.path = path;

        String text = (null == raw) ? "" : raw.replace("\r\n", "\n").replace('\r', '\n');
        String body = text;

        if (text.startsWith("---")) {
            int end = text.indexOf("\n---", 3);
            if (end > 0) {
                skill.applyFrontMatter(text.substring(3, end));
                body = text.substring(end + 4);
            }
        }

        skill.content = body.trim();

        if (null == skill.description || skill.description.trim().isEmpty()) {
            skill.description = firstMeaningfulLine(skill.content);
        }
        if (null == skill.displayName || skill.displayName.trim().isEmpty()) {
            skill.displayName = skill.name;
        }

        return skill;
    }

    private void applyFrontMatter(String header) {
        String[] lines = header.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int index = trimmed.indexOf(':');
            if (index <= 0) {
                continue;
            }

            String key = trimmed.substring(0, index).trim().toLowerCase();
            String value = stripQuotes(trimmed.substring(index + 1).trim());
            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "name":
                case "display_name":
                case "displayname":
                    if ("name".equals(key)) {
                        this.name = normalizeName(value);
                    }
                    else {
                        this.displayName = value;
                    }
                    break;
                case "description":
                case "desc":
                    this.description = value;
                    break;
                case "keywords":
                case "keyword":
                    this.keywords.clear();
                    for (String keyword : value.split("[,，;；]")) {
                        String item = keyword.trim();
                        if (!item.isEmpty()) {
                            this.keywords.add(item);
                        }
                    }
                    break;
                case "when_to_use":
                case "whentouse":
                case "when":
                    this.whenToUse = value;
                    break;
                case "version":
                    this.version = value;
                    break;
                case "enabled":
                case "enable":
                    this.enabled = !"false".equalsIgnoreCase(value) && !"0".equals(value);
                    break;
                case "scope":
                    this.scope = SCOPE_GLOBAL.equalsIgnoreCase(value) ? SCOPE_GLOBAL : value;
                    break;
                default:
                    break;
            }
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    /**
     * 取正文中第一行有意义的文本作为兜底描述。
     */
    private static String firstMeaningfulLine(String body) {
        if (null == body || body.isEmpty()) {
            return null;
        }

        String[] lines = body.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // 去掉 Markdown 标题、列表、引用等前缀
            while (trimmed.startsWith("#") || trimmed.startsWith("*")
                    || trimmed.startsWith("-") || trimmed.startsWith(">")) {
                trimmed = trimmed.substring(1).trim();
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
                trimmed = trimmed.substring(0, MAX_DESCRIPTION_LENGTH) + "…";
            }
            return trimmed;
        }

        return null;
    }
}
