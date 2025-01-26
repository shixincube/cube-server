/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件夹。
 */
public class Folder implements JSONable {

    /**
     * 文件夹/目录 ID 。
     */
    public long id;

    /**
     * 域名称。
     */
    public String domain;

    /**
     * 所有人的 ID 。
     */
    public long ownerId;

    /**
     * 文件夹名。
     */
    public String name;

    /**
     * 创建时间戳。
     */
    public long creationTime;

    /**
     * 最近一次修改时间。
     */
    public long lastModified;

    /**
     * 文件夹占用空间大小。
     */
    public long size;

    /**
     * 是否是隐藏文件夹。
     */
    public boolean hidden;

    /**
     * 文件夹的父文件夹 ID 。
     */
    public long parentId;

    /**
     * 包含的文件夹数量。
     */
    public int numFolders;

    /**
     * 包含的文件数量。
     */
    public int numFiles;

    /**
     * 文件夹的子文件夹列表。
     */
    public List<Folder> children;

    /**
     * 构造函数。
     *
     * @param json 指定 JSON 数据。
     */
    public Folder(JSONObject json) {
        this.children = new ArrayList<>();

        if (json.has("owner")) {
            this.ownerId = json.getLong("owner");
        }
        else {
            this.ownerId = 0;
        }
        if (json.has("parentId")) {
            this.parentId = json.getLong("parentId");
        }
        else {
            this.parentId = 0;
        }

        this.id = json.getLong("id");
        this.domain = json.getString("domain");
        this.name = json.getString("name");
        this.creationTime = json.getLong("creation");
        this.lastModified = json.getLong("lastModified");
        this.size = json.getLong("size");
        this.hidden = json.getBoolean("hidden");
        this.numFolders = json.getInt("numDirs");
        this.numFiles = json.getInt("numFiles");

        JSONArray dirArray = json.getJSONArray("dirs");
        for (int i = 0; i < dirArray.length(); ++i) {
            this.children.add(new Folder(dirArray.getJSONObject(i)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("id", this.id);
        json.put("domain", this.domain);
        json.put("owner", this.ownerId);
        json.put("name", this.name);
        json.put("creation", this.creationTime);
        json.put("lastModified", this.lastModified);
        json.put("size", this.size);
        json.put("hidden", this.hidden);
        json.put("parentId", this.parentId);

        json.put("numDirs", this.numFolders);
        json.put("numFiles", this.numFiles);

        JSONArray dirArray = new JSONArray();
        for (Folder folder : this.children) {
            dirArray.put(folder.toJSON());
        }
        json.put("dirs", dirArray);

        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
