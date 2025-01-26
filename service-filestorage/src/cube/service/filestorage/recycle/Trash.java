/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cell.util.Utils;
import cube.common.entity.Entity;
import cube.service.filestorage.hierarchy.Directory;
import org.json.JSONObject;

/**
 * 垃圾记录。
 */
public abstract class Trash extends Entity {

    /**
     * 所属根目录。
     */
    private Directory root;

    /**
     * 回收链。
     */
    private RecycleChain chain;

    /**
     * 原数据的 ID 。
     */
    private Long originalId;

    /**
     * 构造函数。
     *
     * @param root
     * @param chain
     * @param originalId
     */
    public Trash(Directory root, RecycleChain chain, Long originalId) {
        super(originalId);

        this.root = root;
        this.chain = chain;
        this.originalId = originalId;
    }

    public Trash(Directory root, JSONObject json) {
        super(json.getLong("id"));

        this.root = root;

        // 时间戳
        this.setTimestamp(json.getLong("timestamp"));

        JSONObject chainJson = json.getJSONObject("chain");
        this.chain = new RecycleChain(chainJson);

        this.originalId = json.getLong("originalId");
    }

    public String getDomainName() {
        return this.root.getDomain().getName();
    }

    public Directory getRoot() {
        return this.root;
    }

    public RecycleChain getChain() {
        return this.chain;
    }

    public Long getOriginalId() {
        return this.originalId;
    }

    public abstract Directory getParent();

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId().longValue());
        json.put("domain", this.root.getDomain().getName());
        json.put("timestamp", this.getTimestamp());
        json.put("rootId", this.root.getId().longValue());
        json.put("parentId", this.getParent().getId().longValue());
        json.put("originalId", this.originalId.longValue());
        json.put("chain", this.chain.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId().longValue());
        json.put("domain", this.root.getDomain().getName());
        json.put("timestamp", this.getTimestamp());
        json.put("rootId", this.root.getId().longValue());
        json.put("parentId", this.getParent().getId().longValue());
        json.put("originalId", this.originalId.longValue());
        return json;
    }
}
