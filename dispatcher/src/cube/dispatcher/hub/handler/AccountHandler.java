/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cube.dispatcher.Performer;
import cube.dispatcher.hub.Controller;
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
public class AccountHandler extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/account/";

    private final long coolingTime = 500;

    public AccountHandler(Performer performer, Controller controller) {
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

        GetAccountSignal requestSignal = new GetAccountSignal(channelCode.code);
        Event event = this.syncTransmit(response, requestSignal);
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
