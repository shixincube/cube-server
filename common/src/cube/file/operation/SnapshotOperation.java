/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.file.operation;

import cube.file.VideoOperation;
import cube.util.FileType;
import cube.util.TimeOffset;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频快照操作。
 */
public class SnapshotOperation extends VideoOperation {

    public final static String Operation = "Snapshot";

    public TimeOffset timeOffset;

    public TimeOffset duration;

    public double rate;

    public FileType outputType;

    public boolean packToZip = true;

//    private List<TimeOffset> timeOffsets;

    public SnapshotOperation() {
        super();
        this.timeOffset = new TimeOffset(0, 0, 0);
        this.duration = new TimeOffset(0, 0, 0,0);
        this.rate = 1;
        this.outputType = FileType.JPEG;
    }

    public SnapshotOperation(JSONObject json) {
        super();
        this.timeOffset = new TimeOffset(json.getJSONObject("timeOffset"));
        this.duration = new TimeOffset(json.getJSONObject("duration"));
        this.rate = json.getDouble("rate");
        this.outputType = FileType.matchExtension(json.getString("outputType"));
        this.packToZip = json.getBoolean("packToZip");

        /*if (json.has("timingPoints")) {
            this.timeOffsets = new ArrayList<>();
            JSONArray array = json.getJSONArray("timingPoints");
            for (int i = 0; i < array.length(); ++i) {
                this.timeOffsets.add(new TimeOffset(array.getJSONObject(i)));
            }
        }*/
    }

    @Override
    public String getOperation() {
        return SnapshotOperation.Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("timeOffset", this.timeOffset.toJSON());
        json.put("duration", this.duration.toJSON());
        json.put("rate", this.rate);
        json.put("outputType", this.outputType.getPreferredExtension());
        json.put("packToZip", this.packToZip);
        return json;
    }
}
