/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileProcessorAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.FileProcessResult;
import cube.file.operation.AudioSamplingOperation;
import cube.plugin.PluginSystem;
import cube.service.aigc.listener.AutomaticSpeechRecognitionListener;
import cube.service.aigc.listener.ChatListener;
import cube.service.aigc.listener.SentimentAnalysisListener;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务。
 */
public class AIGCService extends AbstractModule {

    public final static String NAME = "AIGC";

    private AIGCCellet cellet;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, AIGCUnit> unitMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ChatUnitMeta>> chatQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<SentimentUnitMeta>> sentimentQueueMap;

    /**
     * 最大频道数量。
     */
    private int maxChannel = 50;

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    private long channelTimeout = 30 * 60 * 1000;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
        this.chatQueueMap = new ConcurrentHashMap<>();
        this.sentimentQueueMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
        // 周期 60 秒
//        Logger.i(AIGCService.class, "#onTick");

        long now = System.currentTimeMillis();

        // 删除失效的 Unit
        Iterator<AIGCUnit> unitIter = this.unitMap.values().iterator();
        while (unitIter.hasNext()) {
            AIGCUnit unit = unitIter.next();
            if (!unit.getContext().isValid()) {
                // 已失效
                unitIter.remove();
            }
        }

        Iterator<AIGCChannel> iter = this.channelMap.values().iterator();
        while (iter.hasNext()) {
            AIGCChannel channel = iter.next();
            if (now - channel.getActiveTimestamp() >= this.channelTimeout) {
                iter.remove();
            }
        }
    }

    public List<AIGCUnit> setupUnit(Contact contact, List<AICapability> capabilities, TalkContext context) {
        List<AIGCUnit> result = new ArrayList<>(capabilities.size());

        for (AICapability capability : capabilities) {
            String key = AIGCUnit.makeQueryKey(contact, capability);
            AIGCUnit unit = this.unitMap.get(key);
            if (null != unit) {
                unit.setTalkContext(context);
            }
            else {
                unit = new AIGCUnit(contact, capability, context);
                this.unitMap.put(key, unit);
            }
            result.add(unit);
        }

        return result;
    }

    public List<AIGCUnit> teardownUnit(Contact contact) {
        List<AIGCUnit> result = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getContact().getId().equals(contact.getId())) {
                result.add(unit);
                iter.remove();
                this.chatQueueMap.remove(unit.getQueryKey());
            }
        }

        return result;
    }

    public AIGCUnit getUnitBySubtask(String subtask) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getSubtask().equals(subtask)) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            return candidates.get(0);
        }

        // 先进行一次随机选择
        AIGCUnit unit = candidates.get(Utils.randomInt(0, num - 1));

        iter = candidates.iterator();
        while (iter.hasNext()) {
            AIGCUnit u = iter.next();
            if (u.isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        if (candidates.isEmpty()) {
            // 所有单元都在运行，返回随机选择
            return unit;
        }

        unit = candidates.get(Utils.randomInt(0, candidates.size() - 1));
        return unit;
    }

    public AIGCUnit getUnitBySubtask(String subtask, String description) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getSubtask().equals(subtask) &&
                unit.getCapability().getDescription().equals(description)) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            return candidates.get(0);
        }

        // 先进行一次随机选择
        AIGCUnit unit = candidates.get(Utils.randomInt(0, num - 1));

        iter = candidates.iterator();
        while (iter.hasNext()) {
            AIGCUnit u = iter.next();
            if (u.isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        if (candidates.isEmpty()) {
            // 所有单元都在运行，返回随机选择
            return unit;
        }

        unit = candidates.get(Utils.randomInt(0, candidates.size() - 1));
        return unit;
    }

    public AIGCChannel requestChannel(String participant) {
        if (this.channelMap.size() >= this.maxChannel) {
            Logger.w(AIGCService.class, "Channel num overflow: " + this.maxChannel);
            return null;
        }

        if (!this.checkParticipantName(participant)) {
            Logger.w(AIGCService.class, "Participant is sensitive word: " + participant);
            return null;
        }

        AIGCChannel channel = new AIGCChannel(participant);
        this.channelMap.put(channel.getCode(), channel);
        return channel;
    }

    /**
     * 执行聊天任务。
     *
     * @param code
     * @param content
     * @param listener
     * @return
     */
    public boolean chat(String code, String content, ChatListener listener) {
        return this.chat(code, content, null, listener);
    }

    /**
     * 执行聊天任务。
     *
     * @param code
     * @param content
     * @param professionDesc
     * @param listener
     * @return
     */
    public boolean chat(String code, String content, String professionDesc, ChatListener listener) {
        if (!this.started) {
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(code);
        if (null == channel) {
            Logger.i(AIGCService.class, "Can NOT find AIGC channel: " + code);
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "Channel is processing: " + code);
            return false;
        }

        channel.setProcessing(true);

        // 查找有该能力的单元
        AIGCUnit unit = (null != professionDesc) ?
                this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.ProfessionConversational, professionDesc)
                : this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
        if (null == unit) {
            Logger.w(AIGCService.class, "No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        ChatUnitMeta meta = new ChatUnitMeta(unit, channel, content, listener);

        synchronized (this.chatQueueMap) {
            Queue<ChatUnitMeta> queue = this.chatQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.chatQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    processChatQueue(meta.unit.getQueryKey());
                }
            });
            thread.start();
        }

        return true;
    }

    public boolean sentimentAnalysis(String text, SentimentAnalysisListener listener) {
        if (!this.started) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.SentimentAnalysis);
        if (null == unit) {
            Logger.w(AIGCService.class, "No sentiment analysis task unit setup in server");
            return false;
        }

        SentimentUnitMeta meta = new SentimentUnitMeta(unit, text, listener);

        synchronized (this.sentimentQueueMap) {
            Queue<SentimentUnitMeta> queue = this.sentimentQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.sentimentQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    processSentimentQueue(meta.unit.getQueryKey());
                }
            });
            thread.start();
        }

        return true;
    }


    public boolean automaticSpeechRecognition(String domain, String fileCode, AutomaticSpeechRecognitionListener listener) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File storage service is not ready");
            return false;
        }

        JSONObject getFile = new JSONObject();
        getFile.put("action", FileStorageAction.GetFile.name);
        getFile.put("domain", domain);
        getFile.put("fileCode", fileCode);
        getFile.put("transmitting", false);

        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Get file failed: " + fileCode);
            return false;
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);

        AbstractModule fileProcessor = this.getKernel().getModule("FileProcessor");
        if (null == fileProcessor) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor service is not ready");
            return false;
        }

        // 音频重采用
        AudioSamplingOperation samplingOperation = new AudioSamplingOperation(1, 16000, FileType.WAV);

        JSONObject processor = new JSONObject();
        processor.put("action", FileProcessorAction.Audio.name);
        processor.put("domain", fileLabel.getDomain().getName());
        processor.put("fileCode", fileLabel.getFileCode());
        processor.put("parameter", samplingOperation.toJSON());

        JSONObject resultJson = fileProcessor.notify(processor);
        if (null == resultJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor result is NULL");
            return false;
        }

        FileProcessResult result = new FileProcessResult(resultJson);
        if (null == result.getAudioResult()) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Result error");
            return false;
        }

        File resultFile = result.getResultList().get(0).file;
        String localFileCode = FileUtils.makeFileCode(fileLabel.getOwnerId(), fileLabel.getDomain().getName(), resultFile.getName());
        FileLabel localFileLabel = FileUtils.makeFileLabel(fileLabel.getDomain().getName(), localFileCode, fileLabel.getOwnerId(), resultFile);
        // 将处理后文件存入存储器
        JSONObject saveFile = new JSONObject();
        saveFile.put("action", FileStorageAction.SaveFile.name);
        saveFile.put("path", resultFile.getAbsolutePath());
        saveFile.put("fileLabel", localFileLabel.toJSON());
        JSONObject localFileLabelJson = fileStorage.notify(saveFile);
        if (null == localFileLabelJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Save file to storage failed");
            return false;
        }

        localFileLabel = new FileLabel(localFileLabelJson);
        System.out.println("XJW:" + localFileLabel.toJSON().toString(4));

        return false;
    }

    private void processChatQueue(String queryKey) {
        Queue<ChatUnitMeta> queue = this.chatQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "Not found unit: " + queryKey);
            return;
        }

        ChatUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            meta.process();

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processSentimentQueue(String queryKey) {
        Queue<SentimentUnitMeta> queue = this.sentimentQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
            return;
        }

        SentimentUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            meta.process();

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private boolean checkParticipantName(String name) {
        if (name.equalsIgnoreCase("AIGC") || name.equalsIgnoreCase("Cube") ||
            name.equalsIgnoreCase("Baize") || name.equalsIgnoreCase("白泽") ||
            name.equalsIgnoreCase("时信魔方")) {
            return false;
        }
        else {
            return true;
        }
    }


    private class ChatUnitMeta {

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected String content;

        protected ChatListener listener;

        public ChatUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, ChatListener listener) {
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", this.content);

            List<AIGCChatRecord> records = this.channel.getLastHistory();
            JSONArray array = new JSONArray();
            for (AIGCChatRecord record : records) {
                array.put(record.toJSON());
            }
            data.put("history", array);

            Packet request = new Packet(AIGCAction.Chat.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Chat unit error - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            if (!payload.has("response")) {
                Logger.w(AIGCService.class, "Chat unit respond failed - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel);
                return;
            }

            String responseText = payload.getString("response");
            AIGCChatRecord result = this.channel.appendRecord(this.content, responseText);

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onChat(this.channel, result);
        }
    }


    private class SentimentUnitMeta {

        protected AIGCUnit unit;

        protected String text;

        protected SentimentAnalysisListener listener;

        public SentimentUnitMeta(AIGCUnit unit, String text, SentimentAnalysisListener listener) {
            this.unit = unit;
            this.text = text;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("text", this.text);

            Packet request = new Packet(AIGCAction.Sentiment.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Sentiment unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            SentimentResult result = new SentimentResult(payload);

            this.listener.onCompleted(result);
        }
    }
}
