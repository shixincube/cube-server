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
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务。
 */
public class AIGCService extends AbstractModule {

    public final static String NAME = "AIGC";

    private final static String AI_NAME = "Cube";

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
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
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
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getSubtask().equals(subtask)) {
                return unit;
            }
        }

        return null;
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

    public boolean chat(String code, String content, ChatListener listener) {
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
        AIGCUnit unit = this.getUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
        if (null == unit) {
            Logger.w(AIGCService.class, "No conversational task unit setup in server");
            return false;
        }

        ChatUnitMeta meta = new ChatUnitMeta(unit, channel, content, listener);

        synchronized (this) {
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

    private void processChatQueue(String queryKey) {
        Queue<ChatUnitMeta> queue = this.chatQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
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

    private boolean checkParticipantName(String name) {
        if (name.equalsIgnoreCase("AIGC") || name.equalsIgnoreCase("Cube") ||
            name.equalsIgnoreCase("Pixiu") || name.equalsIgnoreCase("貔貅") ||
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
            data.put("content", this.content);
            data.put("history", this.channel.getLastParticipantHistory(5));

            Packet request = new Packet(AIGCAction.Chat.name, data);
            ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Chat unit error - channel: " + this.channel.getCode());
                channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            String responseText = payload.getString("response");

            AIGCChatRecord result = this.channel.appendHistory(AI_NAME, responseText);

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onChat(this.channel, result);
        }
    }
}
