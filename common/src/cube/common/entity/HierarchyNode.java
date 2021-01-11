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

package cube.common.entity;

import cell.util.Utils;
import cube.common.Domain;
import cube.common.UniqueKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层结构节点。
 */
public class HierarchyNode extends Entity {

    /**
     * 父节点。
     */
    private HierarchyNode parent;

    /**
     * 子节点。
     */
    private List<HierarchyNode> children;

    /**
     * 节点内存储的实体唯一键。
     */
    private List<String> relatedKeys;

    /**
     * 节点的上下文信息。
     */
    private JSONObject context;

    /**
     * 用于进行节点遍历控制的子节点的键。
     */
    protected List<String> unloadChildrenKeys;

    /**
     * 构造函数。
     *
     * @param id 节点的 ID 。
     * @param domain 节点的域。
     */
    public HierarchyNode(Long id, String domain) {
        super(id, domain);
        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();
    }

    /**
     * 构造函数。
     *
     * @param parent 父节点。
     */
    public HierarchyNode(HierarchyNode parent) {
        super(Utils.generateSerialNumber(), parent.domain);
        this.parent = parent;
        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();
    }

    /**
     * 构造函数。
     *
     * @param json 层级节点结构的 JSON 对象。
     */
    public HierarchyNode(JSONObject json) {
        super();

        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();

        this.unloadChildrenKeys = new ArrayList<>();

        try {
            this.id = json.getLong("id");
            this.domain = new Domain(json.getString("domain"));

            this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());

            this.context = json.getJSONObject("context");

            JSONArray relatedArray = json.getJSONArray("related");
            for (int i = 0; i < relatedArray.length(); ++i) {
                String uk = relatedArray.getString(i);
                this.relatedKeys.add(uk);
            }

            // 预存子节点的键
            JSONArray childrenKeys = json.getJSONArray("children");
            for (int i = 0; i < childrenKeys.length(); ++i) {
                String key = childrenKeys.getString(i);
                this.unloadChildrenKeys.add(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置父节点。
     *
     * @param parent 父节点。
     */
    public void setParent(HierarchyNode parent) {
        this.parent = parent;
    }

    /**
     * 获取父节点。
     *
     * @return 返回父节点。
     */
    public HierarchyNode getParent() {
        return this.parent;
    }

    /**
     * 添加子节点。
     *
     * @param child 子节点。
     */
    public void addChild(HierarchyNode child) {
        if (child == this) {
            return;
        }
        
        if (this.children.contains(child)) {
            return;
        }

        child.parent = this;
        this.children.add(child);

        if (null != this.unloadChildrenKeys) {
            this.unloadChildrenKeys.remove(child.getUniqueKey());
        }
    }

    /**
     * 移除子节点。
     *
     * @param child 子节点。
     */
    public void removeChild(HierarchyNode child) {
        this.children.remove(child);
        child.parent = null;

        if (null != this.unloadChildrenKeys) {
            this.unloadChildrenKeys.remove(child.getUniqueKey());
        }
    }

    /**
     * 获取所有子节点。
     *
     * @return 返回所有子节点的列表。
     */
    public List<HierarchyNode> getChildren() {
        return new ArrayList<>(this.children);
    }

    /**
     * 返回子节点数量。
     *
     * @return 返回子节点数量。
     */
    public int numChildren() {
        if (null != this.unloadChildrenKeys && !this.unloadChildrenKeys.isEmpty()) {
            return this.unloadChildrenKeys.size();
        }

        return this.children.size();
    }

    /**
     * 连接实体。
     *
     * @param entity 指定实体。
     * @return 连接成功返回 {@code true} 。
     */
    public boolean link(Entity entity) {
        synchronized (this.relatedKeys) {
            if (this.relatedKeys.contains(entity.getUniqueKey())) {
                return false;
            }

            this.relatedKeys.add(entity.getUniqueKey());

            return true;
        }
    }

    /**
     * 断开实体。
     *
     * @param entity 指定实体。
     * @return 断开成功返回 {@code true} 。
     */
    public boolean unlink(Entity entity) {
        synchronized (this.relatedKeys) {
            return this.relatedKeys.remove(entity.getUniqueKey());
        }
    }

    /**
     * 获取所有关联的实体的唯一键。
     *
     * @return 获取所有关联的实体的唯一键。
     */
    public List<String> getRelatedKeys() {
        synchronized (this.relatedKeys) {
            return new ArrayList<>(this.relatedKeys);
        }
    }

    /**
     * 是否包含了关联的实体唯一键。
     *
     * @param key 指定键。
     * @return 如果包含了返回 {@code true} 。
     */
    public boolean hasRelated(String key) {
        synchronized (this.relatedKeys) {
            return this.relatedKeys.contains(key);
        }
    }

    /**
     * 返回关联键的数量。
     *
     * @return 返回关联键的数量。
     */
    public int numRelatedKeys() {
        synchronized (this.relatedKeys) {
            return this.relatedKeys.size();
        }
    }

    /**
     * 返回上下文。
     *
     * @return 返回上下文。
     */
    public JSONObject getContext() {
        return this.context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id.longValue());
            json.put("domain", this.domain.getName());

            if (null != this.parent) {
                json.put("parent", this.parent.getUniqueKey());
            }

            JSONArray children = new JSONArray();
            for (int i = 0; i < this.children.size(); ++i) {
                children.put(this.children.get(i).getUniqueKey());
            }
            json.put("children", children);

            JSONArray relatedKeys = new JSONArray();
            for (int i = 0; i < this.relatedKeys.size(); ++i) {
                relatedKeys.put(this.relatedKeys.get(i));
            }
            json.put("related", relatedKeys);

            json.put("context", this.context);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id.longValue());
            json.put("domain", this.domain.getName());

            if (null != this.parent) {
                json.put("parent", this.parent.getUniqueKey());
            }

            json.put("context", this.context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
