/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 文件存储服务偏好配置。
 */
public class FileStoragePerformance implements JSONable {

    private long contactId;

    private long spaceSize;

    private long maxSpaceSize;

    private long uploadThreshold;

    private long downloadThreshold;

    private int maxSharingNum = 0;

    private boolean sharingWatermarkEnabled = true;

    private boolean sharingPreviewEnabled = true;

    public FileStoragePerformance(long contactId, long maxSpaceSize,
                                  long uploadThreshold, long downloadThreshold,
                                  int maxSharingNum, boolean sharingWatermarkEnabled,
                                  boolean sharingPreviewEnabled) {
        this.contactId = contactId;
        this.spaceSize = -1;
        this.maxSpaceSize = maxSpaceSize;
        this.uploadThreshold = uploadThreshold;
        this.downloadThreshold = downloadThreshold;
        this.maxSharingNum = maxSharingNum;
        this.sharingWatermarkEnabled = sharingWatermarkEnabled;
        this.sharingPreviewEnabled = sharingPreviewEnabled;
    }

    public FileStoragePerformance(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.spaceSize = json.has("spaceSize") ? json.getLong("spaceSize") : -1;
        this.maxSpaceSize = json.getLong("maxSpaceSize");
        this.uploadThreshold = json.getLong("uploadThreshold");
        this.downloadThreshold = json.getLong("downloadThreshold");
        this.maxSharingNum = json.getInt("maxSharingNum");
        this.sharingWatermarkEnabled = json.getBoolean("sharingWatermarkEnabled");
        this.sharingPreviewEnabled = json.getBoolean("sharingPreviewEnabled");
    }

    public long getContactId() {
        return this.contactId;
    }

    public long getSpaceSize() {
        return this.spaceSize;
    }

    public void setSpaceSize(long spaceSize) {
        this.spaceSize = spaceSize;
    }

    public long getMaxSpaceSize() {
        return this.maxSpaceSize;
    }

    public void setMaxSpaceSize(long value) {
        this.maxSpaceSize = value;
    }

    public long getUploadThreshold() {
        return this.uploadThreshold;
    }

    public void setUploadThreshold(long value) {
        this.uploadThreshold = value;
    }

    public long getDownloadThreshold() {
        return this.downloadThreshold;
    }

    public void setDownloadThreshold(long value) {
        this.downloadThreshold = value;
    }

    public int getMaxSharingNum() {
        return this.maxSharingNum;
    }

    public void setMaxSharingNum(int value) {
        this.maxSharingNum = value;
    }

    public boolean isSharingWatermarkEnabled() {
        return this.sharingWatermarkEnabled;
    }

    public void setSharingWatermarkEnabled(boolean value) {
        this.sharingWatermarkEnabled = value;
    }

    public boolean isSharingPreviewEnabled() {
        return this.sharingPreviewEnabled;
    }

    public void setSharingPreviewEnabled(boolean value) {
        this.sharingPreviewEnabled = value;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("maxSpaceSize", this.maxSpaceSize);
        json.put("uploadThreshold", this.uploadThreshold);
        json.put("downloadThreshold", this.downloadThreshold);
        json.put("maxSharingNum", this.maxSharingNum);
        json.put("sharingWatermarkEnabled", this.sharingWatermarkEnabled);
        json.put("sharingPreviewEnabled", this.sharingPreviewEnabled);

        if (this.spaceSize >= 0) {
            json.put("spaceSize", this.spaceSize);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
