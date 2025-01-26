/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.system;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 文件描述符。
 */
public class FileDescriptor implements JSONable {

    private String fileSystem;

    private String fileName;

    private String url;

    private JSONObject descriptor;

    private long timestamp;

    /**
     * 构造函数。
     *
     * @param fileSystem
     * @param url
     */
    public FileDescriptor(String fileSystem, String fileName, String url) {
        this.fileSystem = fileSystem;
        this.fileName = fileName;
        this.url = url;
        this.descriptor = new JSONObject();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public FileDescriptor(JSONObject json) {
        this.fileSystem = json.getString("system");
        this.fileName = json.getString("filename");
        this.url = json.getString("url");
        this.descriptor = json.getJSONObject("descriptor");
        this.timestamp = System.currentTimeMillis();
    }

    public String getFileSystem() {
        return this.fileSystem;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getURL() {
        return this.url;
    }

    public JSONObject getDescriptor() {
        return this.descriptor;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void attr(String name, String value) {
        this.descriptor.put(name, value);
    }

    public void attr(String name, long value) {
        this.descriptor.put(name, value);
    }

    public void attr(String name, int value) {
        this.descriptor.put(name, value);
    }

    public String attr(String name) {
        return this.descriptor.getString(name);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("system", this.fileSystem);
        json.put("filename", this.fileName);
        json.put("url", this.url);
        json.put("descriptor", this.descriptor);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    @Override
    public String toString() {
        return this.descriptor.toString();
    }
}
