/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.core.Cache;
import cube.core.CacheKey;
import cube.core.CacheValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层节点操作工具。
 */
public final class HierarchyNodes {

    private HierarchyNodes() {
    }

    /**
     * 从缓存里加载指定节点。
     *
     * @param cache
     * @param nodeUniqueKey
     * @return
     */
    public static HierarchyNode load(Cache cache, String nodeUniqueKey) {
        CacheValue value = cache.get(new CacheKey(nodeUniqueKey));
        if (null == value) {
            return null;
        }

        JSONObject data = value.get();

        HierarchyNode node = new HierarchyNode(data);

        try {
            // 查找父节点
            if (data.has("parent")) {
                String parentUK = data.getString("parent");
                CacheValue parentValue = cache.get(new CacheKey(parentUK));
                if (null != parentUK) {
                    node.setParent(new HierarchyNode(parentValue.get()));
                }
            }

            // 查找子节点
            JSONArray childrenJson = data.getJSONArray("children");
            for (int i = 0, len = childrenJson.length(); i < len; ++i) {
                String uk = childrenJson.getString(i);
                CacheValue childValue = cache.get(new CacheKey(uk));
                if (null != childValue) {
                    HierarchyNode child = new HierarchyNode(childValue.get());
                    node.addChild(child);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return node;
    }

    /**
     * 保存节点。
     *
     * @param cache
     * @param node
     */
    public static void save(Cache cache, HierarchyNode node) {
        cache.put(new CacheKey(node.getUniqueKey()), new CacheValue(node.toJSON()));
    }

    /**
     * 删除节点。
     *
     * @param cache
     * @param node
     */
    public static void delete(Cache cache, HierarchyNode node) {
        cache.remove(new CacheKey(node.getUniqueKey()));
    }

    /**
     * 向上遍历。
     *
     * @param node
     * @param hierarchy
     * @return
     */
    public static HierarchyNode traversalUp(HierarchyNode node, int hierarchy) {
        int count = hierarchy > 0 ? hierarchy : 512;

        HierarchyNode ret = node;
        HierarchyNode parent = ret.getParent();

        while (count > 0) {
            --count;

            if (null == parent) {
                break;
            }

            ret = parent;
            parent = ret.getParent();
        }

        return ret;
    }

    /**
     * 遍历所有的子节点。
     *
     * @param cache
     * @param node
     * @return
     */
    public static List<HierarchyNode> traversalChildren(Cache cache, HierarchyNode node) {
        if (null != node.unloadChildrenKeys && !node.unloadChildrenKeys.isEmpty()) {
            List<String> keys = new ArrayList<>(node.unloadChildrenKeys);
            for (int i = 0; i < keys.size(); ++i) {
                String key = keys.get(i);

                CacheValue value = cache.get(new CacheKey(key));
                if (null != value) {
                    HierarchyNode child = new HierarchyNode(value.get());

                    node.addChild(child);
                }
            }

            keys.clear();
        }

        return node.getChildren();
    }
}
