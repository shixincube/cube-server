/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
