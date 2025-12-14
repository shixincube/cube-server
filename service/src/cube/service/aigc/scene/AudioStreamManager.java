/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.common.entity.AudioStreamSink;
import cube.common.entity.ConversationTile;
import cube.common.entity.SpeakerIndicator;
import cube.service.aigc.AIGCService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioStreamManager {

    private AIGCService service;

    private Map<String, List<AudioStreamSink>> streamSinkMap;

    private Map<String, List<AudioStreamSink>> combinedStreamSinkMap;

    private final static AudioStreamManager instance = new AudioStreamManager();

    private AudioStreamManager() {
        this.streamSinkMap = new ConcurrentHashMap<>();
    }

    public static AudioStreamManager getInstance() {
        return AudioStreamManager.instance;
    }

    public void setService(AIGCService service) {
        this.service = service;
    }

    public void record(AudioStreamSink streamSink) {
        List<AudioStreamSink> list = this.streamSinkMap.computeIfAbsent(streamSink.getStreamName(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(streamSink);
        }
    }

    public List<ConversationTile> getConversations(String streamName) {
        List<AudioStreamSink> list = this.streamSinkMap.get(streamName);
        synchronized (list) {
            if (list.size() >= 30) {

            }
            for (AudioStreamSink sink : list) {
                for (SpeakerIndicator indicator : sink.getDiarization().indicator.speakerIndicators.values()) {

                }
            }
        }

        return null;
    }

    private void combine() {

    }
}
