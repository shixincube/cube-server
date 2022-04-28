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
import cube.common.entity.ConversationType;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.hub.data.ChannelCode;
import cube.hub.signal.RollPollingSignal;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 轮询句柄。
 * 参数：type
 */
public class RollPollingHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/polling/";

    private final long coolingTime = 100;

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

        //RollPollingSignal signal = new RollPollingSignal(code, )
    }
}
