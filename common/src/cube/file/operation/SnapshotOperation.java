/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.VideoOperation;
import cube.util.FileType;
import cube.util.TimeDuration;
import org.json.JSONObject;

/**
 * 视频快照操作。
 */
public class SnapshotOperation extends VideoOperation {

    public final static String Operation = "Snapshot";

    public TimeDuration offset;

    public TimeDuration duration;

    public double rate;

    public FileType outputType;

    public boolean packToZip = true;

//    private List<TimeOffset> timeOffsets;

    public SnapshotOperation() {
        super();
        this.offset = new TimeDuration(0, 0, 0);
        this.duration = new TimeDuration(0, 0, 0,0);
        this.rate = 1;
        this.outputType = FileType.JPEG;
    }

    public SnapshotOperation(JSONObject json) {
        super();
        this.offset = new TimeDuration(json.getJSONObject("offset"));
        this.duration = new TimeDuration(json.getJSONObject("duration"));
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
        json.put("offset", this.offset.toJSON());
        json.put("duration", this.duration.toJSON());
        json.put("rate", this.rate);
        json.put("outputType", this.outputType.getPreferredExtension());
        json.put("packToZip", this.packToZip);
        return json;
    }
}
