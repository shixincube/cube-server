/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
        super(Utils.generateSerialNumber());

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
        json.put("timestamp", this.getTimestamp());
        json.put("rootId", this.root.getId().longValue());
        json.put("parentId", this.getParent().getId().longValue());
        json.put("originalId", this.originalId.longValue());
        json.put("chain", this.chain.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
