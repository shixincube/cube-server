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

import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;

/**
 * 文件标签。
 */
public class FileLabel extends Entity {

    private Long ownerId;

    private String fileName;

    private long fileSize;

    private long completedTime;

    private String fileCode;

    private String md5Code;

    private String sha1Code;

    public FileLabel(Long ownerId, String domainName, String fileName, long fileSize,
                     long time, String fileCode) {
        super(Utils.generateSerialNumber(), domainName);
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.completedTime = time;
        this.fileCode = fileCode;
    }

    public FileLabel(JSONObject json) {
        super();
        try {
            this.id = json.getLong("id");
            this.domain = new Domain(json.getString("domain"));
            this.ownerId = json.getLong("ownerId");
            this.fileName = json.getString("fileName");
            this.fileSize = json.getLong("fileSize");
            this.completedTime = json.getLong("completed");
            this.fileCode = json.getString("fileCode");
            if (json.has("md5")) {
                this.md5Code = json.getString("md5");
            }
            if (json.has("sha1")) {
                this.sha1Code = json.getString("sha1");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public long getCompletedTime() {
        return this.completedTime;
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public void setMD5Code(String md5Code) {
        this.md5Code = md5Code;
    }

    public String getMD5Code() {
        return this.md5Code;
    }

    public void setSHA1Code(String sha1Code) {
        this.sha1Code = sha1Code;
    }

    public String getSHA1Code() {
        return this.sha1Code;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("domain", this.domain);
            json.put("ownerId", this.ownerId);
            json.put("fileName", this.fileName);
            json.put("fileSize", this.fileSize);
            json.put("completed", this.completedTime);
            json.put("fileCode", this.fileCode);
            if (null != this.md5Code) {
                json.put("md5", this.md5Code);
            }
            if (null != this.sha1Code) {
                json.put("sha1", this.sha1Code);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
