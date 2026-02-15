/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.FileLabel;
import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 停止流数据处理。
 */
public class StopStream extends ContextHandler {

    public StopStream() {
        super("/aigc/stream/stop/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

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
                String streamName = data.has("streamName") ?
                        data.getString("streamName") : data.getString("name");

                FileLabel fileLabel = Manager.getInstance().stopStream(token, streamName);

                if (null != fileLabel) {
                    JSONObject responseData = fileLabel.toCompactJSON();
                    FileLabels.reviseFileLabel(responseData, token,
                            Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                            Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                    this.respondOk(response, responseData);
                    this.complete();
                }
                else {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                this.complete();
            }
        }
    }
}
