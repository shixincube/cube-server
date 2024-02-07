/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.ferry;

import cell.util.Utils;
import cube.common.entity.Entity;
import org.json.JSONObject;

/**
 * 报告。
 */
public class BoxReport extends Entity {

    /**
     * 数据空间大小。
     */
    private long dataSpaceSize;

    private long imageFilesUsedSize;

    private long docFilesUsedSize;

    private long videoFilesUsedSize;

    private long audioFilesUsedSize;

    private long packageFilesUsedSize;

    private long otherFilesUsedSize;

    /**
     * 空闲磁盘空间。
     */
    private long freeDiskSize;

    private int numImageFiles;

    private int numDocFiles;

    private int numVideoFiles;

    private int numAudioFiles;

    private int numPackageFiles;

    private int numOtherFiles;

    /**
     * 消息数量。
     */
    private int totalMessages;

    public BoxReport(String domainName) {
        super(Utils.randomUnsignedLong(), domainName);
    }

    public BoxReport(JSONObject json) {
        super(json);
        this.dataSpaceSize = json.getLong("dataSpaceSize");
        this.freeDiskSize = json.getLong("freeDiskSize");
        this.imageFilesUsedSize = json.getLong("imageFilesUsedSize");
        this.docFilesUsedSize = json.getLong("docFilesUsedSize");
        this.videoFilesUsedSize = json.getLong("videoFilesUsedSize");
        this.audioFilesUsedSize = json.getLong("audioFilesUsedSize");
        this.packageFilesUsedSize = json.getLong("packageFilesUsedSize");
        this.otherFilesUsedSize = json.getLong("otherFilesUsedSize");

        this.numImageFiles = json.getInt("numImageFiles");
        this.numDocFiles = json.getInt("numDocFiles");
        this.numVideoFiles = json.getInt("numVideoFiles");
        this.numAudioFiles = json.getInt("numAudioFiles");
        this.numPackageFiles = json.getInt("numPackageFiles");
        this.numOtherFiles = json.getInt("numOtherFiles");

        this.totalMessages = json.getInt("totalMessages");
    }

    public void setDataSpaceSize(long size) {
        this.dataSpaceSize = size;
    }

    public long getDataSpaceSize() {
        return this.dataSpaceSize;
    }

    public void setFreeDiskSize(long size) {
        this.freeDiskSize = size;
    }

    public long getFreeDiskSize() {
        return this.freeDiskSize;
    }

    public void setImageFilesUsedSize(long size) {
        this.imageFilesUsedSize = size;
    }

    public long getImageFilesUsedSize() {
        return this.imageFilesUsedSize;
    }

    public void setDocFilesUsedSize(long size) {
        this.docFilesUsedSize = size;
    }

    public long getDocFilesUsedSize() {
        return this.docFilesUsedSize;
    }

    public void setVideoFilesUsedSize(long size) {
        this.videoFilesUsedSize = size;
    }

    public long getVideoFilesUsedSize() {
        return this.videoFilesUsedSize;
    }

    public void setAudioFilesUsedSize(long size) {
        this.audioFilesUsedSize = size;
    }

    public long getAudioFilesUsedSize() {
        return this.audioFilesUsedSize;
    }

    public void setPackageFilesUsedSize(long size) {
        this.packageFilesUsedSize = size;
    }

    public long getPackageFilesUsedSize() {
        return this.packageFilesUsedSize;
    }

    public void setOtherFilesUsedSize(long size) {
        this.otherFilesUsedSize = size;
    }

    public long getOtherFilesUsedSize() {
        return this.otherFilesUsedSize;
    }

    public void setNumImageFiles(int num) {
        this.numImageFiles = num;
    }

    public int getNumImageFiles() {
        return this.numImageFiles;
    }

    public void setNumDocFiles(int num) {
        this.numDocFiles = num;
    }

    public int getNumDocFiles() {
        return this.numDocFiles;
    }

    public void setNumVideoFiles(int num) {
        this.numVideoFiles = num;
    }

    public int getNumVideoFiles() {
        return this.numVideoFiles;
    }

    public void setNumAudioFiles(int num) {
        this.numAudioFiles = num;
    }

    public int getNumAudioFiles() {
        return this.numAudioFiles;
    }

    public void setNumPackageFiles(int num) {
        this.numPackageFiles = num;
    }

    public int getNumPackageFiles() {
        return this.numPackageFiles;
    }

    public void setNumOtherFiles(int num) {
        this.numOtherFiles = num;
    }

    public int getNumOtherFiles() {
        return this.numOtherFiles;
    }

    public void setTotalMessages(int total) {
        this.totalMessages = total;
    }

    public int getTotalMessages() {
        return this.totalMessages;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("dataSpaceSize", this.dataSpaceSize);
        json.put("freeDiskSize", this.freeDiskSize);
        json.put("imageFilesUsedSize", this.imageFilesUsedSize);
        json.put("docFilesUsedSize", this.docFilesUsedSize);
        json.put("videoFilesUsedSize", this.videoFilesUsedSize);
        json.put("audioFilesUsedSize", this.audioFilesUsedSize);
        json.put("packageFilesUsedSize", this.packageFilesUsedSize);
        json.put("otherFilesUsedSize", this.otherFilesUsedSize);

        json.put("numImageFiles", this.numImageFiles);
        json.put("numDocFiles", this.numDocFiles);
        json.put("numVideoFiles", this.numVideoFiles);
        json.put("numAudioFiles", this.numAudioFiles);
        json.put("numPackageFiles", this.numPackageFiles);
        json.put("numOtherFiles", this.numOtherFiles);

        json.put("totalMessages", this.totalMessages);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
