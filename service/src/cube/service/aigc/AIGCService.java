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
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.CapabilitySet;
import cube.common.entity.Contact;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIGC 服务。
 */
public class AIGCService extends AbstractModule {

    public final static String NAME = "AIGC";

    private final static String AI_NAME = "Cube";

    private AIGCCellet cellet;

    private List<AIGCUnit> unitList;

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    private int maxChannel = 30;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitList = new ArrayList<>();
        this.channelMap = new ConcurrentHashMap<>();
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

    }

    public AIGCUnit setupUnit(Contact contact, CapabilitySet capabilitySet, TalkContext context) {
        synchronized (this.unitList) {
            for (AIGCUnit unit : this.unitList) {
                if (unit.getContact().getId().equals(contact.getId())) {
                    unit.setCapabilitySet(capabilitySet);
                    unit.setTalkContext(context);
                    return unit;
                }
            }

            AIGCUnit unit = new AIGCUnit(contact, capabilitySet, context);
            this.unitList.add(unit);
            return unit;
        }
    }

    public void teardownUnit(Contact contact) {
        synchronized (this.unitList) {

        }
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

    public AIGCChannel.Record chat(String code, String content) {
        // 获取频道
        AIGCChannel channel = this.channelMap.get(code);
        if (null == channel) {
            Logger.i(AIGCService.class, "Can NOT find AIGC channel: " + code);
            return null;
        }

        AIGCUnit unit = null;
        synchronized (this.unitList) {
            // 获取 Unit
            if (this.unitList.isEmpty()) {
                return null;
            }
            unit = this.unitList.get(0);
        }

        JSONObject data = new JSONObject();
        data.put("content", content);
        data.put("history", channel.getLastParticipantHistory(5));

        Packet request = new Packet(AIGCAction.Chat.name, data);
        ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect());
        if (null == dialect) {
            Logger.w(AIGCService.class, "Chat unit error - channel: " + code);
            return null;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);

        String responseText = payload.getString("response");

        AIGCChannel.Record result = channel.appendHistory(AI_NAME, responseText);
        return result;
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
}
