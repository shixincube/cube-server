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
import cube.common.entity.*;
import cube.service.aigc.AIGCService;
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

    public VoiceIndicator(JSONObject json) {
    }

    public void analyse(AIGCService service, VoiceDiarization voiceDiarization) {
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

            Sentiment sentiment = this.calcSentiment(service, track);
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
            speakerIndicator.emotionRatio = calcEmotionRatio(speakerIndicator);
        }
    }

    private EmotionRatio calcEmotionRatio(SpeakerIndicator speakerIndicator) {
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

    private Sentiment calcSentiment(AIGCService service, VoiceTrack track) {
        GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                String.format(sPromptSentiment, track.recognition.text),
                new GeneratingOption(), null, null);
        if (null == result) {
            Logger.w(this.getClass(), "#calcSentiment - process text failed");
            return Sentiment.Undefined;
        }

        if (result.answer.equalsIgnoreCase("中立")) {

        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class EmotionRatio {

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
    }

    public class SpeakerIndicator {

        public String speaker;

        public String label;

        public double totalDuration;

        public int totalWords;

        public Map<Emotion, AtomicInteger> emotionCounts;

        public int durationRatio;

        /**
         * 说话节奏，w/m，每分钟词数。
         */
        public int rhythm;

        public EmotionRatio emotionRatio;

        public List<SegmentIndicator> segmentIndicators;

        public SpeakerIndicator(String speaker, String label) {
            this.speaker = speaker;
            this.label = label;
            this.emotionCounts = new HashMap<>();
            for (Emotion emotion : Emotion.commonEmotions()) {
                this.emotionCounts.put(emotion, new AtomicInteger(0));
            }
            this.segmentIndicators = new ArrayList<>();
        }

        public void addSegmentIndicator(SegmentIndicator segmentIndicator) {

        }
    }

    public class SegmentIndicator {

        public int rhythm;

        public Sentiment sentiment;

        public SegmentIndicator() {

        }
    }
}
