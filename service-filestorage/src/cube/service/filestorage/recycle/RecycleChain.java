/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cube.common.JSONable;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.MetaDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * 回收链。
 */
public final class RecycleChain implements JSONable {

    private LinkedList<Directory> nodes;

    public RecycleChain(LinkedList<Directory> nodes) {
        this.nodes = new LinkedList<>(nodes);
    }

    public RecycleChain(JSONObject json) {
        this.nodes = new LinkedList<>();

        JSONArray array = json.getJSONArray("nodes");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject dirJson = array.getJSONObject(i);
            MetaDirectory dir = new MetaDirectory(dirJson);
            this.nodes.add(dir);
        }
    }

    public Directory getFirst() {
        return this.nodes.getFirst();
    }

    public Directory getLast() {
        return this.nodes.getLast();
    }

    public List<Directory> getNodes() {
        return this.nodes;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("first", this.getFirst().getId().longValue());
        json.put("last", this.getLast().getId().longValue());

        JSONArray array = new JSONArray();
        for (Directory dir : this.nodes) {
            array.put(dir.toCompactJSON());
        }
        json.put("nodes", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
