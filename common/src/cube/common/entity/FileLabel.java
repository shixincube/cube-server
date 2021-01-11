/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
import cube.common.Domain;
import cube.util.FileType;
import org.json.JSONException;
import org.json.JSONObject;

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
     * @param domainName 域名称。
     * @param fileCode 文件码。
     * @param ownerId 文件所有人 ID 。
     * @param fileName 文件名。
     * @param fileSize 文件大小。
     * @param completedTime 文件进入系统的时间。
     * @param expiryTime 文件的到期时间。
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
     * @param id 文件标签 ID 。
     * @param domainName 域名称。
     * @param fileCode 文件码。
     * @param ownerId 文件所有人 ID 。
     * @param fileName 文件名。
     * @param fileSize 文件大小。
     * @param completedTime 文件进入系统的时间。
     * @param expiryTime 文件的到期时间。
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
     * @param json 文件标签的 JSON 格式。
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

    /**
     * 获取所有人 ID 。
     *
     * @return 所有人 ID 。
     */
    public Long getOwnerId() {
        return this.ownerId;
    }

    /**
     * 获取文件名。
     *
     * @return 返回文件名。
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * 获取文件大小。
     *
     * @return 返回文件大小。
     */
    public long getFileSize() {
        return this.fileSize;
    }

    /**
     * 获取文件完成存储的时间。
     *
     * @return 返回文件完成存储的时间。
     */
    public long getCompletedTime() {
        return this.completedTime;
    }

    /**
     * 设置有效期。
     *
     * @param expiryTime 指定有效期。
     */
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * 获取有效期。
     *
     * @return 返回有效期。
     */
    public long getExpiryTime() { return this.expiryTime; }

    /**
     * 获取文件码。
     *
     * @return 返回文件码。
     */
    public String getFileCode() {
        return this.fileCode;
    }

    /**
     * 设置文件类型。
     *
     * @param fileType 文件类型。
     */
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    /**
     * 获取文件类型。
     *
     * @return 返回文件类型。
     */
    public FileType getFileType() {
        return this.fileType;
    }

    /**
     * 设置 MD5 码。
     *
     * @param md5Code 指定 MD5 码。
     */
    public void setMD5Code(String md5Code) {
        this.md5Code = md5Code;
    }

    /**
     * 获取 MD5 码。
     *
     * @return 返回 MD5 码。
     */
    public String getMD5Code() {
        return this.md5Code;
    }

    /**
     * 设置 SHA1 码。
     *
     * @param sha1Code 指定 SHA1 码。
     */
    public void setSHA1Code(String sha1Code) {
        this.sha1Code = sha1Code;
    }

    /**
     * 获取 SHA1 码。
     *
     * @return 返回 SHA1 码。
     */
    public String getSHA1Code() {
        return this.sha1Code;
    }

    /**
     * 设置文件的 URL 访问地址。
     *
     * @param fileURL 指定文件的 URL 访问地址。
     * @param fileSecureURL 指定文件的安全 URL 访问地址。
     */
    public void setFileURLs(String fileURL, String fileSecureURL) {
        this.fileURL = fileURL;
        this.fileSecureURL = fileSecureURL;
    }

    /**
     * 获取文件的 URL 访问地址。
     *
     * @return 返回文件的 URL 访问地址。
     */
    public String getFileURL() {
        return this.fileURL;
    }

    /**
     * 获取文件的安全 URL 访问地址。
     *
     * @return 返回文件的安全 URL 访问地址。
     */
    public String getFileSecureURL() {
        return this.fileSecureURL;
    }

    /**
     * 设置文件的直接 URL 地址。
     *
     * @param url 指定 URL 。
     */
    public void setDirectURL(String url) {
        this.directURL = url;
    }

    /**
     * 获取文件的直接 URL 地址。
     *
     * @return 返回文件的直接 URL 地址。
     */
    public String getDirectURL() {
        return this.directURL;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof FileLabel) {
            FileLabel other = (FileLabel) object;
            if (other.fileCode.equals(this.fileCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("directURL")) {
            json.remove("directURL");
        }
        return json;
    }
}
