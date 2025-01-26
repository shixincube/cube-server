/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.fileprocessor.handler;

import cube.auth.AuthToken;
import cube.dispatcher.Performer;
import cube.dispatcher.fileprocessor.MediaFileManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 获取媒体源。
 */
public class GetMediaSourceHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/file/source/";

    private Performer performer;

    public GetMediaSourceHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getParameter("t");
        if (null == token || token.length() == 0) {
            token = request.getParameter("token");
        }

        if (null == token || token.length() == 0) {
            // 参数错误
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        AuthToken authToken = this.performer.verifyToken(token);
        if (null == authToken) {
            // 令牌码无效
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            this.complete();
            return;
        }

        String domain = authToken.getDomain();
        String fileCode = request.getParameter("fc");

        String url = MediaFileManager.getInstance().getMediaSourceURL(domain, fileCode, isHttps());
        if (null == url) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            this.complete();
            return;
        }

        JSONObject data = new JSONObject();
        data.put("fileCode", fileCode);
        data.put("url", url);
        this.respondOk(response, data);

        this.complete();
    }
}
