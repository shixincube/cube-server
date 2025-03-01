/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
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

    private final long coolingTime = 10;

    public MessagesHandler(Performer performer, Controller controller) {
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

        Event event = this.syncTransmit(response, signal);
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
