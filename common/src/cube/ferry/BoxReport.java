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

package cube.ferry;

import cell.util.Utils;
import cube.common.entity.Entity;
import org.json.JSONObject;

/**
 * 报告。
 */
public class BoxReport extends Entity {

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

    public BoxReport(String domainName) {
        super(Utils.randomUnsignedLong(), domainName);
    }

    public BoxReport(JSONObject json) {
        super(json);
    }

    public void setImageFilesUsedSize(long size) {
        this.imageFilesUsedSize = size;
    }

    public void setDocFilesUsedSize(long size) {
        this.docFilesUsedSize = size;
    }

    public void setVideoFilesUsedSize(long size) {
        this.videoFilesUsedSize = size;
    }

    public void setAudioFilesUsedSize(long size) {
        this.audioFilesUsedSize = size;
    }

    public void setPackageFilesUsedSize(long size) {
        this.packageFilesUsedSize = size;
    }

    public void setOtherFilesUsedSize(long size) {
        this.otherFilesUsedSize = size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }
}
