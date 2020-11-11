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

import cell.util.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层结构节点。
 */
public class HierarchyNode extends Entity {

    private HierarchyNode parent;

    private List<HierarchyNode> children;

    private List<Entity> contents;

    public HierarchyNode() {
        this.children = new ArrayList<>();
        this.contents = new ArrayList<>();
    }

    public HierarchyNode(HierarchyNode parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
        this.contents = new ArrayList<>();
    }

    public HierarchyNode(JSONObject json) {
        this.children = new ArrayList<>();
        this.contents = new ArrayList<>();


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

        this.children.add(child);
    }

    public void removeChild(HierarchyNode child) {
        this.children.remove(child);
    }

    public List<HierarchyNode> getChildren() {
        return new ArrayList<>(this.children);
    }

    public void addContent(Entity entity) {
        if (this.contents.contains(entity)) {
            return;
        }

        this.contents.add(entity);
    }

    public void removeContent(Entity entity) {
        this.contents.remove(entity);
    }

    public Entity getContent(Long id) {
        for (int i = 0, size = this.contents.size(); i < size; ++i) {
            Entity entity = this.contents.get(i);
            if (entity.id.longValue() == id.longValue()) {
                return entity;
            }
        }

        return null;
    }

    public List<Entity> getContents() {
        return new ArrayList<>(this.contents);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        return json;
    }
}
