/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchyTool;
import org.json.JSONObject;

/**
 * 垃圾目录。
 */
public class DirectoryTrash extends Trash {

    private Directory directory;

    public DirectoryTrash(Directory root, RecycleChain chain, Directory directory) {
        super(root, chain, directory.getId());
        this.directory = directory;
    }

    public DirectoryTrash(Directory root, JSONObject json) {
        super(root, json);
        this.directory = FileHierarchyTool.unpackDirectory(json.getJSONObject("directory"));
    }

    @Override
    public Directory getParent() {
        return this.directory.getParent();
    }

    public Directory getDirectory() {
        return this.directory;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        JSONObject dirJson = this.directory.toCompactJSON();
        FileHierarchyTool.packDirectory(this.directory, dirJson);
        json.put("directory", dirJson);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        JSONObject dirJson = this.directory.toCompactJSON();
        FileHierarchyTool.packDirectory(this.directory, dirJson);
        json.put("directory", dirJson);
        return json;
    }
}
