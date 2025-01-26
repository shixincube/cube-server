/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cube.common.entity.ContactZoneParticipantType;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.hub.data.ChannelCode;
import cube.hub.event.ContactZoneEvent;
import cube.hub.event.Event;
import cube.hub.signal.GetContactZoneSignal;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 获取通讯录。
 */
public class ContactBookHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/book/";

    private final long coolingTime = 10;

    public ContactBookHandler(Performer performer, Controller controller) {
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

        int begin = 0;
        int end = 9;

        String beginString = request.getParameter("begin");
        String endString = request.getParameter("end");

        if (null != beginString && beginString.length() > 0) {
            try {
                begin = Integer.parseInt(beginString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (null != endString && endString.length() > 0) {
            try {
                end = Integer.parseInt(endString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (begin >= end) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        if (end - begin + 1 > 20) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        GetContactZoneSignal signal = new GetContactZoneSignal(code,
                ContactZoneParticipantType.Contact, begin, end);
        Event event = this.syncTransmit(response, signal);
        if (null == event) {
            this.complete();
            return;
        }

        if (event instanceof ContactZoneEvent) {
            this.respondOk(response, event.toCompactJSON());
            this.complete();
        }
        else {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
        }
    }
}
