/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import cube.service.aigc.AIGCStorage;
import cube.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SKILL 技能注册表。
 *
 * <p>技能的唯一权威来源是存储器（DB 表 {@code aigc_skill}），多实例部署下各实例共享同一份技能定义；
 * 本地种子目录（{@code skills.path}）仅作为引导与兜底来源，可按需导入 DB。</p>
 *
 * <p>缓存策略与同包的 {@link Prompts}、{@link Guides} 保持一致，采用「时间戳比对 + 惰性刷新」：</p>
 * <ul>
 *     <li>DB：按 {@code skills.cache.ttl} 周期比对签名（行数 + 最大修改时间），变化才整体重载，
 *         因此多实例下技能变更最长在 TTL 之后生效；</li>
 *     <li>文件：逐个文件比对 {@code lastModified}，仅重新读取发生变化的文件。</li>
 * </ul>
 *
 * <p>刷新在访问时惰性触发，不额外占用定时线程。</p>
 */
public class SkillRegistry {

    /**
     * 默认的技能种子目录（工作目录相对路径）。
     */
    public final static String DEFAULT_SKILLS_PATH = "assets/skills/";

    /**
     * 技能目录内约定的一级技能文件名。
     */
    public final static String SKILL_FILENAME = "SKILL.md";

    /**
     * 默认缓存时间：60 秒。
     */
    public final static long DEFAULT_CACHE_TTL = 60 * 1000L;

    private final static String README_FILENAME = "readme.md";

    private static final SkillRegistry sInstance = new SkillRegistry();

    public static SkillRegistry getInstance() {
        return sInstance;
    }

    private volatile AIGCStorage storage;

    private volatile List<File> sourcePaths = new ArrayList<>();

    private volatile long cacheTtl = DEFAULT_CACHE_TTL;

    private volatile boolean seedEnabled = false;

    private volatile boolean seedOverwrite = false;

    /**
     * 已发布的技能索引：名称（小写）-> 技能。DB 与文件同名时以 DB 为准。
     * 整体替换后再发布，读侧无需加锁。
     */
    private volatile Map<String, SkillMeta> skills = Collections.emptyMap();

    /**
     * DB 侧技能索引。仅在 {@link #mutex} 内替换。
     */
    private Map<String, SkillMeta> databaseSkills = Collections.emptyMap();

    /**
     * 文件来源的 mtime 缓存：绝对路径 -> 技能。仅在 {@link #mutex} 内访问。
     */
    private final Map<String, SkillMeta> fileCache = new HashMap<>();

    /**
     * DB 侧签名，用于判断是否需要整体重载。仅在 {@link #mutex} 内访问。
     */
    private String databaseSignature = null;

    private final Object mutex = new Object();

    private volatile long lastRefreshTime = 0L;

    private volatile boolean warnedNoStorage = false;

    private SkillRegistry() {
    }

    /**
     * 绑定存储器并做首次刷新。必须在存储器 {@code open()} 之后调用。
     *
     * @param storage AIGC 存储器，允许为 {@code null}（此时仅使用文件来源）。
     */
    public void setup(AIGCStorage storage) {
        this.storage = storage;
        this.refresh();

        if (this.seedEnabled) {
            int count = this.seed();
            if (count > 0) {
                Logger.i(SkillRegistry.class, "#setup - Seeded " + count + " skill(s) into storage");
                this.refresh();
            }
        }

        Logger.i(SkillRegistry.class, "#setup - Loaded " + this.skills.size() + " skill(s), sources: "
                + this.sourcePaths);
    }

    /**
     * 应用配置。
     *
     * @param sourcePaths 技能种子目录列表（允许为 {@code null}）。
     * @param cacheTtlMs 缓存时间，单位毫秒，&lt;= 0 时使用默认值。
     * @param seed 是否在启动时把种子目录的技能导入存储器。
     * @param seedOverwrite 导入时是否覆盖存储器中同名技能。
     */
    public void configure(List<File> sourcePaths, long cacheTtlMs,
                          boolean seed, boolean seedOverwrite) {
        this.sourcePaths = (null == sourcePaths) ? new ArrayList<File>() : new ArrayList<>(sourcePaths);
        this.cacheTtl = (cacheTtlMs > 0) ? cacheTtlMs : DEFAULT_CACHE_TTL;
        this.seedEnabled = seed;
        this.seedOverwrite = seedOverwrite;
        this.invalidate();
    }

    /**
     * 使缓存立即失效，下一次访问时重新加载。
     */
    public void invalidate() {
        this.lastRefreshTime = 0L;
    }

    /**
     * 立即刷新（忽略 TTL）。
     */
    public void refresh() {
        this.invalidate();
        this.refreshIfNeeded();
    }

    /**
     * 返回当前已发布的技能数量（含未启用的）。
     */
    public int size() {
        this.refreshIfNeeded();
        return this.skills.size();
    }

    /**
     * 列出对指定 domain 生效且已启用的技能，按名称排序。
     *
     * @param domain 域名，允许为 {@code null}。
     * @return 返回技能列表，永不为 {@code null}。
     */
    public List<SkillMeta> listSkills(String domain) {
        this.refreshIfNeeded();

        List<SkillMeta> list = new ArrayList<>();
        for (SkillMeta skill : this.skills.values()) {
            if (!skill.enabled) {
                continue;
            }
            if (skill.matches(domain)) {
                list.add(skill);
            }
        }

        sortByName(list);
        return list;
    }

    /**
     * 按名称查找技能（大小写不敏感）。
     *
     * @param name 技能名称，来自调用方指定的 categories。
     * @param domain 域名，用于作用域校验。
     * @return 未找到或作用域不匹配返回 {@code null}。
     */
    public SkillMeta getSkill(String name, String domain) {
        if (null == name || name.trim().isEmpty()) {
            return null;
        }

        this.refreshIfNeeded();

        SkillMeta skill = this.skills.get(SkillMeta.normalizeName(name));
        if (null == skill || !skill.matches(domain)) {
            return null;
        }

        return skill;
    }

    /**
     * 生成技能目录（索引）文本，用于在提示词里告知模型有哪些技能可用。
     *
     * @param domain 域名。
     * @return 没有任何可用技能时返回 {@code null}。
     */
    public String buildCatalog(String domain) {
        List<SkillMeta> list = this.listSkills(domain);
        if (list.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (SkillMeta skill : list) {
            if (buf.length() > 0) {
                buf.append('\n');
            }
            buf.append(skill.catalogLine());
        }
        return buf.toString();
    }

    /**
     * 按关键词匹配技能。
     *
     * @param words 分词结果。
     * @param limit 返回的最大数量，&lt;= 0 表示不限制。
     * @param domain 域名。
     * @return 按名称排序的命中技能列表。
     */
    public List<SkillMeta> matchSkills(List<String> words, int limit, String domain) {
        List<SkillMeta> result = new ArrayList<>();
        if (null == words || words.isEmpty()) {
            return result;
        }

        for (SkillMeta skill : this.listSkills(domain)) {
            for (String keyword : skill.keywords) {
                if (words.contains(keyword)) {
                    result.add(skill);
                    break;
                }
            }
        }

        sortByName(result);

        if (limit > 0 && result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }

        return result;
    }

    /**
     * 按原始查询文本匹配技能：分词命中**或**原文包含关键词即视为命中。
     *
     * <p>同时使用两种判定，是因为技能作者声明关键词时通常写的是完整词组（例如「会议纪要」），
     * 而分词结果未必与词组完全一致；原文包含判定更接近作者意图。命中后按关键词长度倒序优先，
     * 让更具体的技能排在前面。</p>
     *
     * @param query 用户原始查询文本，允许为 {@code null}。
     * @param words 分词结果，允许为 {@code null}。
     * @param limit 返回的最大数量，&lt;= 0 表示不限制。
     * @param domain 域名。
     * @return 按命中优先级排序的技能列表。
     */
    public List<SkillMeta> matchSkills(String query, List<String> words, int limit, String domain) {
        List<SkillMeta> result = new ArrayList<>();
        String text = (null == query) ? "" : query.toLowerCase();

        for (SkillMeta skill : this.listSkills(domain)) {
            boolean hit = false;
            for (String keyword : skill.keywords) {
                String key = (null == keyword) ? null : keyword.trim();
                if (null == key || key.isEmpty()) {
                    continue;
                }

                if ((null != words && words.contains(key)) || text.contains(key.toLowerCase())) {
                    hit = true;
                    break;
                }
            }

            if (hit) {
                result.add(skill);
            }
        }

        sortByName(result);

        if (limit > 0 && result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }

        return result;
    }

    /**
     * 把种子目录里的技能导入存储器。同名技能已存在且未开启覆盖时跳过，因此是幂等的。
     *
     * @return 返回实际写入存储器的技能数量。
     */
    public int seed() {
        AIGCStorage storage = this.storage;
        if (null == storage || !this.seedEnabled) {
            return 0;
        }

        List<SkillMeta> fileSkills;
        synchronized (this.mutex) {
            this.refreshFiles();
            fileSkills = new ArrayList<>(this.fileCache.values());
        }

        int count = 0;
        for (SkillMeta skill : fileSkills) {
            try {
                if (storage.writeSkill(skill, this.seedOverwrite)) {
                    ++count;
                }
            } catch (Exception e) {
                Logger.w(SkillRegistry.class, "#seed - Write skill failed: " + skill.name, e);
            }
        }

        return count;
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (this.lastRefreshTime != 0L && (now - this.lastRefreshTime) < this.cacheTtl) {
            return;
        }

        synchronized (this.mutex) {
            now = System.currentTimeMillis();
            if (this.lastRefreshTime != 0L && (now - this.lastRefreshTime) < this.cacheTtl) {
                return;
            }

            this.refreshDatabase();
            this.refreshFiles();

            // 合并索引：文件作为底，DB 覆盖同名项
            Map<String, SkillMeta> merged = new LinkedHashMap<>();
            for (SkillMeta skill : this.fileCache.values()) {
                merged.put(skill.key(), skill);
            }
            for (SkillMeta skill : this.databaseSkills.values()) {
                merged.put(skill.key(), skill);
            }

            this.skills = Collections.unmodifiableMap(merged);
            this.lastRefreshTime = now;
        }
    }

    private void refreshDatabase() {
        AIGCStorage storage = this.storage;
        if (null == storage) {
            if (!this.warnedNoStorage) {
                this.warnedNoStorage = true;
                Logger.w(SkillRegistry.class, "#refreshDatabase - No storage bound, use file skills only");
            }
            return;
        }

        try {
            if (!storage.hasSkillTable()) {
                return;
            }

            String signature = storage.getSkillSignature();
            if (null == signature) {
                // 查询失败（如表不存在或瞬时故障）：保留上一次的缓存，避免清空技能
                return;
            }
            if (signature.equals(this.databaseSignature)) {
                return;
            }

            List<SkillMeta> list = storage.readSkills();

            Map<String, SkillMeta> map = new LinkedHashMap<>();
            for (SkillMeta skill : list) {
                map.put(skill.key(), skill);
            }

            this.databaseSkills = Collections.unmodifiableMap(map);
            this.databaseSignature = signature;

            if (Logger.isDebugLevel()) {
                Logger.d(SkillRegistry.class, "#refreshDatabase - Reloaded " + map.size()
                        + " skill(s), signature: " + signature);
            }
        } catch (Exception e) {
            Logger.w(SkillRegistry.class, "#refreshDatabase", e);
        }
    }

    private void refreshFiles() {
        if (this.sourcePaths.isEmpty()) {
            this.fileCache.clear();
            return;
        }

        Map<String, SkillMeta> cache = new HashMap<>();

        for (File root : this.sourcePaths) {
            if (null == root || !root.isDirectory()) {
                continue;
            }

            File[] children = root.listFiles();
            if (null == children) {
                continue;
            }

            for (File child : children) {
                File skillFile = resolveSkillFile(child);
                if (null == skillFile) {
                    continue;
                }

                String path = skillFile.getAbsolutePath();
                long lastModified = skillFile.lastModified();

                SkillMeta cached = this.fileCache.get(path);
                if (null != cached && cached.modified == lastModified) {
                    cache.put(path, cached);
                    continue;
                }

                try {
                    String raw = FileUtils.readTextFile(path);
                    if (null == raw || raw.trim().isEmpty()) {
                        continue;
                    }

                    String fallbackName = child.isDirectory() ? child.getName() : stripExtension(child.getName());
                    SkillMeta skill = SkillMeta.parse(raw, fallbackName, path);
                    skill.modified = lastModified;

                    if (Logger.isDebugLevel()) {
                        Logger.d(SkillRegistry.class, "#refreshFiles - Loaded " + skill.name + " from " + path);
                    }

                    cache.put(path, skill);
                } catch (Exception e) {
                    Logger.w(SkillRegistry.class, "#refreshFiles - Read skill file failed: " + path, e);
                }
            }
        }

        this.fileCache.clear();
        this.fileCache.putAll(cache);
    }

    /**
     * 解析某个目录项对应的 SKILL 文件，不存在时返回 {@code null}。
     * 支持 {@code <root>/<name>/SKILL.md}（{@code skill.md} 亦可）与 {@code <root>/<name>.md} 两种形式；
     * 以 {@code _} 或 {@code .} 开头的目录项视为内部文件，不参与加载。
     */
    private static File resolveSkillFile(File child) {
        if (null == child) {
            return null;
        }

        String name = child.getName();
        if (name.isEmpty() || name.startsWith("_") || name.startsWith(".")) {
            return null;
        }

        if (child.isDirectory()) {
            File file = new File(child, SKILL_FILENAME);
            if (!file.isFile()) {
                file = new File(child, "skill.md");
            }
            return file.isFile() ? file : null;
        }

        if (!child.isFile() || !name.toLowerCase().endsWith(".md")
                || README_FILENAME.equalsIgnoreCase(name)) {
            return null;
        }

        return child;
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(0, index) : fileName;
    }

    private static void sortByName(List<SkillMeta> list) {
        Collections.sort(list, new Comparator<SkillMeta>() {
            @Override
            public int compare(SkillMeta o1, SkillMeta o2) {
                return o1.name.compareTo(o2.name);
            }
        });
    }
}
