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

import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.event.MessagesEvent;
import cube.hub.signal.GetMessagesSignal;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 消息数据句柄。
 * 查询参数：cid
 * 查询参数：gn
 * 查询参数：begin
 * 查询参数：end
 */
public class MessagesHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/messages/";

    public MessagesHandler(Performer performer) {
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

        String contactId = request.getParameter("cid");
        String groupName = request.getParameter("gn");
        if (null == contactId && null == groupName) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        if (null != groupName) {
            try {
                groupName = URLDecoder.decode(groupName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Logger.w(this.getClass(), "", e);
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }
        }

        int begin = 0;
        int end = 9;
        String beginParam = request.getParameter("begin");
        String endParam = request.getParameter("end");
        if (null != beginParam) {
            try {
                begin = Integer.parseInt(beginParam);
            } catch (Exception e) {
                // Nothing
            }
        }
        if (null != endParam) {
            try {
                end = Integer.parseInt(endParam);
            } catch (Exception e) {
                // Nothing
            }
        }

        if (end <= begin) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        if (end - begin + 1 > 20) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        GetMessagesSignal signal = new GetMessagesSignal(code, begin, end);
        if (null != contactId) {
            signal.setPartnerId(contactId);
        }
        else {
            signal.setGroupName(groupName);
        }

        Event event = this.syncTransmit(request, response, signal);
        if (null == event) {
            this.complete();
            return;
        }

        if (!(event instanceof MessagesEvent)) {
            this.complete();
            return;
        }

        MessagesEvent messagesEvent = (MessagesEvent) event;
        this.respondOk(response, messagesEvent.toCompactJSON());
        this.complete();
    }
}
