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

import cube.file.operation.SnapshotOperation;
import cube.util.FileType;
import cube.util.TimeOffset;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 快照设置。
 */
public class SnapshotContext extends VideoProcessorContext {

    public TimeOffset timeOffset;

    public TimeOffset duration;

    public double rate;

    public FileType outputType;

    /**
     * 将结果打包到 ZIP 文件。
     */
    public boolean packToZip = true;

    /**
     * 是否复制文件到工作目录下。
     */
    public boolean copyToWorkPath = false;

    private List<File> outputFileList;

    private List<TimeOffset> fileTimingPoints;

    public SnapshotContext() {
        this.timeOffset = new TimeOffset(0, 0, 0);
        this.duration = new TimeOffset(0, 0, 0,0);
        this.rate = 0.5;
        this.outputType = FileType.JPEG;
    }

    public void setVideoOperation(SnapshotOperation value) {
        super.setVideoOperation(value);
        this.timeOffset = value.timeOffset;
        this.duration = value.duration;
        this.rate = value.rate;
        this.outputType = value.outputType;
    }

    public void setOutputFiles(List<File> outputFileList) {
        this.outputFileList = outputFileList;
        this.fileTimingPoints = new ArrayList<>(outputFileList.size());

        double delta = 1.0f / this.rate;
        int millisStep = (int) Math.round(delta * 1000.0);

        int millis = 0;
        for (int i = 0; i < outputFileList.size(); ++i) {
            this.fileTimingPoints.add(this.timeOffset.increment(millis, Calendar.MILLISECOND));
            millis += millisStep;
        }
    }

    public List<File> getOutputFiles() {
        return this.outputFileList;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("timeOffset", this.timeOffset.toJSON());
        json.put("duration", this.duration.toJSON());
        json.put("rate", this.rate);
        json.put("outputType", this.outputType.getPreferredExtension());

        JSONArray fileTimingPointArray = new JSONArray();
        for (TimeOffset timing : this.fileTimingPoints) {
            fileTimingPointArray.put(timing.toJSON());
        }
        json.put("timingPoints", fileTimingPointArray);

        List<String> logs = this.getStdOutput();
        if (null != logs) {
            JSONArray array = new JSONArray();
            for (String log : logs) {
                array.put(log);
            }
            json.put("logs", array);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
