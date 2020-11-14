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
import cube.util.FileType;

/**
 * 文件标签。
 */
public class FileLabel extends Entity {

    /**
     * 文件所有人 ID 。
     */
    private Long ownerId;

    /**
     * 文件名。
     */
    private String fileName;

    /**
     * 文件大小。
     */
    private long fileSize;

    /**
     * 文件的完成时间。
     */
    private long completedTime;

    /**
     * 文件的到期时间。
     */
    private long expiryTime;

    /**
     * 文件访问码。
     */
    private String fileCode;

    /**
     * 文件类型。
     */
    private FileType fileType = FileType.UNKNOWN;

    /**
     * 文件 MD5 码。
     */
    private String md5Code;

    /**
     * 文件 SHA1 码。
     */
    private String sha1Code;

    /**
     * 访问文件的 HTTP URL 。
     */
    private String fileURL;

    /**
     * 访问文件的 HTTPS URL 。
     */
    private String fileSecureURL;

    /**
     * 内部服务的直接访问 URL 。
     */
    private String directURL;

    /**
     * 构造函数。
     *
     * @param domainName
     * @param fileCode
     * @param ownerId
     * @param fileName
     * @param fileSize
     * @param completedTime
     * @param expiryTime
     */
    public FileLabel(String domainName, String fileCode, Long ownerId, String fileName, long fileSize,
                     long completedTime, long expiryTime) {
        super(Utils.generateSerialNumber(), domainName);
        this.uniqueKey = fileCode;      // Unique Key 设置为文件码
        this.fileCode = fileCode;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.completedTime = completedTime;
        this.expiryTime = expiryTime;
    }

    /**
     * 构造函数。
     *
     * @param id
     * @param domainName
     * @param fileCode
     * @param ownerId
     * @param fileName
     * @param fileSize
     * @param completedTime
     * @param expiryTime
     */
    public FileLabel(Long id, String domainName, String fileCode, Long ownerId, String fileName,
                     long fileSize, long completedTime, long expiryTime) {
        super(id, domainName);
        this.uniqueKey = fileCode;      // Unique Key 设置为文件码
        this.fileCode = fileCode;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.completedTime = completedTime;
        this.expiryTime = expiryTime;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public FileLabel(JSONObject json) {
        super();
        try {
            this.id = json.getLong("id");
            this.domain = new Domain(json.getString("domain"));
            this.fileCode = json.getString("fileCode");
            this.ownerId = json.getLong("ownerId");
            this.fileName = json.getString("fileName");
            this.fileSize = json.getLong("fileSize");
            this.completedTime = json.getLong("completed");
            this.expiryTime = json.getLong("expiry");
            this.fileType = FileType.matchExtension(json.getString("fileType"));
            if (json.has("md5")) {
                this.md5Code = json.getString("md5");
            }
            if (json.has("sha1")) {
                this.sha1Code = json.getString("sha1");
            }
            if (json.has("fileURL")) {
                this.fileURL = json.getString("fileURL");
            }
            if (json.has("fileSecureURL")) {
                this.fileSecureURL = json.getString("fileSecureURL");
            }
            if (json.has("directURL")) {
                this.directURL = json.getString("directURL");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.uniqueKey = this.fileCode;
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

    public long getExpiryTime() { return this.expiryTime; }

    public String getFileCode() {
        return this.fileCode;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType getFileType() {
        return this.fileType;
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

    public void setFileURLs(String fileURL, String fileSecureURL) {
        this.fileURL = fileURL;
        this.fileSecureURL = fileSecureURL;
    }

    public String getFileURL() {
        return this.fileURL;
    }

    public String getFileSecureURL() {
        return this.fileSecureURL;
    }

    public void setDirectURL(String url) {
        this.directURL = url;
    }

    public String getDirectURL() {
        return this.directURL;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("domain", this.domain.getName());
            json.put("fileCode", this.fileCode);
            json.put("ownerId", this.ownerId);
            json.put("fileName", this.fileName);
            json.put("fileSize", this.fileSize);
            json.put("completed", this.completedTime);
            json.put("expiry", this.expiryTime);
            json.put("fileType", this.fileType.getPreferredExtension());
            if (null != this.md5Code) {
                json.put("md5", this.md5Code);
            }
            if (null != this.sha1Code) {
                json.put("sha1", this.sha1Code);
            }
            if (null != this.fileURL) {
                json.put("fileURL", this.fileURL);
            }
            if (null != this.fileSecureURL) {
                json.put("fileSecureURL", this.fileSecureURL);
            }
            if (null != this.directURL) {
                json.put("directURL", this.directURL);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("directURL")) {
            json.remove("directURL");
        }
        return json;
    }
}
