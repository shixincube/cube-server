/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cube.common.Domain;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 元目录结构。
 */
public class MetaDirectory extends Directory {

    private JSONObject json;

    private Domain domain;

    private MetaDirectory parent;

    private List<Directory> children;

    private List<FileLabel> files;

    /**
     * 构造函数。
     *
     * @param json
     */
    public MetaDirectory(JSONObject json) {
        super(null, null);
        this.json = json;
        this.domain = new Domain(json.getString("domain"));
        this.children = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    public void setParent(MetaDirectory directory) {
        this.parent = directory;
    }

    public void addChild(MetaDirectory directory) {
        this.children.add(directory);
    }

    @Override
    public boolean addFile(FileLabel fileLabel) {
        this.files.add(fileLabel);
        return true;
    }

    @Override
    public Long getId() {
        return this.json.getLong("id");
    }

    @Override
    public Domain getDomain() {
        return this.domain;
    }

    @Override
    public String getName() {
        return this.json.getString("name");
    }

    @Override
    public long getCreationTime() {
        return this.json.getLong("creation");
    }

    @Override
    public long getLastModified() {
        return this.json.getLong("lastModified");
    }

    @Override
    public long getSize() {
        return this.json.getLong("size");
    }

    @Override
    public boolean isHidden() {
        return this.json.getBoolean("hidden");
    }

    @Override
    public boolean hasParent() {
        return this.json.has("parent");
    }

    @Override
    public Directory getParent() {
        return this.parent;
    }

    @Override
    public int numDirectories() {
        return this.json.getInt("numDirs");
    }

    @Override
    public int numFiles() {
        return this.json.getInt("numFiles");
    }

    @Override
    public List<Directory> getDirectories() {
        return this.children;
    }

    @Override
    public List<FileLabel> listFiles(int beginIndex, int endIndex) {
        int begin = beginIndex;
        int end = endIndex;

        if (end + 1 > this.files.size()) {
            end = this.files.size();
        }

        if (begin >= end) {
            return new ArrayList<>();
        }
        return this.files.subList(begin, end);
    }
}
