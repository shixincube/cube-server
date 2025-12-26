/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.ConsultationTheme;
import cube.aigc.psychology.Attribute;
import cube.common.entity.CounselingStrategy;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 查询咨询策略。
 */
public class QueryCounselingStrategy extends ContextHandler {

    public QueryCounselingStrategy() {
        super("/aigc/stream/strategy/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                String streamName = data.getString("streamName");
                ConsultationTheme theme = ConsultationTheme.parse(data.getString("theme"));
                Attribute attribute = new Attribute(data.getJSONObject("attribute"));
                int index = data.has("index") ? data.getInt("index") : -1;

                CounselingStrategy strategy = Manager.getInstance().queryCounselingStrategy(token,
                        streamName, theme, attribute, index);
                if (null == strategy) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                }
                else {
                    this.respondOk(response, strategy.toJSON());
                    this.complete();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
