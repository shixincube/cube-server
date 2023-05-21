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
import cube.aigc.ModelConfig;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileProcessorAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.FileProcessResult;
import cube.file.operation.AudioCropOperation;
import cube.file.operation.ExtractAudioOperation;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.service.aigc.command.Command;
import cube.service.aigc.command.CommandListener;
import cube.service.aigc.listener.*;
import cube.service.aigc.plugin.InjectTokenPlugin;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private Map<String, Queue<ConversationUnitMeta>> conversationQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<NaturalLanguageTaskMeta>> nlTaskQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<SentimentUnitMeta>> sentimentQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ASRUnitMeta>> asrQueueMap;

    /**
     * 最大频道数量。
     */
    private int maxChannel = 50;

    /**
     * 聊天内容最大长度限制。
     */
    private int maxChatContent = 500;

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    private long channelTimeout = 30 * 60 * 1000;

    private ExecutorService executor;

    private AIGCStorage storage;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
        this.chatQueueMap = new ConcurrentHashMap<>();
        this.conversationQueueMap = new ConcurrentHashMap<>();
        this.nlTaskQueueMap = new ConcurrentHashMap<>();
        this.sentimentQueueMap = new ConcurrentHashMap<>();
        this.asrQueueMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        this.executor = Executors.newCachedThreadPool();

        (new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject config = ConfigUtils.readStorageConfig();
                if (config.has(AIGCService.NAME)) {
                    config = config.getJSONObject(AIGCService.NAME);
                    if (config.getString("type").equalsIgnoreCase("SQLite")) {
                        storage = new AIGCStorage(StorageType.SQLite, config);
                    }
                    else {
                        storage = new AIGCStorage(StorageType.MySQL, config);
                    }

                    storage.open();
                    storage.execSelfChecking(null);
                }
                else {
                    Logger.e(AIGCService.class, "Can NOT find AIGC storage config");
                }

                // 监听事件
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                while (!authService.isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                authService.getPluginSystem().register(AuthServiceHook.InjectToken,
                        new InjectTokenPlugin(AIGCService.this));

                started.set(true);
            }
        })).start();
    }

    @Override
    public void stop() {
        if (null != this.executor) {
            this.executor.shutdown();
            this.executor = null;
        }

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        this.started.set(false);
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
                this.conversationQueueMap.remove(unit.getQueryKey());
            }
        }

        return result;
    }

    public List<AIGCUnit> getAllUnits() {
        List<AIGCUnit> list = new ArrayList<>(this.unitMap.size());
        list.addAll(this.unitMap.values());
        return list;
    }

    public List<AIGCChannel> getAllChannels() {
        List<AIGCChannel> list = new ArrayList(this.channelMap.size());
        list.addAll(this.channelMap.values());
        return list;
    }

    public List<ModelConfig> getModelConfigs() {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.getModelConfigs();
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

    /**
     * 通过邀请码查询令牌。
     *
     * @param invitationCode
     * @return
     */
    public String queryTokenByInvitation(String invitationCode) {
        return this.storage.readTokenByInvitation(invitationCode);
    }

    /**
     * 为令牌创建新的邀请码。
     *
     * @param token
     * @return
     */
    public String newInvitationForToken(String token) {
        String invitation = Utils.randomNumberString(6);
        this.storage.writeInvitation(invitation, token);
        return invitation;
    }

    /**
     * 校验令牌。
     *
     * @param tokenCode
     * @return
     */
    public AuthToken checkToken(String tokenCode) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        return authToken;
    }

    /**
     * 申请频道。
     *
     * @param participant
     * @return
     */
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
        return this.chat(code, content, null, null, listener);
    }

    /**
     * 执行聊天任务。
     *
     * @param code
     * @param content
     * @param desc
     * @param records
     * @param listener
     * @return
     */
    public boolean chat(String code, String content, String desc, List<AIGCChatRecord> records, ChatListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        if (content.length() > this.maxChatContent) {
            Logger.i(AIGCService.class, "Content length greater than " + this.maxChatContent);
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
        AIGCUnit unit = (null != desc) ?
                this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.ImprovedConversational, desc)
                : this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
        if (null == unit) {
            Logger.w(AIGCService.class, "No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        ChatUnitMeta meta = new ChatUnitMeta(unit, channel, content, records, listener);

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

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processChatQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 增强型互动会话。
     *
     * @param code
     * @param content
     * @param parameter
     * @param listener
     * @return
     */
    public boolean conversation(String code, String content, AIGCConversationParameter parameter, ConversationListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        if (content.length() > this.maxChatContent) {
            Logger.i(AIGCService.class, "Content length greater than " + this.maxChatContent);
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
        AIGCUnit unit = this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.ImprovedConversational,
                "MOSS");
        if (null == unit) {
            Logger.w(AIGCService.class, "No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        ConversationUnitMeta meta = new ConversationUnitMeta(unit, channel, content, parameter, listener);

        synchronized (this.conversationQueueMap) {
            Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.conversationQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processConversationQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 执行自然语言任务。
     *
     * @param task
     * @param listener
     * @return
     */
    public boolean performNaturalLanguageTask(NLTask task, NaturalLanguageTaskListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 检查任务
        if (!task.check()) {
            Logger.w(AIGCService.class, "Natural language task data error: " + task.type);
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.MultiTask);
        if (null == unit) {
            Logger.w(AIGCService.class, "No natural language task unit setup in server");
            return false;
        }

        NaturalLanguageTaskMeta meta = new NaturalLanguageTaskMeta(unit, task, listener);

        synchronized (this.nlTaskQueueMap) {
            Queue<NaturalLanguageTaskMeta> queue = this.nlTaskQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.nlTaskQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processNaturalLanguageTaskQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    public boolean sentimentAnalysis(String text, SentimentAnalysisListener listener) {
        if (!this.isStarted()) {
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

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processSentimentQueue(meta.unit.getQueryKey());
                }
            });
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

        FileLabel sourceFile = new FileLabel(fileLabelJson);

        FileLabel fileLabel = new FileLabel(fileLabelJson);
        boolean deleteFileLabel = false;

        AbstractModule fileProcessor = this.getKernel().getModule("FileProcessor");
        if (null == fileProcessor) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor service is not ready");
            return false;
        }

        if (FileUtils.isVideoType(fileLabel.getFileType())) {
            // 从视频文件中提取音频文件
            ExtractAudioOperation audioOperation = new ExtractAudioOperation(FileType.WAV);
            JSONObject processor = new JSONObject();
            processor.put("action", FileProcessorAction.Video.name);
            processor.put("domain", fileLabel.getDomain().getName());
            processor.put("fileCode", fileLabel.getFileCode());
            processor.put("parameter", audioOperation.toJSON());

            JSONObject resultJson = fileProcessor.notify(processor);
            if (null == resultJson) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Extract audio operation result is NULL: "
                    + fileLabel.getFileCode());
                return false;
            }

            FileProcessResult result = new FileProcessResult(resultJson);
            if (!result.success) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Extract audio operation result error: "
                        + fileLabel.getFileCode());
                return false;
            }

            // 音频文件
            File audioFile = result.getResultList().get(0).file;
            String audioFileCode = FileUtils.makeFileCode(fileLabel.getOwnerId(), fileLabel.getDomain().getName(), audioFile.getName());
            FileLabel audioFileLabel = FileUtils.makeFileLabel(fileLabel.getDomain().getName(), audioFileCode, fileLabel.getOwnerId(), audioFile);
            // 将处理后文件存入存储器
            JSONObject saveFile = new JSONObject();
            saveFile.put("action", FileStorageAction.SaveFile.name);
            saveFile.put("path", audioFile.getAbsolutePath());
            saveFile.put("fileLabel", audioFileLabel.toJSON());
            JSONObject audioFileLabelJson = fileStorage.notify(saveFile);
            if (null == audioFileLabelJson) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Save audio file to storage failed");
                return false;
            }

            // 更新文件标签
            fileLabel = new FileLabel(audioFileLabelJson);
            // 在处理后删除该音频数据
            deleteFileLabel = true;
        }
        else if (!FileUtils.isAudioType(fileLabel.getFileType())) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File type is NOT audio type: "
                    + fileLabel.getFileCode());
            return false;
        }

        // 音频重采用
        /*AudioSamplingOperation samplingOperation = new AudioSamplingOperation(1, 16000, FileType.WAV);

        JSONObject processor = new JSONObject();
        processor.put("action", FileProcessorAction.Audio.name);
        processor.put("domain", fileLabel.getDomain().getName());
        processor.put("fileCode", fileLabel.getFileCode());
        processor.put("parameter", samplingOperation.toJSON());

        JSONObject resultJson = fileProcessor.notify(processor);
        if (null == resultJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor result is NULL");
            return false;
        }*/

        // 音频文件分割
        AudioCropOperation cropOperation = new AudioCropOperation(0, 30, FileType.WAV);
        JSONObject processor = new JSONObject();
        processor.put("action", FileProcessorAction.Audio.name);
        processor.put("domain", fileLabel.getDomain().getName());
        processor.put("fileCode", fileLabel.getFileCode());
        processor.put("parameter", cropOperation.toJSON());

        JSONObject resultJson = fileProcessor.notify(processor);
        if (null == resultJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Audio crop result is NULL: "
                    + fileLabel.getFileCode());
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        FileProcessResult result = new FileProcessResult(resultJson);
        if (null == result.getAudioResult()) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Audio crop result error: "
                    + fileLabel.getFileCode());
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
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
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        // 文件标签
        localFileLabel = new FileLabel(localFileLabelJson);

        // 请求 AIGC 单元
        // 查找有该能力的单元
        AIGCUnit unit = this.getUnitBySubtask(AICapability.AudioProcessing.AutomaticSpeechRecognition);
        if (null == unit) {
            Logger.w(AIGCService.class, "No ASR task unit setup in server");
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        ASRUnitMeta meta = new ASRUnitMeta(unit, sourceFile, localFileLabel, listener);

        synchronized (this.asrQueueMap) {
            Queue<ASRUnitMeta> queue = this.asrQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.asrQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processASRQueue(meta.unit.getQueryKey());
                }
            });
        }

        if (deleteFileLabel) {
            JSONObject deleteFile = new JSONObject();
            deleteFile.put("action", FileStorageAction.DeleteFile.name);
            deleteFile.put("domain", fileLabel.getDomain().getName());
            deleteFile.put("fileCode", fileLabel.getFileCode());
            fileStorage.notify(deleteFile);
        }

        return true;
    }

    /**
     * 执行命令。
     *
     * @param command
     * @param listener
     */
    public void executeCommand(final Command command, final CommandListener listener) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                command.run();


            }
        });
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
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processChatQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processConversationQueue(String queryKey) {
        Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "Not found unit: " + queryKey);
            return;
        }

        ConversationUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processConversationQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processNaturalLanguageTaskQueue(String queryKey) {
        Queue<NaturalLanguageTaskMeta> queue = this.nlTaskQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
            return;
        }

        NaturalLanguageTaskMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processNaturalLanguageTaskQueue - meta process", e);
            }

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
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processSentimentQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processASRQueue(String queryKey) {
        Queue<ASRUnitMeta> queue = this.asrQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
            return;
        }

        ASRUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processASRQueue - meta process", e);
            }

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

        protected List<AIGCChatRecord> records;

        protected int histories;

        protected ChatListener listener;

        public ChatUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, List<AIGCChatRecord> records, ChatListener listener) {
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.records = records;
            this.histories = 3;
            this.listener = listener;
        }

        public ChatUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, int histories, ChatListener listener) {
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.histories = histories;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", this.content);

            if (null == this.records) {
                if (this.histories > 0) {
                    List<AIGCChatRecord> records = this.channel.getLastHistory(this.histories);
                    JSONArray array = new JSONArray();
                    for (AIGCChatRecord record : records) {
                        array.put(record.toJSON());
                    }
                    data.put("history", array);
                }
                else {
                    data.put("history", new JSONArray());
                }
            }
            else {
                JSONArray history = new JSONArray();
                for (AIGCChatRecord record : this.records) {
                    history.put(record.toJSON());
                }
                data.put("history", history);
            }

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


    private class ConversationUnitMeta {

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected String content;

        protected AIGCConversationParameter parameter;

        protected ConversationListener listener;

        public ConversationUnitMeta(AIGCUnit unit, AIGCChannel channel, String content,
                                    AIGCConversationParameter parameter, ConversationListener listener) {
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.parameter = parameter;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", this.content);
            data.put("temperature", this.parameter.temperature);
            data.put("topP", this.parameter.topP);
            data.put("repetitionPenalty", this.parameter.repetitionPenalty);

            int totalLength = this.content.length();

            if (null != this.parameter.records) {
                JSONArray history = new JSONArray();
                for (AIGCChatRecord record : this.parameter.records) {
                    history.put(record.toJSON());
                    // 字数
                    totalLength += record.totalWords();
                }
                data.put("history", history);
            }
            else {
                List<AIGCConversationResponse> responseList = this.channel.extractConversationResponses();
                JSONArray history = new JSONArray();
                for (AIGCConversationResponse response : responseList) {
                    AIGCChatRecord record = response.toRecord();
                    history.put(record.toJSON());
                    // 字数
                    totalLength += record.totalWords();
                }
                data.put("history", history);
            }

            // 判断长度
            if (totalLength > maxChatContent + maxChatContent) {
                // 总长度越界
                this.listener.onFailed(this.channel, AIGCStateCode.ContentLengthOverflow.code);
                return;
            }

            Packet request = new Packet(AIGCAction.Conversation.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Conversation unit error - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError.code);
                return;
            }

            Packet response = new Packet(dialect);
            int stateCode = Packet.extractCode(response);
            if (stateCode != AIGCStateCode.Ok.code) {
                Logger.w(AIGCService.class, "Conversation unit respond failed - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, stateCode);
                return;
            }

            JSONObject payload = Packet.extractDataPayload(response);
            AIGCConversationResponse convResponse = new AIGCConversationResponse(payload);
            convResponse.query = this.content;

            // 更新字数
            this.unit.setTotalQueryWords(this.unit.getTotalQueryWords() + this.content.length());

            // 记录
            this.channel.appendRecord(convResponse);

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onConversation(this.channel, convResponse);
        }
    }


    private class NaturalLanguageTaskMeta {

        protected AIGCUnit unit;

        protected NLTask task;

        protected NaturalLanguageTaskListener listener;

        public NaturalLanguageTaskMeta(AIGCUnit unit, NLTask task, NaturalLanguageTaskListener listener) {
            this.unit = unit;
            this.task = task;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("task", this.task.toJSON());

            Packet request = new Packet(AIGCAction.NaturalLanguageTask.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Natural language task unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            String type = payload.getString("task");
            JSONArray resultList = payload.getJSONArray("result");

            this.listener.onCompleted(new NLTask(type, resultList));
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


    private class ASRUnitMeta {

        protected AIGCUnit unit;

        protected FileLabel source;

        protected FileLabel input;

        protected AutomaticSpeechRecognitionListener listener;

        public ASRUnitMeta(AIGCUnit unit, FileLabel source, FileLabel input, AutomaticSpeechRecognitionListener listener) {
            this.unit = unit;
            this.source = source;
            this.input = input;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("input", this.input.toJSON());

            Packet request = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "ASR unit error");
                // 回调错误
                this.listener.onFailed(this.source);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (!payload.has("list")) {
                Logger.w(AIGCService.class, "ASR unit process failed");
                // 回调错误
                this.listener.onFailed(this.source);
                return;
            }

            this.listener.onCompleted(this.input, new ASRResult(source, payload));

            if (!this.input.getFileCode().equals(this.source.getFileCode())) {
                // 输入文件和源文件不一致，删除输入文件
                AbstractModule fileStorage = getKernel().getModule("FileStorage");
                if (null != fileStorage) {
                    JSONObject deleteFile = new JSONObject();
                    deleteFile.put("action", FileStorageAction.DeleteFile.name);
                    deleteFile.put("domain", this.input.getDomain().getName());
                    deleteFile.put("fileCode", this.input.getFileCode());
                    fileStorage.notify(deleteFile);
                }
            }
        }
    }
}
