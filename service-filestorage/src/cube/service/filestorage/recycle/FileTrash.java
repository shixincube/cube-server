/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cube.common.entity.FileLabel;
import cube.service.filestorage.hierarchy.Directory;
import org.json.JSONObject;

/**
 * 垃圾文件。
 */
public class FileTrash extends Trash {

    private Directory parent;

    private FileLabel fileLabel;

    public FileTrash(Directory root, RecycleChain chain, FileLabel fileLabel) {
        super(root, chain, fileLabel.getId());
        this.fileLabel = fileLabel;
        this.parent = chain.getLast();
    }

    public FileTrash(Directory root, JSONObject json) {
        super(root, json);
        this.parent = getChain().getLast();
        this.fileLabel = new FileLabel(json.getJSONObject("file"));
    }

    @Override
    public Directory getParent() {
        return this.parent;
    }

    public String getFileCode() {
        return this.fileLabel.getFileCode();
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("file", this.fileLabel.toCompactJSON());
        json.put("parent", this.parent.toCompactJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("file", this.fileLabel.toCompactJSON());
        json.put("parent", this.parent.toCompactJSON());
        return json;
    }
}
