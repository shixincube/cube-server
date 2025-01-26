/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.CacheCenter;
import cube.dispatcher.hub.Controller;
import cube.dispatcher.hub.HubCellet;
import cube.hub.EventBuilder;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.SignalBuilder;
import cube.hub.event.Event;
import cube.hub.signal.LoginQRCodeSignal;
import cube.hub.signal.Signal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 开通通道。
 * 参数 c - 通道码。
 */
public class OpenChannel extends HubHandler {

    public final static String CONTEXT_PATH = "/hub/open/";

    private final long coolingTime = 500;

    public OpenChannel(Performer performer, Controller controller) {
        super(performer, controller);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String code = this.getRequestPath(request);
        if (code.length() < 8) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        if (!this.controller.verify(code, CONTEXT_PATH, this.coolingTime)) {
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
            return;
        }

        // 创建信令
        LoginQRCodeSignal requestSignal = new LoginQRCodeSignal(code);
        ActionDialect actionDialect = new ActionDialect(HubAction.Channel.name);
        actionDialect.addParam("signal", requestSignal.toJSON());

        ActionDialect result = this.performer.syncTransmit(HubCellet.NAME, actionDialect, 3 * 60 * 1000);
        if (null == result) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        int stateCode = result.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.w(this.getClass(), "#doGet - state : " + stateCode);
            JSONObject data = new JSONObject();
            data.put("code", stateCode);
            this.respond(response, HttpStatus.UNAUTHORIZED_401, data);
            this.complete();
            return;
        }

        if (result.containsParam("event")) {
            Event event = EventBuilder.build(result.getParamAsJson("event"));
            if (null != event.getFileLabel()) {
                // 添加到缓存
                CacheCenter.getInstance().putFileLabel(event.getFileLabel());
                this.respondOk(response, event.toCompactJSON());
            }
            else {
                this.respond(response, HttpStatus.SERVICE_UNAVAILABLE_503, event.toCompactJSON());
            }
        }
        else if (result.containsParam("signal")) {
            Signal signal = SignalBuilder.build(result.getParamAsJson("signal"));
            this.respondOk(response, signal.toCompactJSON());
        }
        else {
            JSONObject data = new JSONObject();
            data.put("code", HubStateCode.Failure.code);
            this.respond(response, HttpStatus.NOT_FOUND_404, data);
        }

        this.complete();
    }
}
