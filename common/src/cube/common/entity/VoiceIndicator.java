/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.log.Logger;
import cube.aigc.psychology.composition.Emotion;
import cube.common.JSONable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceIndicator implements JSONable {

    public double silenceDuration;

    public Map<String, SpeakerIndicator> speakerIndicators = new HashMap<>();

    public VoiceIndicator(List<VoiceTrack> tracks, double duration) {
        this.build(tracks, duration);
    }

    private void build(List<VoiceTrack> tracks, double duration) {
        double totalDuration = 0;

        for (VoiceTrack track : tracks) {
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
        }

        // 沉默时长
        this.silenceDuration = duration - totalDuration;
        if (this.silenceDuration < 0) {
            Logger.w(this.getClass(), "#build - silence duration is negative");
            this.silenceDuration = 0;
        }

        for (SpeakerIndicator speakerIndicator : this.speakerIndicators.values()) {
            // 时长占比
            speakerIndicator.durationRatio = (int) Math.round(speakerIndicator.totalDuration / duration);

            // 节奏
            speakerIndicator.rhythm = (int) Math.round(speakerIndicator.totalWords / (speakerIndicator.totalDuration / 60.0));


        }
    }

    private EmotionRatio calcEmotionRatio(SpeakerIndicator speakerIndicator) {
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

        int positive;

        int negative;

        int neutral;

        public EmotionRatio() {

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

        public SpeakerIndicator(String speaker, String label) {
            this.speaker = speaker;
            this.label = label;
            this.emotionCounts = new HashMap<>();
            for (Emotion emotion : Emotion.commonEmotions()) {
                this.emotionCounts.put(emotion, new AtomicInteger(0));
            }
        }
    }

    public class SegmentIndicator {

        public int rhythm;

        public SegmentIndicator() {

        }
    }
}
