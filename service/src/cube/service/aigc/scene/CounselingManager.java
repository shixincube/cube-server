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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CounselingManager {

    private final static String CATEGORY = "conversation";

    private AIGCService service;

    private ExecutorService executor;

    private Map<String, List<VoiceStreamSink>> streamSinkMap;

    private Map<String, List<VoiceDiarization>> combinedVoiceMap;

    private Map<String, Wrapper> counselingStrategyMap;

    private final static CounselingManager instance = new CounselingManager();

    private CounselingManager() {
        this.executor = Executors.newCachedThreadPool();
        this.streamSinkMap = new ConcurrentHashMap<>();
        this.combinedVoiceMap = new ConcurrentHashMap<>();
        this.counselingStrategyMap = new ConcurrentHashMap<>();
    }

    public static CounselingManager getInstance() {
        return CounselingManager.instance;
    }

    public void start(AIGCService service) {
        this.service = service;
    }

    public void stop() {
        this.executor.shutdown();

        Iterator<Map.Entry<String, List<VoiceStreamSink>>> iter = this.streamSinkMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<VoiceStreamSink>> entry = iter.next();
            for (VoiceStreamSink streamSink : entry.getValue()) {
                this.service.deleteFile(streamSink.authToken.getDomain(), streamSink.getFileLabel().getFileCode());
            }
        }

        Iterator<Map.Entry<String, List<VoiceDiarization>>> diarizationIter = this.combinedVoiceMap.entrySet().iterator();
        while (diarizationIter.hasNext()) {
            Map.Entry<String, List<VoiceDiarization>> entry = diarizationIter.next();
            String streamName = entry.getKey();
            Wrapper wrapper = this.counselingStrategyMap.get(streamName);
            if (null != wrapper) {
                for (VoiceDiarization diarization : entry.getValue()) {
                    this.service.deleteFile(wrapper.authToken.getDomain(), diarization.fileCode);
                }
            }
        }
    }

    public void onTick(long now) {
        Iterator<Map.Entry<String, Wrapper>> iter = this.counselingStrategyMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Wrapper> entry = iter.next();
            Wrapper wrapper = entry.getValue();
            if (now - wrapper.refreshTimestamp > 4 * 60 * 60 * 1000) {
                // 删除超时的数据
                String streamName = entry.getKey();

                // 删除文件
                List<VoiceStreamSink> sinkList = this.streamSinkMap.get(streamName);
                if (null != sinkList) {
                    for (VoiceStreamSink sink : sinkList) {
                        this.service.deleteFile(sink.authToken.getDomain(), sink.getFileLabel().getFileCode());
                    }
                }

                List<VoiceDiarization> diarizationList = this.combinedVoiceMap.get(streamName);
                if (null != diarizationList) {
                    for (VoiceDiarization diarization : diarizationList) {
                        this.service.deleteFile(wrapper.authToken.getDomain(), diarization.fileCode);
                    }
                }

                // 删除内存里的数据
                this.streamSinkMap.remove(streamName);
                this.combinedVoiceMap.remove(streamName);

                iter.remove();
            }
        }
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
                    // 生成策略
                    formulateStrategyWithText(listCopy);
                    formulateStrategyWithEmotion(listCopy);

                    // 生成 Caption
                    formulateCaption(listCopy);

                    // 组合数据再次进行分析并归档
                    combine(listCopy, true);
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
     * @param index
     * @return
     */
    public CounselingStrategy queryCounselingStrategy(AuthToken authToken, ConsultationTheme theme,
                                                      Attribute attribute, String streamName, int index) {
        Wrapper wrapper = this.counselingStrategyMap.computeIfAbsent(streamName,
                k -> new Wrapper(streamName, authToken, theme, attribute));
        // 更新时间戳
        wrapper.refreshTimestamp = System.currentTimeMillis();
        List<CounselingStrategy> strategies = wrapper.strategies;

        if (strategies.isEmpty()) {
            if (wrapper.generatingStrategy.get()) {
                Logger.d(this.getClass(), "#queryCounselingStrategy - Generating: " + streamName);
                return null;
            }

            wrapper.generatingStrategy.set(true);

            // 无数据策略支持
            String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_OPENING"),
                    attribute.language.isChinese() ? theme.nameCN : theme.nameEN,
                    attribute.getGenderText(), attribute.age);

            GeneratingRecord record = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);

            wrapper.generatingStrategy.set(false);

            if (null == record) {
                Logger.w(this.getClass(), "#queryCounselingStrategy - The response is null");
                return null;
            }

            CounselingStrategy strategy = new CounselingStrategy(strategies.size(), attribute, theme, streamName,
                    CounselingStrategy.ConsultingAction.General, record.answer);
            synchronized (strategies) {
                strategies.add(strategy);
            }
            return strategy;
        }
        else {
            synchronized (strategies) {
                for (CounselingStrategy strategy : strategies) {
                    if (strategy.index == index) {
                        return strategy;
                    }
                }
            }

            return strategies.get(strategies.size() - 1);
        }
    }

    /**
     * 查询咨询字幕。
     *
     * @param authToken
     * @param theme
     * @param attribute
     * @param consultingAction
     * @param streamName
     * @param index
     * @return
     */
    public CounselingStrategy queryCounselingCaption(AuthToken authToken, ConsultationTheme theme, Attribute attribute,
                                                     CounselingStrategy.ConsultingAction consultingAction,
                                                     String streamName, int index) {
        Wrapper wrapper = this.counselingStrategyMap.computeIfAbsent(streamName,
                k -> new Wrapper(streamName, authToken, theme, attribute));
        // 设置策略动作
        wrapper.consultingAction = consultingAction;

        final List<CounselingStrategy> captions = wrapper.captions;
        if (captions.isEmpty()) {
            if (wrapper.generatingCaption.get()) {
                Logger.d(this.getClass(), "#queryCounselingCaption - Generating: " + streamName);
                return null;
            }

            wrapper.generatingCaption.set(true);

            // 无数据策略支持
            String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_OPENING_CAPTION"),
                    attribute.language.isChinese() ? theme.nameCN : theme.nameEN,
                    attribute.getGenderText(), attribute.age);

            GeneratingRecord record = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);

            // 设置生成状态
            wrapper.generatingCaption.set(false);

            if (null == record) {
                Logger.w(this.getClass(), "#queryCounselingCaption - The response is null");
                return null;
            }

            List<String> contents = TextUtils.extractMarkdownTextAsList(record.answer);
            Logger.d(this.getClass(), "#queryCounselingCaption - Content lines: " + contents.size());
            synchronized (captions) {
                for (String content : contents) {
                    CounselingStrategy caption = new CounselingStrategy(captions.size(), attribute, theme,
                            streamName, CounselingStrategy.ConsultingAction.General, content);
                    captions.add(caption);
                }
            }

            return captions.get(0);
        }
        else {
            synchronized (captions) {
                for (CounselingStrategy caption : captions) {
                    if (caption.index == index) {
                        return caption;
                    }
                }
            }

            if (index >= 0 && index < captions.size()) {
                return captions.get(index);
            }
            else {
                return captions.get(captions.size() - 1);
            }
        }
    }

    private void formulateStrategyWithText(List<VoiceStreamSink> sinks) {
        final String streamName = sinks.get(0).getStreamName();
        Wrapper wrapper = this.counselingStrategyMap.get(streamName);
        if (null == wrapper) {
            Logger.w(this.getClass(), "#formulateStrategyWithText - No find wrapper: " + streamName);
            return;
        }

        StringBuilder conversation = new StringBuilder();

        String lastLabel = "";
        for (VoiceStreamSink sink : sinks) {
            for (VoiceTrack track : sink.getDiarization().tracks) {
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
                        lastLabel = track.label;
                    }
                    else if (track.label.equalsIgnoreCase(VoiceDiarization.LABEL_CUSTOMER)) {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = track.label;
                    }
                    else {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = VoiceDiarization.LABEL_CUSTOMER;
                    }
                }

                // 内容
                conversation.append(text);
                conversation.append(mark);

                if (conversation.length() > 500) {
                    // 控制对话总字数
                    break;
                }
            }

            if (conversation.length() > 500) {
                // 控制对话总字数
                break;
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_STRATEGY_TEXT"),
                conversation.toString(), wrapper.attribute.getGenderText(), wrapper.attribute.getAgeText(),
                wrapper.theme.nameCN, wrapper.theme.nameCN);

        GeneratingRecord record = this.service.syncGenerateText(wrapper.authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#formulateStrategyWithText - The record is null");
            return;
        }

        List<CounselingStrategy> strategies = wrapper.strategies;
        synchronized (strategies) {
            CounselingStrategy strategy = new CounselingStrategy(strategies.size(), wrapper.attribute, wrapper.theme,
                    wrapper.streamName, CounselingStrategy.ConsultingAction.Suggestion, record.answer);
            strategies.add(strategy);
        }
    }

    private void formulateStrategyWithEmotion(List<VoiceStreamSink> sinks) {
        final String streamName = sinks.get(0).getStreamName();
        Wrapper wrapper = this.counselingStrategyMap.get(streamName);
        if (null == wrapper) {
            Logger.w(this.getClass(), "#formulateStrategyWithEmotion - No find wrapper: " + streamName);
            return;
        }

        float totalRhythm = 0;
        float countRhythm = 0;

        float totalPositiveRatio = 0;
        float countPositiveRatio = 0;

        float totalNegativeRatio = 0;
        float countNegativeRatio = 0;

        for (VoiceStreamSink sink : sinks) {
            for (SpeakerIndicator indicator : sink.getDiarization().indicator.speakerIndicators.values()) {
                if (indicator.label.equalsIgnoreCase(VoiceDiarization.LABEL_CUSTOMER)) {
                    // 语言节奏
                    totalRhythm += indicator.rhythm;
                    ++countRhythm;

                    // 正面情绪
                    totalPositiveRatio += indicator.emotionRatio.positiveRatio;
                    ++countPositiveRatio;

                    // 负面情绪
                    totalNegativeRatio += indicator.emotionRatio.negativeRatio;
                    ++countNegativeRatio;
                    break;
                }
            }
        }

        int rhythm = Math.round(totalRhythm / countRhythm);
        int positiveRatio = Math.round(totalPositiveRatio / countPositiveRatio);
        int negativeRatio = Math.round(totalNegativeRatio / countNegativeRatio);
        int neutralRatio = 100 - positiveRatio - negativeRatio;

        String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, "FORMAT_COUNSELING_STRATEGY_EMOTION"),
                wrapper.attribute.getGenderText(), wrapper.attribute.getAgeText(), wrapper.theme.nameCN,
                rhythm, positiveRatio, negativeRatio, neutralRatio);

        GeneratingRecord record = this.service.syncGenerateText(wrapper.authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#formulateStrategyWithEmotion - The record is null");
            return;
        }

        List<CounselingStrategy> strategies = wrapper.strategies;
        synchronized (strategies) {
            CounselingStrategy strategy = new CounselingStrategy(strategies.size(), wrapper.attribute, wrapper.theme,
                    wrapper.streamName, CounselingStrategy.ConsultingAction.Analysis, record.answer);
            strategies.add(strategy);
        }
    }

    private void formulateCaption(List<VoiceStreamSink> sinks) {
        final String streamName = sinks.get(0).getStreamName();
        Wrapper wrapper = this.counselingStrategyMap.get(streamName);
        if (null == wrapper) {
            Logger.w(this.getClass(), "#formulateCaption - No find wrapper: " + streamName);
            return;
        }

        if (wrapper.generatingCaption.get()) {
            Logger.d(this.getClass(), "#formulateCaption - Generating caption : " + wrapper.streamName);
            return;
        }

        wrapper.generatingCaption.set(true);

        Logger.d(this.getClass(), "#formulateCaption - Formulates caption : " + wrapper.streamName);

        StringBuilder conversation = new StringBuilder();

        String lastLabel = "";
        for (VoiceStreamSink sink : sinks) {
            for (VoiceTrack track : sink.getDiarization().tracks) {
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
                        lastLabel = track.label;
                    }
                    else if (track.label.equalsIgnoreCase(VoiceDiarization.LABEL_CUSTOMER)) {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = track.label;
                    }
                    else {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = VoiceDiarization.LABEL_CUSTOMER;
                    }
                }

                // 内容
                conversation.append(text);
                // 语气情绪
                conversation.append("（语气").append(TextUtils.gColonInChinese);
                conversation.append(track.emotion.emotion.primaryWord);
                conversation.append("）");
                conversation.append(mark);

                if (conversation.length() > 500) {
                    // 控制对话总字数
                    break;
                }
            }

            if (conversation.length() > 500) {
                // 控制对话总字数
                break;
            }
        }

        if (conversation.toString().startsWith("\n\n")) {
            conversation.delete(0, 2);
        }

        String promptTitle = "FORMAT_COUNSELING_STRATEGY_CAPTION_ANALYSIS";
        switch (wrapper.consultingAction) {
            case Analysis:
                promptTitle = "FORMAT_COUNSELING_STRATEGY_CAPTION_ANALYSIS";
                break;
            case Suggestion:
                promptTitle = "FORMAT_COUNSELING_STRATEGY_CAPTION_SUGGESTION";
                break;
            case Conversation:
                promptTitle = "FORMAT_COUNSELING_STRATEGY_CAPTION_CONVERSATION";
                break;
            default:
                break;
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CATEGORY, promptTitle),
                conversation.toString(), wrapper.attribute.getGenderText(), wrapper.attribute.getAgeText(),
                wrapper.theme.nameCN);

        GeneratingRecord record = this.service.syncGenerateText(wrapper.authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);

        // 设置生成状态
        wrapper.generatingCaption.set(false);

        if (null == record) {
            Logger.w(this.getClass(), "#formulateCaption - The record is null");
            return;
        }

        List<String> contentList = TextUtils.extractMarkdownTextAsList(record.answer);
        Logger.d(this.getClass(), "#formulateCaption - Content lines: " + contentList.size());
        List<CounselingStrategy> captions = wrapper.captions;
        synchronized (captions) {
            for (String content : contentList) {
                CounselingStrategy caption = new CounselingStrategy(captions.size(), wrapper.attribute,
                        wrapper.theme, wrapper.streamName, wrapper.consultingAction, content);
                captions.add(caption);
            }
        }

        Logger.d(this.getClass(), "#formulateCaption - New caption : " + wrapper.streamName + "/" + captions.size());
    }

    private void combine(List<VoiceStreamSink> sinks, boolean archiving) {
        final AuthToken authToken = sinks.get(0).authToken;
        final String streamName = sinks.get(0).getStreamName();
        final int beginIndex = sinks.get(0).getIndex();
        final int endIndex = sinks.get(sinks.size() - 1).getIndex();

        final Wrapper wrapper = this.counselingStrategyMap.get(streamName);
        if (null == wrapper) {
            Logger.w(this.getClass(), "#combine - No stream: " + streamName);
            return;
        }

        if (wrapper.generatingStrategy.get()) {
            Logger.d(this.getClass(), "#combine - Generating: " + streamName);
            return;
        }

        wrapper.generatingStrategy.set(true);

        List<File> fileList = new ArrayList<>();

        FlexibleByteBuffer buffer = new FlexibleByteBuffer();
        FlexibleByteBuffer fileBuf = new FlexibleByteBuffer();

        final VoiceStreamArchive archive = new VoiceStreamArchive(this.service.workingPath.getAbsolutePath(),
                streamName, AudioUtils.SAMPLE_RATE, AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);

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

            // 保存
            if (archiving) {
                archive.save(sink, pcmData);
            }

            buffer.put(pcmData);
        }

        buffer.flip();

        if (archiving) {
            // 归档
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    archive.archive();
                }
            });
        }

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

        if (!outputFile.exists()) {
            Logger.w(this.getClass(), "#combine - Creates file failed: " + outputFile.getName());
            wrapper.generatingStrategy.set(false);
            return;
        }

        // 将文件保存到存储器
        String tmpFileCode = FileUtils.makeFileCode(authToken.getContactId(),
                authToken.getDomain(), outputFile.getName());
        FileLabel fileLabel = this.service.saveFile(authToken, tmpFileCode, outputFile,
                outputFile.getName(), true);

        if (null == fileLabel) {
            Logger.e(this.getClass(), "#combine - Save file failed, stream: " + streamName);
            wrapper.generatingStrategy.set(false);
            return;
        }

        Logger.d(this.getClass(), "#combine - num: " + sinks.size() + " , stream: " + streamName + " " +
                beginIndex + "-" + endIndex + " , file code: " + fileLabel.getFileCode());

        // 任务以插队方式提高优先级
        boolean success = this.service.performSpeakerDiarization(authToken, fileLabel, false,
                false, true, new VoiceDiarizationListener() {
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
                        } finally {
                            wrapper.generatingStrategy.set(false);
                        }
                    }
                });
            }

            @Override
            public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                Logger.e(this.getClass(), "#combine - onFailed - state: " + stateCode.code);
                wrapper.generatingStrategy.set(false);
            }
        });

        if (!success) {
            Logger.e(this.getClass(), "#combine - #performSpeakerDiarization ERROR: " + streamName);
            wrapper.generatingStrategy.set(false);
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
                        lastLabel = track.label;
                    }
                    else if (track.label.equalsIgnoreCase(VoiceDiarization.LABEL_CUSTOMER)) {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = track.label;
                    }
                    else {
                        conversation.append("来访者").append(TextUtils.gColonInChinese);
                        lastLabel = VoiceDiarization.LABEL_CUSTOMER;
                    }
                }

                // 内容
                conversation.append(text);
                // 语气情绪
                conversation.append("（语气").append(TextUtils.gColonInChinese);
                conversation.append(track.emotion.emotion.primaryWord);
                conversation.append("）");
                conversation.append(mark);

                if (conversation.length() > 500) {
                    // 控制对话总字数
                    break;
                }
            }

            if (conversation.length() > 500) {
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

        GeneratingRecord record = this.service.syncGenerateText(wrapper.authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#formulateStrategy - The record is null");
            return;
        }

        List<CounselingStrategy> strategies = wrapper.strategies;
        synchronized (strategies) {
            CounselingStrategy strategy = new CounselingStrategy(strategies.size(), wrapper.attribute, wrapper.theme,
                    wrapper.streamName, CounselingStrategy.ConsultingAction.General, record.answer);
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

        public final List<CounselingStrategy> captions;

        public CounselingStrategy.ConsultingAction consultingAction;

        public long refreshTimestamp;

        protected AtomicBoolean generatingStrategy = new AtomicBoolean(false);

        protected AtomicBoolean generatingCaption = new AtomicBoolean(false);

        protected Wrapper(String streamName, AuthToken authToken, ConsultationTheme theme, Attribute attribute) {
            this.streamName = streamName;
            this.authToken = authToken;
            this.theme = theme;
            this.attribute = attribute;
            this.strategies = new ArrayList<>();
            this.captions = new ArrayList<>();
            this.consultingAction = CounselingStrategy.ConsultingAction.General;
            this.refreshTimestamp = System.currentTimeMillis();
        }
    }
}
