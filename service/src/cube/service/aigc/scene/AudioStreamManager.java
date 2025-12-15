/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.VoiceDiarizationListener;
import cube.util.AudioUtils;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioStreamManager {

    private AIGCService service;

    private Map<String, List<AudioStreamSink>> streamSinkMap;

    private Map<String, List<VoiceDiarization>> combinedVoiceMap;

    private final static AudioStreamManager instance = new AudioStreamManager();

    private AudioStreamManager() {
        this.streamSinkMap = new ConcurrentHashMap<>();
        this.combinedVoiceMap = new ConcurrentHashMap<>();
    }

    public static AudioStreamManager getInstance() {
        return AudioStreamManager.instance;
    }

    public void setService(AIGCService service) {
        this.service = service;
    }

    public void record(AuthToken authToken, AudioStreamSink streamSink) {
        streamSink.authToken = authToken;

        List<AudioStreamSink> list = this.streamSinkMap.computeIfAbsent(streamSink.getStreamName(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(streamSink);
            Logger.d(this.getClass(), "#record : " + list.size());
            if (list.size() >= 5) {
                List<AudioStreamSink> copy = new ArrayList<>(list);
                list.clear();
                combine(copy);
            }
        }
    }

    public List<ConversationTile> getConversations(String streamName) {
        List<AudioStreamSink> list = this.streamSinkMap.get(streamName);
        synchronized (list) {
            for (AudioStreamSink sink : list) {
                for (SpeakerIndicator indicator : sink.getDiarization().indicator.speakerIndicators.values()) {

                }
            }
        }

        return null;
    }

    private void combine(List<AudioStreamSink> sinks) {
        sinks.sort(new Comparator<AudioStreamSink>() {
            @Override
            public int compare(AudioStreamSink s1, AudioStreamSink s2) {
                return s1.getIndex() - s2.getIndex();
            }
        });

        final AuthToken authToken = sinks.get(0).authToken;
        final String streamName = sinks.get(0).getStreamName();
        final int beginIndex = sinks.get(0).getIndex();
        final int endIndex = sinks.get(sinks.size() - 1).getIndex();

        List<File> fileList = new ArrayList<>();

        FlexibleByteBuffer buffer = new FlexibleByteBuffer();
        FlexibleByteBuffer fileBuf = new FlexibleByteBuffer();

        for (AudioStreamSink sink : sinks) {
            File file = this.service.loadFile(authToken.getDomain(), sink.getFileLabel().getFileCode());
            if (null == file) {
                Logger.w(this.getClass(), "#combine - The file is NOT exists : " + sink.getFileLabel().getFileCode());
                continue;
            }

            fileList.add(file);

            FileInputStream fis = null;
            fileBuf.clear();

            try {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[8 * 1024];
                int bytesRead = 0;
                while ((bytesRead = fis.read(bytes)) > 0) {
                    fileBuf.put(bytes, 0, bytesRead);
                }
                fileBuf.flip();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#combine", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        // Nothing
                    }
                }
            }

            // WAVE 转 PCM
            byte[] pcmData = AudioUtils.wavToPcm(fileBuf.array(), fileBuf.limit());
            buffer.put(pcmData);
        }

        buffer.flip();

        // PCM 转 WAVE
        byte[] wavData = AudioUtils.pcmToWav(buffer.array(), 0, buffer.limit(),
                AudioUtils.SAMPLE_RATE, AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);

        File outputFile = new File(this.service.getWorkingPath(),
                streamName + "-" + beginIndex + "_" + endIndex + ".wav");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            fos.write(wavData);
            fos.flush();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#combine", e);
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (Exception e) {
                    // Nothing
                }
            }
        }

        // 删除文件
        for (AudioStreamSink sink : sinks) {
            this.service.deleteFile(authToken.getDomain(), sink.getFileLabel().getFileCode());
        }
        for (File file : fileList) {
            if (file.exists()) {
                file.delete();
            }
        }

        // 将文件保存到存储器
        String tmpFileCode = FileUtils.makeFileCode(authToken.getContactId(),
                authToken.getDomain(), outputFile.getName());
        FileLabel fileLabel = this.service.saveFile(authToken, tmpFileCode, outputFile,
                outputFile.getName(), true);

        this.service.performSpeakerDiarization(authToken, fileLabel.getFileCode(), false,
                new VoiceDiarizationListener() {
            @Override
            public void onCompleted(FileLabel source, VoiceDiarization diarization) {
                List<VoiceDiarization> list = combinedVoiceMap.computeIfAbsent(streamName, k -> new ArrayList<>());
                diarization.remark = beginIndex + "-" + endIndex;

                // 更新备注
                service.getStorage().updateVoiceDiarizationRemark(diarization);
            }

            @Override
            public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                Logger.e(this.getClass(), "#onFailed - state: " + stateCode.code);
            }
        });
    }
}
