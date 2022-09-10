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

package cube.service.fileprocessor.processor.video;

import cube.common.action.FileProcessorAction;
import cube.file.VideoOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import org.json.JSONObject;

import java.io.File;

/**
 * 视频处理器上下文。
 */
public abstract class VideoProcessorContext extends ProcessorContext {

    private VideoOperation videoOperation;

    private File inputFile;

    private long elapsedTime;

    public VideoProcessorContext() {
    }

    public VideoProcessorContext(JSONObject json) {
        super(json);
        this.elapsedTime = json.getLong("elapsed");
    }

    public void setInputFile(File file) {
        this.inputFile = file;
    }

    public File getInputFile() {
        return this.inputFile;
    }

    public void setVideoOperation(VideoOperation value) {
        this.videoOperation = value;
    }

    public VideoOperation getVideoOperation() {
        return this.videoOperation;
    }

    public void setElapsedTime(long value) {
        this.elapsedTime = value;
    }

    public long getElapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.Video.name);
        json.put("operation", this.videoOperation.toJSON());
        json.put("elapsed", this.elapsedTime);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
