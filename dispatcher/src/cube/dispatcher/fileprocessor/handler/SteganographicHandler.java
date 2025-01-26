/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * 隐写数据。
 */
public class SteganographicHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/file/steganographic/";

    private Performer performer;

    private List<JSONObject> resultLog = new LinkedList<>();

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

        // 记录处理
        this.resultLog.add(responsePacket.data);
        if (this.resultLog.size() > 20) {
            this.resultLog.remove(0);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        // 进行隐写解码

        String logs = request.getParameter("logs");
        if (null != logs) {
            int size = 1;
            try {
                size = Integer.parseInt(logs);
            } catch (Exception e) {
                // Nothing
            }

            JSONArray array = new JSONArray();
            for (int i = this.resultLog.size() - 1; i >= 0; --i) {
                JSONObject data = this.resultLog.get(i);
                array.put(data);
                if (array.length() >= size) {
                    break;
                }
            }

            JSONObject data = new JSONObject();
            data.put("size", size);
            data.put("logs", array);
            this.respondOk(response, data);
            this.complete();
            return;
        }

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
