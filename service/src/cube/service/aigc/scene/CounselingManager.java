/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.aigc.ConsultationTheme;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Resource;
import cube.auth.AuthToken;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.VoiceDiarizationListener;
import cube.util.AudioUtils;
import cube.util.FileUtils;
import cube.util.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CounselingManager {

    private final static String CATEGORY = "conversation";

    private AIGCService service;

    private Map<String, List<AudioStreamSink>> streamSinkMap;

    private Map<String, List<VoiceDiarization>> combinedVoiceMap;

    private Map<String, Wrapper> counselingStrategyMap;

    private final static CounselingManager instance = new CounselingManager();

    private CounselingManager() {
        this.streamSinkMap = new ConcurrentHashMap<>();
        this.combinedVoiceMap = new ConcurrentHashMap<>();
        this.counselingStrategyMap = new ConcurrentHashMap<>();
    }

    public static CounselingManager getInstance() {
        return CounselingManager.instance;
    }

    public void setService(AIGCService service) {
        this.service = service;
    }

    public void record(AuthToken authToken, AudioStreamSink streamSink) {
        streamSink.authToken = authToken;

        List<AudioStreamSink> list = this.streamSinkMap.computeIfAbsent(streamSink.getStreamName(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(streamSink);
            list.sort(new Comparator<AudioStreamSink>() {
                @Override
                public int compare(AudioStreamSink s1, AudioStreamSink s2) {
                    return s1.getIndex() - s2.getIndex();
                }
            });
            Logger.d(this.getClass(), "#record : " + list.size());
            if (list.size() >= 5 && this.isContinuous(list)) {
                // 大约30秒，且数据连续
                List<AudioStreamSink> copy = new ArrayList<>(list);
                list.clear();
                combine(copy);
            }
            else if (list.size() > 10) {
                // 数据量大，即便不连续也处理
                Logger.w(this.getClass(), "#record - list overflow: " + list.size());
                List<AudioStreamSink> copy = new ArrayList<>(list);
                list.clear();
                combine(copy);
            }
        }
    }

    /**
     * 查询咨询策略。
     *
     * @param authToken
     * @param theme
     * @param attribute
     * @param streamName
     * @return
     */
    public CounselingStrategy queryCounselingStrategy(AuthToken authToken, ConsultationTheme theme,
                                                      Attribute attribute, String streamName) {
        Wrapper wrapper = this.counselingStrategyMap.computeIfAbsent(streamName,
                k -> new Wrapper(streamName, authToken, theme, attribute));
        List<CounselingStrategy> strategies = wrapper.strategies;

        if (strategies.isEmpty()) {
            // 无数据策略支持
            String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_OPENING"),
                    attribute.language.isChinese() ? theme.nameCN : theme.nameEN,
                    attribute.getGenderText(), attribute.age);

            GeneratingRecord record = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);
            if (null == record) {
                Logger.w(this.getClass(), "#queryCounselingStrategy - The response is null");
                return null;
            }

            CounselingStrategy strategy = new CounselingStrategy(0, attribute, theme, streamName, record.answer);
            synchronized (strategies) {
                strategies.add(strategy);
            }
            return strategy;
        }
        else {
            return strategies.get(strategies.size() - 1);
        }
    }

    private void combine(List<AudioStreamSink> sinks) {
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
                diarization.remark = beginIndex + "-" + endIndex;
                final List<VoiceDiarization> voiceDiarizationList = combinedVoiceMap.computeIfAbsent(streamName, k -> new ArrayList<>());
                synchronized (voiceDiarizationList) {
                    voiceDiarizationList.add(diarization);
                    voiceDiarizationList.sort(new Comparator<VoiceDiarization>() {
                        @Override
                        public int compare(VoiceDiarization vd1, VoiceDiarization vd2) {
                            String[] index1 = vd1.remark.split("-");
                            String[] index2 = vd2.remark.split("-");
                            return Integer.parseInt(index1[0]) - Integer.parseInt(index2[0]);
                        }
                    });
                }

                // 更新备注
                service.getStorage().updateVoiceDiarizationRemark(diarization);

                // 生成策略
                Wrapper wrapper = counselingStrategyMap.get(streamName);
                if (null != wrapper) {
                    service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            List<VoiceDiarization> diarizations = new ArrayList<>();
                            double total = 0;
                            synchronized (voiceDiarizationList) {
                                for (int i = voiceDiarizationList.size() - 1; i >= 0; --i) {
                                    VoiceDiarization vd = voiceDiarizationList.get(i);
                                    diarizations.add(vd);
                                    total += vd.duration;
                                    if (total >= 5 * 60) {
                                        break;
                                    }
                                }
                            }
                            // 排序
                            diarizations.sort(new Comparator<VoiceDiarization>() {
                                @Override
                                public int compare(VoiceDiarization vd1, VoiceDiarization vd2) {
                                    String[] index1 = vd1.remark.split("-");
                                    String[] index2 = vd2.remark.split("-");
                                    return Integer.parseInt(index1[0]) - Integer.parseInt(index2[0]);
                                }
                            });
                            // 制作策略
                            formulateStrategy(wrapper, diarizations);
                        }
                    });
                }
            }

            @Override
            public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                Logger.e(this.getClass(), "#onFailed - state: " + stateCode.code);
            }
        });
    }

    /**
     * 制订策略。
     *
     * @param wrapper
     * @param diarizations
     */
    private void formulateStrategy(Wrapper wrapper, List<VoiceDiarization> diarizations) {
        StringBuilder conversation = new StringBuilder();

        for (VoiceDiarization voiceDiarization : diarizations) {
            for (VoiceTrack track : voiceDiarization.tracks) {
                // 角色
                if (track.label.equalsIgnoreCase(VoiceDiarization.LABEL_COUNSELOR)) {
                    conversation.append("咨询师").append(TextUtils.gColonInChinese);
                }
                else if (track.label.equalsIgnoreCase(VoiceDiarization.LABEL_CUSTOMER)) {
                    conversation.append("来访者").append(TextUtils.gColonInChinese);
                }
                else {
                    conversation.append("来访者").append(TextUtils.gColonInChinese);
                }

                // 内容
                conversation.append(track.recognition.text);
                // 语气情绪
                conversation.append("（语气情绪").append(TextUtils.gColonInChinese);
                conversation.append(track.emotion.emotion.primaryWord);
                conversation.append("）\n\n");
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_STRATEGY"),
                conversation.toString(), wrapper.attribute.getGenderText(), wrapper.attribute.getAgeText(),
                wrapper.theme.nameCN);

        GeneratingRecord record = this.service.syncGenerateText(wrapper.authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#formulateStrategy - The record is null");
            return;
        }

        List<CounselingStrategy> strategies = wrapper.strategies;
        synchronized (strategies) {
            CounselingStrategy strategy = new CounselingStrategy(strategies.size(), wrapper.attribute,
                    wrapper.theme, wrapper.streamName, record.answer);
            strategies.add(strategy);
        }

        Logger.d(this.getClass(), "#formulateStrategy - New strategy : " + wrapper.streamName + "/" + strategies.size());
    }

    private boolean isContinuous(List<AudioStreamSink> sinkList) {
        int begin = sinkList.get(0).getIndex();
        int next = begin + 1;
        for (int i = 1; i < sinkList.size(); ++i) {
            int index = sinkList.get(i).getIndex();
            if (index != next) {
                // 索引不连续
                return false;
            }
            ++next;
        }
        return true;
    }

    protected class Wrapper {

        public final String streamName;

        public final AuthToken authToken;

        public final ConsultationTheme theme;

        public final Attribute attribute;

        public final List<CounselingStrategy> strategies;

        protected Wrapper(String streamName, AuthToken authToken, ConsultationTheme theme, Attribute attribute) {
            this.streamName = streamName;
            this.authToken = authToken;
            this.theme = theme;
            this.attribute = attribute;
            this.strategies = new ArrayList<>();
        }
    }
}
