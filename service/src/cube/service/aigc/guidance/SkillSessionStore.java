/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import cube.service.aigc.AIGCStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话级技能绑定的存储。
 *
 * <p>存储器（DB 表 {@code aigc_skill_session}）是权威来源：服务多实例部署时，同一频道的不同请求可能落在
 * 不同实例上，只有落库才能保证「上一轮启用的技能这一轮还在」。为避免每轮都读库，本地加一层**短 TTL 缓存**
 * （默认 30 秒），变化时写穿（write-through）。</p>
 *
 * <p>绑定有生存时间（{@code skills.session.ttl}，默认 30 分钟）：超过 TTL 未更新的绑定自动失效，
 * 避免长期悬挂的旧技能影响新话题。</p>
 */
public class SkillSessionStore {

    /**
     * 绑定默认生存时间：30 分钟。
     */
    public final static long DEFAULT_TTL = 30 * 60 * 1000L;

    /**
     * 本地缓存默认 TTL：30 秒。
     */
    public final static long DEFAULT_CACHE_TTL = 30 * 1000L;

    /**
     * 本地缓存的最大条目数，超出后整体清空（会话状态属可恢复数据，清空只是多读一次库）。
     */
    public final static int DEFAULT_MAX_ENTRIES = 4096;

    private static final SkillSessionStore sInstance = new SkillSessionStore();

    public static SkillSessionStore getInstance() {
        return sInstance;
    }

    private static class Entry {

        final SkillSession session;

        final long readTime;

        Entry(SkillSession session, long readTime) {
            this.session = session;
            this.readTime = readTime;
        }
    }

    private volatile AIGCStorage storage;

    private volatile boolean enabled = true;

    private volatile long ttl = DEFAULT_TTL;

    private volatile long cacheTtl = DEFAULT_CACHE_TTL;

    private volatile int maxEntries = DEFAULT_MAX_ENTRIES;

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    private SkillSessionStore() {
    }

    /**
     * 绑定存储器。必须在存储器 {@code open()} 之后调用。
     *
     * @param storage AIGC 存储器，允许为 {@code null}（此时仅使用本地缓存，不具备多实例一致性）。
     */
    public void setup(AIGCStorage storage) {
        this.storage = storage;
        this.cache.clear();
        Logger.i(SkillSessionStore.class, "#setup - Skill session store ready, storage: "
                + ((null != storage) ? "yes" : "no"));
    }

    /**
     * 应用配置。
     *
     * @param enabled 是否启用会话级技能绑定。
     * @param ttlMs 绑定生存时间（毫秒），&lt;= 0 时使用默认值。
     * @param cacheTtlMs 本地缓存时间（毫秒），&lt;= 0 时使用默认值。
     */
    public void configure(boolean enabled, long ttlMs, long cacheTtlMs) {
        this.enabled = enabled;
        this.ttl = (ttlMs > 0) ? ttlMs : DEFAULT_TTL;
        this.cacheTtl = (cacheTtlMs > 0) ? cacheTtlMs : DEFAULT_CACHE_TTL;
        this.cache.clear();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getTtl() {
        return this.ttl;
    }

    public int size() {
        return this.cache.size();
    }

    /**
     * 读取频道当前有效的技能绑定。
     *
     * @param channelCode 频道代码。
     * @return 返回技能名称列表（已归一化、去重、排序），永不为 {@code null}。
     */
    public List<String> get(String channelCode) {
        if (!this.enabled || null == channelCode) {
            return new ArrayList<>();
        }

        SkillSession session = this.load(channelCode);
        if (null == session || session.isEmpty()) {
            return new ArrayList<>();
        }

        return session.copySkills();
    }

    /**
     * 解析本次请求最终生效的技能集合，并把结果持久化为会话绑定。
     *
     * <p>合并顺序：已有绑定 → 应用调用方的绑定指令（{@code *} / {@code -name} / {@code name}） →
     * 追加自动匹配到的技能。若全程无变化则**不写库**（避免每轮一次写入），仅在绑定临近过期时续期。</p>
     *
     * @param channelCode 频道代码。
     * @param domain 域名，用于审计。
     * @param contactId 联系人 ID，用于审计。
     * @param requested 调用方传入的分类/技能列表，可能包含绑定指令。
     * @param auto 平台自动匹配到的技能名称。
     * @return 返回最终生效的技能名称列表（已归一化、去重、排序），永不为 {@code null}。
     */
    public List<String> resolve(String channelCode, String domain, long contactId,
                                List<String> requested, List<String> auto) {
        List<String> effective = new ArrayList<>();
        if (null == channelCode) {
            return effective;
        }

        if (!this.enabled) {
            // 未启用会话绑定：只做本次合并，不落库
            List<String> temp = new ArrayList<>();
            List<String> plain = SkillSession.applyDirectives(temp, requested);
            SkillSession.merge(temp, plain);
            SkillSession.merge(temp, auto);
            Collections.sort(temp);
            return temp;
        }

        SkillSession session = this.load(channelCode);
        boolean dirty = false;
        if (null == session) {
            session = new SkillSession(channelCode);
            dirty = true;
        }

        List<String> before = session.copySkills();

        List<String> plain = SkillSession.applyDirectives(session.skills, requested);
        SkillSession.merge(session.skills, plain);
        SkillSession.merge(session.skills, auto);

        Collections.sort(session.skills);

        if (!before.equals(session.skills)) {
            dirty = true;
        }

        long now = System.currentTimeMillis();
        if (dirty) {
            session.domain = domain;
            session.contactId = contactId;
            session.updated = now;
            this.persist(session);
        }
        else if (!session.isEmpty() && (now - session.updated) > (this.ttl / 2)) {
            // 未变化但已过半程：仅续期，避免活跃会话的绑定中途过期
            session.updated = now;
            this.persist(session);
        }

        this.cache.put(channelCode, new Entry(session, now));
        effective.addAll(session.skills);
        return effective;
    }

    /**
     * 清空指定频道的绑定。
     *
     * @param channelCode 频道代码。
     * @return 返回是否执行了删除。
     */
    public boolean clear(String channelCode) {
        if (null == channelCode) {
            return false;
        }

        this.cache.remove(channelCode);

        AIGCStorage storage = this.storage;
        if (null == storage) {
            return false;
        }

        try {
            return storage.deleteSkillSession(channelCode);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#clear", e);
            return false;
        }
    }

    /**
     * 使本地缓存失效，下次读取时回源存储器。用于多实例场景下手工刷新。
     */
    public void invalidate(String channelCode) {
        if (null == channelCode) {
            this.cache.clear();
        }
        else {
            this.cache.remove(channelCode);
        }
    }

    /**
     * 清理存储器中已过期的绑定记录。
     *
     * @return 返回是否执行了删除。
     */
    public boolean prune() {
        AIGCStorage storage = this.storage;
        if (null == storage) {
            return false;
        }

        try {
            return storage.pruneSkillSessions(System.currentTimeMillis() - this.ttl);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#prune", e);
            return false;
        }
    }

    private SkillSession load(String channelCode) {
        long now = System.currentTimeMillis();
        AIGCStorage storage = this.storage;

        if (null == storage) {
            // 未绑定存储器（例如存储器尚未就绪）：本地缓存即权威，仅受绑定 TTL 约束，
            // 不能因为缓存过期就把绑定丢掉
            Entry cached = this.cache.get(channelCode);
            if (null == cached) {
                return null;
            }
            return this.checkExpired(cached.session, now) ? null : cached.session;
        }

        Entry entry = this.cache.get(channelCode);
        if (null != entry && (now - entry.readTime) < this.cacheTtl) {
            return this.checkExpired(entry.session, now) ? null : entry.session;
        }

        SkillSession session = null;
        try {
            session = storage.readSkillSession(channelCode);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#load", e);
        }

        if (null == session) {
            this.cache.remove(channelCode);
            return null;
        }

        if (this.checkExpired(session, now)) {
            // 已过期：删除记录并视为无绑定
            this.cache.remove(channelCode);
            try {
                storage.deleteSkillSession(channelCode);
            } catch (Exception e) {
                Logger.w(this.getClass(), "#load - Delete expired session failed", e);
            }
            return null;
        }

        if (this.cache.size() >= this.maxEntries) {
            this.cache.clear();
        }
        this.cache.put(channelCode, new Entry(session, now));
        return session;
    }

    private boolean checkExpired(SkillSession session, long now) {
        return (null == session) || ((now - session.updated) > this.ttl);
    }

    private void persist(SkillSession session) {
        AIGCStorage storage = this.storage;
        if (null == storage) {
            return;
        }

        try {
            if (session.isEmpty()) {
                storage.deleteSkillSession(session.channelCode);
            }
            else {
                storage.writeSkillSession(session);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#persist", e);
        }
    }
}
