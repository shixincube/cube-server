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
import cube.common.entity.ConversationType;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.dispatcher.hub.HubCellet;
import cube.hub.EventBuilder;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.signal.RollPollingSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 轮询句柄。
 * 参数：type 会话类型
 * 参数：name 会话名称
 * 参数：num 指定查询数量
 */
public class RollPollingHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/polling/";

    private final long coolingTime = 200;

    public RollPollingHandler(Performer performer, Controller controller) {
        super(performer, controller);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String code = this.getRequestPath(request);

        if (!this.controller.verify(code, CONTEXT_PATH, this.coolingTime)) {
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
            return;
        }

        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String numParam = request.getParameter("num");
        if (null == type || null == name) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.w(this.getClass(), "#doGet", e);
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        ConversationType conversationType = null;
        try {
            conversationType = ConversationType.parse(Integer.parseInt(type));
        } catch (Exception e) {
            Logger.w(this.getClass(), "#doGet", e);
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        int num = 5;
        if (null != numParam) {
            try {
                num = Integer.parseInt(numParam);
            } catch (Exception e) {
                // Nothing
            }
        }

        RollPollingSignal signal = new RollPollingSignal(code, conversationType, name);
        signal.setLimit(num);

        ActionDialect actionDialect = new ActionDialect(HubAction.Channel.name);
        actionDialect.addParam("signal", signal.toJSON());

        ActionDialect result = this.performer.syncTransmit(HubCellet.NAME, actionDialect, 1 * 60 * 1000);
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

        if (!result.containsParam("event")) {
            JSONObject data = new JSONObject();
            data.put("code", HubStateCode.Failure.code);
            this.respond(response, HttpStatus.NOT_FOUND_404, data);
            this.complete();
            return;
        }

        Event event = EventBuilder.build(result.getParamAsJson("event"));
        this.respondOk(response, event.toCompactJSON());
        this.complete();
    }
}
