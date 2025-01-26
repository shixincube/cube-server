/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.event.GroupDataEvent;
import cube.hub.signal.GetGroupSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 获取指定的群组信息。
 */
public class GroupHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/group/";

    private final long coolingTime = 10;

    public GroupHandler(Performer performer, Controller controller) {
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

        String groupName = request.getParameter("name");
        if (null == groupName || groupName.length() < 1) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        // 群组名解码
        try {
            groupName = URLDecoder.decode(groupName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.w(this.getClass(), "#decode failed: " + groupName, e);
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#decode failed: " + groupName, e);
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        GetGroupSignal requestSignal = new GetGroupSignal(channelCode.code, groupName);
        Event event = this.syncTransmit(response, requestSignal);
        if (null == event) {
            this.complete();
            return;
        }

        if (event instanceof GroupDataEvent) {
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
