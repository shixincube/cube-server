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

package cube.dispatcher.fileprocessor.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Base64;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileProcessorAction;
import cube.dispatcher.Performer;
import cube.dispatcher.fileprocessor.FileProcessorCellet;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 隐写数据。
 */
public class SteganographicHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/file/steganographic/";

    private Performer performer;

    public SteganographicHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        // 进行隐写编码

        JSONObject data = null;

        try {
            FlexibleByteBuffer buf = new FlexibleByteBuffer(256);

            InputStream is = request.getInputStream();
            int length = 0;
            byte[] bytes = new byte[256];
            while ((length = is.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }

            buf.flip();
            data = new JSONObject(new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.w(this.getClass(), "#doPost", e);
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        if (null == data || !data.has("content")) {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        String base64Content = Base64.encodeBytes(data.getString("content").getBytes(StandardCharsets.UTF_8));
        data.put("content", base64Content);

        Packet packet = new Packet(FileProcessorAction.Steganographic.name, data);
        ActionDialect resultDialect = this.performer.syncTransmit(FileProcessorCellet.NAME, packet.toDialect());
        if (null == resultDialect) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(resultDialect);
        this.respondOk(response, responsePacket.data);
        this.complete();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        // 进行隐写解码

        String fileCode = request.getParameter("fc");
        if (null == fileCode || fileCode.length() < 8) {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        String mask = request.getParameter("mask");
        if (null == mask || mask.length() < 8) {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        JSONObject packetPayload = new JSONObject();
        packetPayload.put("fileCode", fileCode);
        packetPayload.put("maskCode", mask);

        Packet packet = new Packet(FileProcessorAction.Steganographic.name, packetPayload);
        ActionDialect resultDialect = this.performer.syncTransmit(FileProcessorCellet.NAME, packet.toDialect());
        if (null == resultDialect) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(resultDialect);
        this.respondOk(response, responsePacket.data);
        this.complete();
    }
}
