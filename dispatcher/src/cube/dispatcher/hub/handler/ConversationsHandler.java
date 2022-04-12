/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.dispatcher.hub.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.dispatcher.hub.HubCellet;
import cube.hub.EventBuilder;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.event.ConversationsEvent;
import cube.hub.event.Event;
import cube.hub.signal.GetConversationsSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 获取最近会话数据。
 * 查询参数 nc - 请求的最近会话数量。
 * 查询参数 nm - 请求的每个会话的消息数量。
 */
public class ConversationsHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/conversations/";

    public ConversationsHandler(Performer performer, Controller controller) {
        super(performer, controller);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String code = this.getRequestPath(request);

        if (!this.controller.verify(code)) {
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
            return;
        }

        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        int numOfConv = 8;
        String numOfConvString = request.getParameter("nc");
        if (null != numOfConvString && numOfConvString.length() > 0) {
            try {
                numOfConv = Integer.parseInt(numOfConvString);
            } catch (Exception e) {
                // Nothing
            }
        }

        if (numOfConv > 10) {
            numOfConv = 10;
        }

        int numOfMessage = 5;
        String numOfMessageString = request.getParameter("nm");
        if (null != numOfMessageString && numOfMessageString.length() > 0) {
            try {
                numOfMessage = Integer.parseInt(numOfMessageString);
            } catch (Exception e) {
                // Nothing
            }
        }
        if (numOfMessage > 10) {
            numOfMessage = 10;
        }

        // 创建信令
        GetConversationsSignal getConversationsSignal = new GetConversationsSignal(channelCode.code);
        getConversationsSignal.setNumConversations(numOfConv);
        getConversationsSignal.setNumRecentMessages(numOfMessage);
        ActionDialect actionDialect = new ActionDialect(HubAction.Channel.name);
        actionDialect.addParam("signal", getConversationsSignal.toJSON());

        ActionDialect result = this.performer.syncTransmit(HubCellet.NAME, actionDialect, 3 * 60 * 1000);
        if (null == result) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        int stateCode = result.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.w(this.getClass(), "#doGet - state : " + stateCode);
            JSONObject data = new JSONObject();
            data.put("code", stateCode);
            this.respond(response, HttpStatus.UNAUTHORIZED_401, data);
            this.complete();
            return;
        }

        if (result.containsParam("event")) {
            Event event = EventBuilder.build(result.getParamAsJson("event"));
            if (event instanceof ConversationsEvent) {
                this.respondOk(response, event.toCompactJSON());
            }
            else {
                JSONObject data = new JSONObject();
                data.put("code", HubStateCode.ControllerError.code);
                this.respond(response, HttpStatus.NOT_FOUND_404, data);
            }
        }
        else {
            JSONObject data = new JSONObject();
            data.put("code", HubStateCode.Failure.code);
            this.respond(response, HttpStatus.NOT_FOUND_404, data);
        }

        this.complete();
    }
}
