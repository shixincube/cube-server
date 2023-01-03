/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.common.entity.ConversationType;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
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
public class SendMessageHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/message/";

    private final long coolingTime = 500;

    public SendMessageHandler(Performer performer, Controller controller) {
        super(performer, controller);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
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
        try {
            JSONObject data = new JSONObject(jsonString);

            SendMessageSignal signal = null;
            if (data.has("partnerId")) {
                signal = new SendMessageSignal(channelCode.code,
                        ConversationType.Contact, data.getString("partnerId"));
            }
            else if (data.has("groupName")) {
                signal = new SendMessageSignal(channelCode.code,
                        ConversationType.Group, data.getString("groupName"));
            }
            else {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            if (data.has("text")) {
                String content = data.getString("text");
                // 进行 Base64 解码
                byte[] bytes = Base64.decode(content);
                // 还原的原文本
                content = new String(bytes, StandardCharsets.UTF_8);

                // 设置文本内容
                signal.setText(content);
            }
            else if (data.has("fileCode")) {
                String fileCode = data.getString("fileCode");
                // 设置文件码
                signal.setFileCode(fileCode);
            }
            else {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            Event event = this.syncTransmit(response, signal);
            if (null != event) {
                this.respondOk(response, event.toJSON());
                this.complete();
            }
            else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#doPost - " + request.getRemoteAddr(), e);
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
        }
    }
}
