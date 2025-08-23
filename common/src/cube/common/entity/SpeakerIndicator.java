/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.composition.Emotion;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SpeakerIndicator implements JSONable {

    public String speaker;

    public String label;

    public double totalDuration;

    public int totalWords;

    public Map<Emotion, AtomicInteger> emotionCounts;

    /**
     * 说话比例
     */
    public int durationRatio;

    /**
     * 说话节奏，w/m，每分钟词数。
     */
    public int rhythm;

    /**
     * 情绪比例。
     */
    public EmotionRatio emotionRatio;

    /**
     * 分段的内容指标。
     */
    public List<SpeakerSegmentIndicator> segmentIndicators;

    public int positiveSegmentRatio;
    public int negativeSegmentRatio;
    public int neutralSegmentRatio;

    public SpeakerIndicator(String speaker, String label) {
        this.speaker = speaker;
        this.label = label;
        this.emotionCounts = new HashMap<>();
        for (Emotion emotion : Emotion.commonEmotions()) {
            this.emotionCounts.put(emotion, new AtomicInteger(0));
        }
        this.segmentIndicators = new ArrayList<>();
    }

    public SpeakerIndicator(JSONObject json) {
        this.speaker = json.getString("speaker");
        this.label = json.getString("label");
        this.totalDuration = json.getDouble("totalDuration");
        this.totalWords = json.getInt("totalWords");

        this.emotionCounts = new HashMap<>();
        JSONObject emotionCountsMap = json.getJSONObject("emotionCounts");
        for (String key : emotionCountsMap.keySet()) {
            int value = emotionCountsMap.getInt(key);
            this.emotionCounts.put(Emotion.parse(key), new AtomicInteger(value));
        }

        this.durationRatio = json.getInt("durationRatio");
        this.rhythm = json.getInt("rhythm");
        this.emotionRatio = new EmotionRatio(json.getJSONObject("emotionRatio"));

        this.segmentIndicators = new ArrayList<>();
        JSONArray segmentIndicatorArray = json.getJSONArray("segmentIndicators");
        for (int i = 0; i < segmentIndicatorArray.length(); ++i) {
            this.segmentIndicators.add(new SpeakerSegmentIndicator(segmentIndicatorArray.getJSONObject(i)));
        }

        this.positiveSegmentRatio = json.getInt("positiveSegmentRatio");
        this.negativeSegmentRatio = json.getInt("negativeSegmentRatio");
        this.neutralSegmentRatio = json.getInt("neutralSegmentRatio");
    }

    public void addSegmentIndicator(SpeakerSegmentIndicator segmentIndicator) {
        this.segmentIndicators.add(segmentIndicator);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("speaker", this.speaker);
        json.put("label", this.label);
        json.put("totalDuration", this.totalDuration);
        json.put("totalWords", this.totalWords);

        JSONObject emotionCountsMap = new JSONObject();
        for (Map.Entry<Emotion, AtomicInteger> e : this.emotionCounts.entrySet()) {
            emotionCountsMap.put(e.getKey().name(), e.getValue().get());
        }
        json.put("emotionCounts", emotionCountsMap);

        json.put("durationRatio", this.durationRatio);
        json.put("rhythm", this.rhythm);
        json.put("emotionRatio", this.emotionRatio.toJSON());

        JSONArray segmentIndicatorArray = new JSONArray();
        for (SpeakerSegmentIndicator indicator : this.segmentIndicators) {
            segmentIndicatorArray.put(indicator.toJSON());
        }
        json.put("segmentIndicators", segmentIndicatorArray);

        json.put("positiveSegmentRatio", this.positiveSegmentRatio);
        json.put("negativeSegmentRatio", this.negativeSegmentRatio);
        json.put("neutralSegmentRatio", this.neutralSegmentRatio);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
