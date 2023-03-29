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

package cube.dispatcher.aigc;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCChatRecord;
import cube.common.state.AIGCStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.aigc.handler.Chat;
import cube.dispatcher.aigc.handler.RequestChannel;
import cube.util.HttpServer;
import org.json.JSONObject;

/**
 * 接口管理器。
 */
public class Manager {

    private final static Manager instance = new Manager();

    private Performer performer;

    private String token;

    public static Manager getInstance() {
        return Manager.instance;
    }

    public void start(Performer performer, String token) {
        this.performer = performer;
        this.token = token;

        this.setupHandler();
    }

    public void stop() {
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        httpServer.addContextHandler(new RequestChannel());
        httpServer.addContextHandler(new Chat());
    }

    public String getToken() {
        return this.token;
    }

    public AIGCChannel requestChannel(String participant) {
        JSONObject data = new JSONObject();
        data.put("participant", participant);
        Packet packet = new Packet(AIGCAction.RequestChannel.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#requestChannel Response is null - " + participant);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#requestChannel Response state code is NOT Ok - " + participant +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        AIGCChannel channel = new AIGCChannel(Packet.extractDataPayload(responsePacket));
        return channel;
    }


    public AIGCChatRecord chat(String channelCode, String content) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);
        data.put("content", content);
        Packet packet = new Packet(AIGCAction.Chat.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#chat Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#chat Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        AIGCChatRecord record = new AIGCChatRecord(Packet.extractDataPayload(responsePacket));
        return record;
    }
}
