/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 文件存储服务偏好配置。
 */
public class FileStoragePerformance implements JSONable {

    private long contactId;

    private long maxSpaceSize;

    private long uploadThreshold;

    private long downloadThreshold;

    private long spaceSize;

    public FileStoragePerformance(long contactId, long maxSpaceSize,
                                  long uploadThreshold, long downloadThreshold) {
        this.contactId = contactId;
        this.maxSpaceSize = maxSpaceSize;
        this.uploadThreshold = uploadThreshold;
        this.downloadThreshold = downloadThreshold;
    }

    public long getContactId() {
        return this.contactId;
    }

    public long getMaxSpaceSize() {
        return this.maxSpaceSize;
    }

    public long getUploadThreshold() {
        return this.uploadThreshold;
    }

    public long getDownloadThreshold() {
        return this.downloadThreshold;
    }

    public long getSpaceSize() {
        return this.spaceSize;
    }

    public void setSpaceSize(long spaceSize) {
        this.spaceSize = spaceSize;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("maxSpaceSize", this.maxSpaceSize);
        json.put("uploadThreshold", this.uploadThreshold);
        json.put("downloadThreshold", this.downloadThreshold);
        json.put("spaceSize", this.spaceSize);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}