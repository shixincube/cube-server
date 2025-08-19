/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.Sentiment;
import cube.aigc.psychology.composition.Emotion;
import cube.common.JSONable;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.VoiceDiarization;
import cube.common.entity.VoiceTrack;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceIndicator implements JSONable {

    private final static String sPromptSentiment = "已知内容：%s。\n\n判断以上内容的情绪倾向属于以下哪种：\n\n* 积极的\n* 消极的\n* 中立的\n* 积极和消极均有\n。仅回答属于以上那种情绪倾向。";

    public enum Tense {
        Future,
        Past
    }

    private static final Map<Emotion, Double> sEmotionWeight = new HashMap<>();

    static {
        if (sEmotionWeight.isEmpty()) {
            sEmotionWeight.put(Emotion.Angry, -0.2);
            sEmotionWeight.put(Emotion.Fearful, -0.3);
            sEmotionWeight.put(Emotion.Happy, 0.8);
            sEmotionWeight.put(Emotion.Neutral, 0.0);
            sEmotionWeight.put(Emotion.Sad, -0.5);
            sEmotionWeight.put(Emotion.Surprise, 0.2);
        }
    }

    public double silenceDuration;

    public Map<String, SpeakerIndicator> speakerIndicators = new HashMap<>();

    public VoiceIndicator() {
    }

    public VoiceIndicator(JSONObject json) {
        this.silenceDuration = json.getDouble("silenceDuration");
        JSONObject speakerIndicatorMap = json.getJSONObject("speakerIndicators");
        for (String key : speakerIndicatorMap.keySet()) {
            JSONObject value = speakerIndicatorMap.getJSONObject(key);
            this.speakerIndicators.put(key, new SpeakerIndicator(value));
        }
    }

    public void analyse(AIGCService service, VoiceDiarization voiceDiarization) {
        if (voiceDiarization.duration == 0) {
            return;
        }

        double totalDuration = 0;

        for (VoiceTrack track : voiceDiarization.tracks) {
            SpeakerIndicator speakerIndicator = this.speakerIndicators.get(track.label);
            if (null == speakerIndicator) {
                speakerIndicator = new SpeakerIndicator(track.label, track.label);
                this.speakerIndicators.put(track.label, speakerIndicator);
            }

            // 总时长
            speakerIndicator.totalDuration += track.segment.duration;
            totalDuration += track.segment.duration;

            // 总词数
            speakerIndicator.totalWords += track.recognition.words.size();

            // 情绪计数
            AtomicInteger counts = speakerIndicator.emotionCounts.get(track.emotion.emotion);
            if (null != counts) {
                counts.incrementAndGet();
            }
            else {
                Logger.w(this.getClass(), "#build - No emotion: " + track.emotion.emotion.name());
            }

            Sentiment sentiment = this.evalSentiment(service, track);
            SegmentIndicator segmentIndicator = new SegmentIndicator(track.track,
                    (track.segment.duration != 0) ?
                            (int) Math.round(track.recognition.words.size() / (track.segment.duration / 60.0)) : 0,
                    sentiment);
            // 内容描述情绪正负面指标
            speakerIndicator.addSegmentIndicator(segmentIndicator);
        }

        // 沉默时长
        this.silenceDuration = voiceDiarization.duration - totalDuration;
        if (this.silenceDuration < 0) {
            Logger.w(this.getClass(), "#build - silence duration is negative");
            this.silenceDuration = 0;
        }

        for (SpeakerIndicator speakerIndicator : this.speakerIndicators.values()) {
            // 时长占比
            speakerIndicator.durationRatio = (int) Math.round(speakerIndicator.totalDuration / voiceDiarization.duration);

            // 节奏
            speakerIndicator.rhythm = (int) Math.round(speakerIndicator.totalWords / (speakerIndicator.totalDuration / 60.0));

            // 情绪比例
            speakerIndicator.emotionRatio = this.evalEmotionRatio(speakerIndicator);

            float positive = 0;
            float negative = 0;
            float neutral = 0;
            for (SegmentIndicator segmentIndicator : speakerIndicator.segmentIndicators) {
                if (Sentiment.Positive == segmentIndicator.sentiment) {
                    ++positive;
                }
                else if (Sentiment.Negative == segmentIndicator.sentiment) {
                    ++negative;
                }
                else if (Sentiment.Neutral == segmentIndicator.sentiment) {
                    ++neutral;
                }
            }
            speakerIndicator.positiveSegmentRatio = Math.round((positive / speakerIndicator.segmentIndicators.size()) * 100);
            speakerIndicator.negativeSegmentRatio = Math.round((negative / speakerIndicator.segmentIndicators.size()) * 100);
            speakerIndicator.neutralSegmentRatio = Math.round((neutral / speakerIndicator.segmentIndicators.size()) * 100);
        }
    }

    private EmotionRatio evalEmotionRatio(SpeakerIndicator speakerIndicator) {
        double score = 0;
        int positiveCounts = 0;
        int negativeCounts = 0;
        int neutralCounts = 0;
        for (Emotion emotion : Emotion.commonEmotions()) {
            AtomicInteger counts = speakerIndicator.emotionCounts.get(emotion);
            double weight = sEmotionWeight.get(emotion);
            // 计算加权得分
            score += counts.get() * weight;
            // 按照权重分类正负性
            if (weight > 0) {
                positiveCounts += counts.get();
            }
            else if (weight < 0) {
                negativeCounts += counts.get();
            }
            else {
                neutralCounts += counts.get();
            }
        }
        float total = positiveCounts + negativeCounts + neutralCounts;
        int positive = Math.round(positiveCounts / total) * 100;
        int negative = Math.round(negativeCounts / total) * 100;
        return new EmotionRatio(score, positive, negative, 100 - positive - negative);
    }

    private Sentiment evalSentiment(AIGCService service, VoiceTrack track) {
        GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                String.format(sPromptSentiment, track.recognition.text),
                new GeneratingOption(), null, null);
        if (null == result) {
            Logger.w(this.getClass(), "#calcSentiment - process text failed");
            return Sentiment.Undefined;
        }

        if (result.answer.contains("中立")) {
            return Sentiment.Neutral;
        }
        else if (result.answer.contains("均有")) {
            return Sentiment.Both;
        }
        else if (result.answer.contains("积极")) {
            return Sentiment.Positive;
        }
        else if (result.answer.contains("消极")) {
            return Sentiment.Negative;
        }
        else {
            return Sentiment.Undefined;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("silenceDuration", this.silenceDuration);

        JSONObject speakerIndicatorMap = new JSONObject();
        for (Map.Entry<String, SpeakerIndicator> e : this.speakerIndicators.entrySet()) {
            speakerIndicatorMap.put(e.getKey(), e.getValue().toJSON());
        }
        json.put("speakerIndicators", speakerIndicatorMap);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

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
        public List<SegmentIndicator> segmentIndicators;

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
                this.segmentIndicators.add(new SegmentIndicator(segmentIndicatorArray.getJSONObject(i)));
            }

            this.positiveSegmentRatio = json.getInt("positiveSegmentRatio");
            this.negativeSegmentRatio = json.getInt("negativeSegmentRatio");
            this.neutralSegmentRatio = json.getInt("neutralSegmentRatio");
        }

        public void addSegmentIndicator(SegmentIndicator segmentIndicator) {
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
            for (SegmentIndicator indicator : this.segmentIndicators) {
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

    public class EmotionRatio implements JSONable {

        public final double score;

        public final int positiveRatio;

        public final int negativeRatio;

        public final int neutralRatio;

        public EmotionRatio(double score, int positiveRatio, int negativeRatio, int neutralRatio) {
            this.score = score;
            this.positiveRatio = positiveRatio;
            this.negativeRatio = negativeRatio;
            this.neutralRatio = neutralRatio;
        }

        public EmotionRatio(JSONObject json) {
            this.score = json.getDouble("score");
            this.positiveRatio = json.getInt("positiveRatio");
            this.negativeRatio = json.getInt("negativeRatio");
            this.neutralRatio = json.getInt("neutralRatio");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("score", this.score);
            json.put("positiveRatio", this.positiveRatio);
            json.put("negativeRatio", this.negativeRatio);
            json.put("neutralRatio", this.neutralRatio);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }

    public class SegmentIndicator implements JSONable {

        public final String track;

        public final int rhythm;

        public final Sentiment sentiment;

        public SegmentIndicator(String track, int rhythm, Sentiment sentiment) {
            this.track = track;
            this.rhythm = rhythm;
            this.sentiment = sentiment;
        }

        public SegmentIndicator(JSONObject json) {
            this.track = json.getString("track");
            this.rhythm = json.getInt("rhythm");
            this.sentiment = Sentiment.parse(json.getString("sentiment"));
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("track", this.track);
            json.put("rhythm", this.rhythm);
            json.put("sentiment", this.sentiment.code);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
