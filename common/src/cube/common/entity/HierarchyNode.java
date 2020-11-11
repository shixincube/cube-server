/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.UniqueKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层结构节点。
 */
public class HierarchyNode extends Entity {

    private HierarchyNode parent;

    private List<HierarchyNode> children;

    private List<String> relatedKeys;

    private JSONObject context;

    protected List<String> unloadChildrenKey;

    public HierarchyNode(Long id, String domain) {
        super(id, domain);
        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();
    }

    public HierarchyNode(HierarchyNode parent) {
        super(Utils.generateSerialNumber(), parent.domain);
        this.parent = parent;
        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();
    }

    public HierarchyNode(JSONObject json) {
        super();

        this.children = new ArrayList<>();
        this.relatedKeys = new ArrayList<>();
        this.context = new JSONObject();

        this.unloadChildrenKey = new ArrayList<>();

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
                this.unloadChildrenKey.add(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setParent(HierarchyNode parent) {
        this.parent = parent;
    }

    public HierarchyNode getParent() {
        return this.parent;
    }

    public void addChild(HierarchyNode child) {
        if (this.children.contains(child)) {
            return;
        }

        child.parent = this;
        this.children.add(child);

        this.unloadChildrenKey.remove(child.getUniqueKey());
    }

    public void removeChild(HierarchyNode child) {
        this.children.remove(child);
        child.parent = null;

        this.unloadChildrenKey.remove(child.getUniqueKey());
    }

    public List<HierarchyNode> getChildren() {
        return new ArrayList<>(this.children);
    }

    public int numChildren() {
        return this.children.size();
    }

    public void link(Entity entity) {
        synchronized (this.relatedKeys) {
            if (this.relatedKeys.contains(entity.getUniqueKey())) {
                return;
            }

            this.relatedKeys.add(entity.getUniqueKey());
        }
    }

    public void unlink(Entity entity) {
        synchronized (this.relatedKeys) {
            this.relatedKeys.remove(entity.getUniqueKey());
        }
    }

    public List<String> getRelatedKeys() {
        synchronized (this.relatedKeys) {
            return new ArrayList<>(this.relatedKeys);
        }
    }

    public boolean hasRelated(String key) {
        synchronized (this.relatedKeys) {
            return this.relatedKeys.contains(key);
        }
    }

    public int numRelatedKeys() {
        synchronized (this.relatedKeys) {
            return this.relatedKeys.size();
        }
    }

    public JSONObject getContext() {
        return this.context;
    }

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
