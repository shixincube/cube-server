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
        Event event = this.syncTransmit(request, response, signal);
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
