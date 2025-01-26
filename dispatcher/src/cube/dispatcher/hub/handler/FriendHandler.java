/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.signal.AddFriendSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 添加朋友。
 */
public class FriendHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/friend/";

    private final long coolingTime = 1000;

    public FriendHandler(Performer performer, Controller controller) {
        super(performer, controller);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String path = this.getRequestPath(request);
        String[] tmp = path.split("/");
        if (tmp.length != 2) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        String action = tmp[0];
        String code = tmp[1];

        if (!this.controller.verify(code, CONTEXT_PATH, this.coolingTime)) {
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
            return;
        }

        // 校验访问码
        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        FlexibleByteBuffer buf = new FlexibleByteBuffer(256);

        try {
            InputStream is = request.getInputStream();
            int length = 0;
            byte[] bytes = new byte[128];
            while ((length = is.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "#doPost", e);
            this.respond(response, HttpStatus.INTERNAL_SERVER_ERROR_500);
            this.complete();
            return;
        }

        // 整理
        buf.flip();

        JSONObject data = null;
        try {
            data = new JSONObject(new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#doPost", e);
            this.respond(response, HttpStatus.INTERNAL_SERVER_ERROR_500);
            this.complete();
            return;
        }

        if (action.equalsIgnoreCase("add")) {
            this.doAdd(code, data, response);
        }
        else if (action.equalsIgnoreCase("remove")) {
            this.doRemove(code, data, response);
        }
        else {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
        }
    }

    private void doAdd(String channelCode, JSONObject requestData, HttpServletResponse response) {
        AddFriendSignal signal = new AddFriendSignal(channelCode, requestData.getString("keyword"));
        if (requestData.has("postscript")) {
            signal.setPostscript(requestData.getString("postscript"));
        }
        if (requestData.has("remarkName")) {
            signal.setRemarkName(requestData.getString("remarkName"));
        }

        Event event = syncTransmit(response, signal);
        if (null != event) {
            this.respondOk(response, event.toJSON());
        }

        this.complete();
    }

    private void doRemove(String channelCode, JSONObject requestData, HttpServletResponse response) {
        this.respond(response, HttpStatus.BAD_REQUEST_400);
        this.complete();
    }
}
