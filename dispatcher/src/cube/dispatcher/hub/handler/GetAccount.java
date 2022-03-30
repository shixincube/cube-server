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
import cube.hub.event.AccountEvent;
import cube.hub.event.Event;
import cube.hub.signal.GetAccountSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 获取账号信息。
 */
public class GetAccount extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/account";

    public GetAccount(Performer performer) {
        super(performer);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        ChannelCode channelCode = Helper.checkChannelCode(request, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        GetAccountSignal requestSignal = new GetAccountSignal(channelCode.code);
        Event event = this.syncTransmit(request, response, requestSignal);
        if (null == event) {
            this.complete();
            return;
        }

        if (event instanceof AccountEvent) {
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
