/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.Language;
import cube.util.ConfigUtils;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceDiarization extends Entity {

    public final static String[] SPEAKER_NAMES = new String[] {
            "张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十", "徐大", "田二"
    };

    public final static String[] SPEAKER_DISPLAY_NAMES = new String[] {
            "说话人1", "说话人2", "说话人3", "说话人4", "说话人5", "说话人6", "说话人7", "说话人8", "说话人9", "说话人10"
    };

    public final static String LABEL_CUSTOMER = "customer";

    public final static String LABEL_COUNSELOR = "counselor";

    public long contactId;

    public String title;

    public String remark;

    public String fileCode;

    public FileLabel file;

    /**
     * 单位：秒。
     */
    public double duration;

    public long elapsed;

    public List<VoiceTrack> tracks;

    public VoiceIndicator indicator;

    public String analysis;

    public String suggestion;

    public VoiceDiarization(JSONObject json) {
        super(json);

        // 处理 ID
        if (!json.has("id")) {
            this.id = ConfigUtils.generateSerialNumber();
        }

        if (json.has("contactId")) {
            this.contactId = json.getLong("contactId");
        }
        if (json.has("title")) {
            this.title = json.getString("title");
        }
        if (json.has("remark")) {
            this.remark = json.getString("remark");
        }
        if (json.has("fileCode")) {
            this.fileCode = json.getString("fileCode");
        }
        if (json.has("file")) {
            this.file = new FileLabel(json.getJSONObject("file"));
            this.fileCode = this.file.getFileCode();
        }
        this.duration = json.getDouble("duration");
        this.elapsed = json.getLong("elapsed");
        this.tracks = new ArrayList<>();
        JSONArray array = json.getJSONArray("tracks");
        for (int i = 0; i < array.length(); ++i) {
            this.tracks.add(new VoiceTrack(array.getJSONObject(i)));
        }

        if (json.has("indicator")) {
            this.indicator = new VoiceIndicator(json.getJSONObject("indicator"));
        }

        if (json.has("analysis")) {
            this.analysis = json.getString("analysis");
        }

        if (json.has("suggestion")) {
            this.suggestion = json.getString("suggestion");
        }
    }

    public VoiceDiarization(long id, long timestamp, long contactId, String title, String remark, String fileCode,
                            double duration, long elapsed) {
        super(id, "", timestamp);
        this.contactId = contactId;
        this.title = title;
        this.remark = remark;
        this.fileCode = fileCode;
        this.duration = duration;
        this.elapsed = elapsed;
        this.tracks = new ArrayList<>();
    }

    public void setTrackLabel(String oldLabel, String newLabel) {
        for (VoiceTrack track : this.tracks) {
            if (track.label.equals(oldLabel)) {
                track.label = newLabel;
            }
        }
    }

    public List<String> extractTrackLabels() {
        List<String> result = new ArrayList<>();
        for (VoiceTrack track : this.tracks) {
            if (result.contains(track.label)) {
                continue;
            }
            result.add(track.label);
        }
        return result;
    }

    public int alignSpeakerLabels() {
        HashMap<String, List<VoiceTrack>> map = new HashMap<>();
        for (int i = 0; i < this.tracks.size(); ++i) {
            VoiceTrack track = this.tracks.get(i);
            track.track = String.valueOf(i + 1);

            if (map.containsKey(track.label)) {
                List<VoiceTrack> trackList = map.get(track.label);
                trackList.add(track);
            }
            else {
                List<VoiceTrack> trackList = new ArrayList<>();
                trackList.add(track);
                map.put(track.label, trackList);
            }
        }

        int index = 0;
        for (String key : map.keySet()) {
            String newName = SPEAKER_NAMES[index];
            ++index;
            List<VoiceTrack> trackList = map.get(key);
            for (VoiceTrack track : trackList) {
                track.label = newName;
            }
        }

        return map.size();
    }

    public void guessSpeakerLabels() {
        HashMap<String, List<VoiceTrack>> map = new HashMap<>();
        for (int i = 0; i < this.tracks.size(); ++i) {
            VoiceTrack track = this.tracks.get(i);
            track.track = String.valueOf(i + 1);

            if (map.containsKey(track.label)) {
                List<VoiceTrack> trackList = map.get(track.label);
                trackList.add(track);
            }
            else {
                List<VoiceTrack> trackList = new ArrayList<>();
                trackList.add(track);
                map.put(track.label, trackList);
            }
        }

        if (map.size() == 1) {
            for (VoiceTrack track : this.tracks) {
                track.label = LABEL_COUNSELOR;
            }
        }
        else if (map.size() >= 2) {
            VoiceTrack first = this.tracks.get(0);
            String firstLabel = first.label;
            String otherLabel = "";
            for (VoiceTrack track : this.tracks) {
                if (!track.label.equals(firstLabel)) {
                    otherLabel = track.label;
                    break;
                }
            }

            for (VoiceTrack track : this.tracks) {
                if (track.label.equals(firstLabel)) {
                    track.label = "counselor";
                }
                else if (track.label.equals(otherLabel)) {
                    track.label = "customer";
                }
            }
        }
    }

    /**
     * 优化说话者标签显示。
     *
     * @param language
     */
    public void enhanceSpeakerDisplayNames(Language language) {
        Map<String, String> usedNameMap = new HashMap<>();
        for (int i = 0; i < this.tracks.size(); ++i) {
            VoiceTrack track = this.tracks.get(i);
            if (usedNameMap.containsKey(track.display)) {
                track.display = usedNameMap.get(track.display);
            }
            else {
                String oldDisplay = track.display;
                String newDisplay = SPEAKER_DISPLAY_NAMES[usedNameMap.size()];
                usedNameMap.put(oldDisplay, newDisplay);
                track.display = newDisplay;
            }
        }
    }

    public String buildSpeechText(boolean merge) {
        StringBuilder buf = new StringBuilder();
        if (merge) {
            String lastDisplay = null;
            for (VoiceTrack track : this.tracks) {
                if (null != lastDisplay && lastDisplay.equalsIgnoreCase(track.display)) {
                    buf.delete(buf.length() - 1, buf.length());
                    buf.append(track.recognition.text);
                    buf.append("\n");
                }
                else {
                    buf.append(track.display).append(TextUtils.gColonInChinese).append(track.recognition.text);
                    buf.append("\n");
                }
                lastDisplay = track.display;
            }
        }
        else {
            for (VoiceTrack track : this.tracks) {
                buf.append(track.display).append(TextUtils.gColonInChinese).append(track.recognition.text);
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);

        if (null != this.title) {
            json.put("title", this.title);
        }
        else {
            json.put("title", "");
        }

        if (null != this.remark) {
            json.put("remark", this.remark);
        }
        else {
            json.put("remark", "");
        }

        if (null != this.file) {
            json.put("file", this.file.toJSON());
        }
        json.put("fileCode", this.fileCode);
        json.put("duration", this.duration);
        json.put("elapsed", this.elapsed);
        JSONArray array = new JSONArray();
        for (VoiceTrack track : this.tracks) {
            array.put(track.toJSON());
        }
        json.put("tracks", array);

        if (null != this.indicator) {
            json.put("indicator", this.indicator.toJSON());
        }

        if (null != this.analysis) {
            json.put("analysis", this.analysis);
        }

        if (null != this.suggestion) {
            json.put("suggestion", this.suggestion);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
