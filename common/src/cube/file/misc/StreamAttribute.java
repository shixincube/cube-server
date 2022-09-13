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

package cube.file.misc;

import org.json.JSONObject;

/**
 * 媒体流属性描述。
 */
public class StreamAttribute {

    public int index;

    public String codecName;

    public String codecLongName;

    // video/audio
    public String profile;

    public String codecType;

    public String codecTagString;

    public String codecTag;

    // video
    public int width;

    // video
    public int height;

    // video
    public int codedWidth;

    // video
    public int codedHeight;

    // video
    public int closedCaptions;

    // video
    public int filmGrain;

    // video
    public String sampleAspectRatio;

    // video
    public String displayAspectRatio;

    // video
    public String pixFmt;

    // video
    public int level;

    // video
    public String colorRange;

    // video
    public String colorSpace;

    // video
    public String colorTransfer;

    // video
    public String colorPrimaries;

    // video
    public String chromaLocation;

    // video
    public String fieldOrder;

    // video
    public int refs;

    // video
    public boolean isAvc;

    // video
    public int nalLengthSize;

    // video
    public String id;

    // audio
    public String sampleFmt;

    // audio
    public int sampleRate;

    // audio
    public int channels;

    // audio
    public String channelLayout;

    // audio
    public int bitsPerSample;

    public String rFrameRate;

    public String avgFrameRate;

    public String timeBase;

    // video/audio
    public int startPts = -1;

    // video/audio
    public double startTime = -1;

    public long durationTS;

    public double duration;

    public long bitRate;

    // video
    public int bitsPerRawSample;

    // video/audio
    public int numFrames = 0;

    // video/audio
    public int extradataSize = 0;

    public JSONObject disposition;

    // only audio
    public JSONObject tags;

    public StreamAttribute(JSONObject json) {
        this.index = json.getInt("index");
        this.codecName = json.getString("codec_name");
        this.codecLongName = json.getString("codec_long_name");

        if (json.has("profile")) {
            this.profile = json.getString("profile");
        }

        this.codecType = json.getString("codec_type");
        this.codecTagString = json.getString("codec_tag_string");
        this.codecTag = json.getString("codec_tag");

        if (json.has("width")) {
            this.width = json.getInt("width");
        }
        if (json.has("height")) {
            this.height = json.getInt("height");
        }
        if (json.has("coded_width")) {
            this.codedWidth = json.getInt("coded_width");
        }
        if (json.has("coded_height")) {
            this.codedHeight = json.getInt("coded_height");
        }
        if (json.has("closed_captions")) {
            this.closedCaptions = json.getInt("closed_captions");
        }
        if (json.has("film_grain")) {
            this.filmGrain = json.getInt("film_grain");
        }
        if (json.has("sample_aspect_ratio")) {
            this.sampleAspectRatio = json.getString("sample_aspect_ratio");
        }
        if (json.has("display_aspect_ratio")) {
            this.displayAspectRatio = json.getString("display_aspect_ratio");
        }
        if (json.has("pix_fmt")) {
            this.pixFmt = json.getString("pix_fmt");
        }
        if (json.has("level")) {
            this.level = json.getInt("level");
        }
        if (json.has("color_range")) {
            this.colorRange = json.getString("color_range");
        }
        if (json.has("color_space")) {
            this.colorSpace = json.getString("color_space");
        }
        if (json.has("color_transfer")) {
            this.colorTransfer = json.getString("color_transfer");
        }
        if (json.has("color_primaries")) {
            this.colorPrimaries = json.getString("color_primaries");
        }
        if (json.has("chroma_location")) {
            this.chromaLocation = json.getString("chroma_location");
        }
        if (json.has("field_order")) {
            this.fieldOrder = json.getString("field_order");
        }
        if (json.has("refs")) {
            this.refs = json.getInt("refs");
        }
        if (json.has("is_avc")) {
            this.isAvc = json.getString("is_avc").equalsIgnoreCase("true");
        }
        if (json.has("nal_length_size")) {
            this.nalLengthSize = Integer.parseInt(json.getString("nal_length_size"));
        }
        if (json.has("id")) {
            this.id = json.getString("id");
        }

        if (json.has("sample_fmt")) {
            this.sampleFmt = json.getString("sample_fmt");
        }
        if (json.has("sample_rate")) {
            this.sampleRate = Integer.parseInt(json.getString("sample_rate"));
        }
        if (json.has("channels")) {
            this.channels = json.getInt("channels");
        }
        if (json.has("channel_layout")) {
            this.channelLayout = json.getString("channel_layout");
        }
        if (json.has("bits_per_sample")) {
            this.bitsPerSample = json.getInt("bits_per_sample");
        }

        this.rFrameRate = json.getString("r_frame_rate");
        this.avgFrameRate = json.getString("avg_frame_rate");
        this.timeBase = json.getString("time_base");

        if (json.has("start_pts")) {
            this.startPts = json.getInt("start_pts");
        }
        if (json.has("start_time")) {
            this.startTime = Double.parseDouble(json.getString("start_time"));
        }

        this.durationTS = json.getLong("duration_ts");
        this.duration = Double.parseDouble(json.getString("duration"));
        this.bitRate = Long.parseLong(json.getString("bit_rate"));

        if (json.has("bits_per_raw_sample")) {
            this.bitsPerRawSample = Integer.parseInt(json.getString("bits_per_raw_sample"));
        }
        if (json.has("nb_frames")) {
            this.numFrames = Integer.parseInt(json.getString("nb_frames"));
        }
        if (json.has("extradata_size")) {
            this.extradataSize = json.getInt("extradata_size");
        }

        if (json.has("disposition")) {
            this.disposition = json.getJSONObject("disposition");
        }

        if (json.has("tags")) {
            this.tags = json.getJSONObject("tags");
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("index", this.index);
        json.put("codec_name", this.codecName);
        json.put("codec_long_name", this.codecLongName);
        json.put("codec_type", this.codecType);
        json.put("codec_tag_string", this.codecTagString);
        json.put("codec_tag", this.codecTag);

        if (null != this.profile) {
            json.put("profile", this.profile);
        }

        if (this.codecType.equalsIgnoreCase("video")) {
            json.put("width", this.width);
            json.put("height", this.height);
            json.put("coded_width", this.codedWidth);
            json.put("coded_height", this.codedHeight);
            json.put("closed_captions", this.closedCaptions);
            json.put("film_grain", this.filmGrain);
            json.put("sample_aspect_ratio", this.sampleAspectRatio);
            json.put("display_aspect_ratio", this.displayAspectRatio);
            json.put("pix_fmt", this.pixFmt);
            json.put("level", this.level);
            json.put("color_range", this.colorRange);
            json.put("color_space", this.colorSpace);
            json.put("color_transfer", this.colorTransfer);
            json.put("color_primaries", this.colorPrimaries);
            json.put("chroma_location", this.chromaLocation);
            json.put("field_order", this.fieldOrder);
            json.put("refs", this.refs);
            json.put("is_avc", this.isAvc ? "true" : "false");
            json.put("nal_length_size", Integer.toString(this.nalLengthSize));
            json.put("bits_per_raw_sample", Integer.toString(this.bitsPerRawSample));
        }
        else if (this.codecType.equalsIgnoreCase("audio")) {
            json.put("sample_fmt", this.sampleFmt);
            json.put("sample_rate", Integer.toString(this.sampleRate));
            json.put("channels", this.channels);
            json.put("bits_per_sample", this.bitsPerSample);

            if (null != this.channelLayout) {
                json.put("channel_layout", this.channelLayout);
            }
        }

        if (null != this.id) {
            json.put("id", this.id);
        }

        if (this.startPts >= 0) {
            json.put("start_pts", this.startPts);
            json.put("start_time", Double.toString(this.startTime));
        }

        if (this.numFrames > 0) {
            json.put("nb_frames", Integer.toString(this.numFrames));
        }
        if (this.extradataSize > 0) {
            json.put("extradata_size", this.extradataSize);
        }

        json.put("r_frame_rate", this.rFrameRate);
        json.put("avg_frame_rate", this.avgFrameRate);
        json.put("time_base", this.timeBase);
        json.put("duration_ts", this.durationTS);
        json.put("duration", Double.toString(this.duration));
        json.put("bit_rate", Long.toString(this.bitRate));

        if (null != this.disposition) {
            json.put("disposition", this.disposition);
        }

        if (null != this.tags) {
            json.put("tags", this.tags);
        }

        return json;
    }
}
