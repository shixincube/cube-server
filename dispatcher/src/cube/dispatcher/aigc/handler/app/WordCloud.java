/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 激活设备。
 */
public class WordCloud extends ContextHandler {

    public WordCloud() {
        super("/app/wordcloud/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = getApiToken(request);
                if (!Manager.getInstance().checkToken(token)) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                JSONObject responseJson = Manager.getInstance().getWordCloud(token);
                this.respondOk(response, responseJson);
                this.complete();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
