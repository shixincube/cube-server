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

import cube.dispatcher.Performer;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.event.GroupDataEvent;
import cube.hub.signal.GetGroupSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 获取指定的群组信息。
 */
public class GroupHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/group/";

    public GroupHandler(Performer performer) {
        super(performer);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String code = this.getRequestPath(request);
        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        String groupName = request.getParameter("name");
        if (null == groupName || groupName.length() < 1) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        // 群组名解码
        try {
            groupName = URLDecoder.decode(groupName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        GetGroupSignal requestSignal = new GetGroupSignal(channelCode.code, groupName);
        Event event = this.syncTransmit(request, response, requestSignal);
        if (null == event) {
            this.complete();
            return;
        }

        if (event instanceof GroupDataEvent) {
            this.respondOk(response, event.toCompactJSON());
        }
        else {
            JSONObject data = new JSONObject();
            data.put("code", HubStateCode.ControllerError.code);
            this.respond(response, HttpStatus.NOT_FOUND_404, data);
        }

        this.complete();
    }
}