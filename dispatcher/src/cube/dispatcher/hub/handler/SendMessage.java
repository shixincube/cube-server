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

import cell.util.Base64;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.entity.Message;
import cube.dispatcher.Performer;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.signal.SendMessageSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 发送消息。
 */
public class SendMessage extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/message/";

    public SendMessage(Performer performer) {
        super(performer);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String code = this.getRequestPath(request);
        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        // 读取流
        FlexibleByteBuffer buf = new FlexibleByteBuffer();

        try {
            byte[] bytes = new byte[4096];
            InputStream is = request.getInputStream();

            int length = 0;
            while ((length = is.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }

            // 整理缓存
            buf.flip();
        } catch (IOException e) {
            Logger.e(this.getClass(), "#doPost", e);
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        String jsonString = new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8);
        Message message = null;
        try {
            JSONObject data = new JSONObject(jsonString);



            message = new Message(data);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#doPost - " + request.getRemoteAddr(), e);
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        JSONObject payload = message.getPayload();
        if (!payload.has("type")) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        String type = payload.getString("type");
        if (type.equalsIgnoreCase("text") && payload.has("content")) {
            String content = payload.getString("content");
            try {
                // 进行 Base64 解码
                byte[] bytes = Base64.decode(content);
                content = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#doPost - " + request.getRemoteAddr(), e);
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            SendMessageSignal signal = new SendMessageSignal(channelCode.code, message.getPartner(), content);
            Event event = this.syncTransmit(request, response, signal);
            if (null != event) {
                this.respondOk(response, event.toJSON());
                this.complete();
            }
            else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        }
        else {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
        }
    }
}
