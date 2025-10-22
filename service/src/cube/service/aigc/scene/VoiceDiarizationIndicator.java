/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.Sentiment;
import cube.common.entity.Emotion;
import cube.common.entity.*;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class VoiceDiarizationIndicator extends VoiceIndicator {

    public VoiceDiarizationIndicator(long id) {
        super(id);
    }

    public VoiceDiarizationIndicator(JSONObject json) {
        super(json);
    }

    public void analyse(AIGCService service, VoiceDiarization voiceDiarization) {
        if (voiceDiarization.duration == 0) {
            return;
        }

        double totalDuration = 0;

        for (VoiceTrack track : voiceDiarization.tracks) {
            if (track.segment.duration < 1.0) {
                // 跳过时长较短的分段
                continue;
            }

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
            SpeakerSegmentIndicator segmentIndicator = new SpeakerSegmentIndicator(track.track,
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
            for (SpeakerSegmentIndicator segmentIndicator : speakerIndicator.segmentIndicators) {
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
            double weight = VoiceIndicator.sEmotionWeight.get(emotion);
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
}
