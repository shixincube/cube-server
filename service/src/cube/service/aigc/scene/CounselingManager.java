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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CounselingManager {

    private final static String CATEGORY = "conversation";

    private AIGCService service;

    private ExecutorService executor;

    private Map<String, List<VoiceStreamSink>> streamSinkMap;

    private Map<String, List<VoiceDiarization>> combinedVoiceMap;

    private Map<String, Wrapper> counselingStrategyMap;

    private final static CounselingManager instance = new CounselingManager();

    private CounselingManager() {
        this.streamSinkMap = new ConcurrentHashMap<>();
        this.combinedVoiceMap = new ConcurrentHashMap<>();
        this.counselingStrategyMap = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public static CounselingManager getInstance() {
        return CounselingManager.instance;
    }

    public void setService(AIGCService service) {
        this.service = service;
    }

    public void record(AuthToken authToken, VoiceStreamSink streamSink) {
        streamSink.authToken = authToken;

        List<VoiceStreamSink> list = this.streamSinkMap.computeIfAbsent(streamSink.getStreamName(), k -> new ArrayList<>());
        final List<VoiceStreamSink> listCopy = new ArrayList<>();
        synchronized (list) {
            list.add(streamSink);
            list.sort(new Comparator<VoiceStreamSink>() {
                @Override
                public int compare(VoiceStreamSink s1, VoiceStreamSink s2) {
                    return s1.getIndex() - s2.getIndex();
                }
            });
            Logger.d(this.getClass(), "#record : " + list.size());
            if (list.size() >= 5 && this.isContinuous(list)) {
                // 大约30秒，且数据连续
                listCopy.addAll(list);
                list.clear();
            }
            else if (list.size() > 8) {
                // 数据量大，即便不连续也处理
                Logger.w(this.getClass(), "#record - list overflow: " + list.size());
                listCopy.addAll(list);
                list.clear();
            }
        }

        if (!listCopy.isEmpty()) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    combine(listCopy);
                }
            });
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
//        StringBuilder buf = new StringBuilder();
//        buf.append("第一行：").append(Utils.randomString(32)).append("\n\n");
//        buf.append("第二行：").append(Utils.randomString(64)).append("\n\n");
//        buf.append("第三行：").append(Utils.randomString(32)).append("\n\n");
//        CounselingStrategy r = new CounselingStrategy(0, attribute, theme, streamName, buf.toString());
//        if (buf.length() > 32) {
//            return r;
//        }

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

    private void combine(List<VoiceStreamSink> sinks) {
        final AuthToken authToken = sinks.get(0).authToken;
        final String streamName = sinks.get(0).getStreamName();
        final int beginIndex = sinks.get(0).getIndex();
        final int endIndex = sinks.get(sinks.size() - 1).getIndex();

        List<File> fileList = new ArrayList<>();

        FlexibleByteBuffer buffer = new FlexibleByteBuffer();
        FlexibleByteBuffer fileBuf = new FlexibleByteBuffer();

        for (VoiceStreamSink sink : sinks) {
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
        for (VoiceStreamSink sink : sinks) {
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

        if (null == fileLabel) {
            Logger.e(this.getClass(), "#combine - Save file failed, stream: " + streamName);
            return;
        }

        Logger.d(this.getClass(), "#combine - num: " + sinks.size() + " , stream: " + streamName + " " +
                beginIndex + "-" + endIndex + " , file code: " + fileLabel.getFileCode());

        // 任务以插队方式提高优先级
        boolean success = this.service.performSpeakerDiarization(authToken, fileLabel, false, true,
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

                // 生成策略
                Wrapper wrapper = counselingStrategyMap.get(streamName);
                if (null != wrapper) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                List<VoiceDiarization> diarizations = new ArrayList<>();
                                double total = 0;
                                synchronized (voiceDiarizationList) {
                                    for (int i = voiceDiarizationList.size() - 1; i >= 0; --i) {
                                        VoiceDiarization vd = voiceDiarizationList.get(i);
                                        diarizations.add(vd);
                                        total += vd.duration;
                                        if (total >= 25) {
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
                            } catch (Exception e) {
                                Logger.e(this.getClass(), "#combine", e);
                            }
                        }
                    });
                }
                else {
                    Logger.e(this.getClass(), "#combine - No wrapper data: " + streamName);
                }

                // 更新备注
                service.getStorage().updateVoiceDiarizationRemark(diarization);
            }

            @Override
            public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                Logger.e(this.getClass(), "#combine - onFailed - state: " + stateCode.code);
            }
        });

        if (!success) {
            Logger.e(this.getClass(), "#combine - #performSpeakerDiarization ERROR: " + streamName);
        }
    }

    /**
     * 制订策略。
     *
     * @param wrapper
     * @param diarizations
     */
    private void formulateStrategy(Wrapper wrapper, List<VoiceDiarization> diarizations) {
        Logger.d(this.getClass(), "#formulateStrategy - Formulates strategy : " + wrapper.streamName);

        StringBuilder conversation = new StringBuilder();

        String lastLabel = "";
        for (VoiceDiarization voiceDiarization : diarizations) {
            for (VoiceTrack track : voiceDiarization.tracks) {
                String text = track.recognition.text;
                String mark = "";
                if (TextUtils.isLastPunctuationMark(text)) {
                    text = track.recognition.text.substring(0, track.recognition.text.length() - 1);
                    mark = track.recognition.text.substring(track.recognition.text.length() - 1);
                }

                if (text.length() == 0) {
                    continue;
                }

                if (!lastLabel.equalsIgnoreCase(track.label)) {
                    // 标签变更
                    conversation.append("\n\n");
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
                }

                // 内容
                conversation.append(text);
                // 语气情绪
                conversation.append("（语气").append(TextUtils.gColonInChinese);
                conversation.append(track.emotion.emotion.primaryWord);
                conversation.append("）");
                conversation.append(mark);

                lastLabel = track.label;

                if (conversation.length() > 300) {
                    // 控制对话总字数
                    break;
                }
            }

            if (conversation.length() > 300) {
                // 控制对话总字数
                break;
            }
        }

        if (conversation.toString().startsWith("\n\n")) {
            conversation.delete(0, 2);
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_STRATEGY"),
                conversation.toString(), wrapper.attribute.getGenderText(), wrapper.attribute.getAgeText(),
                wrapper.theme.nameCN);

        System.out.println("XJW prompt:\n" + prompt);

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

    private boolean isContinuous(List<VoiceStreamSink> sinkList) {
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
