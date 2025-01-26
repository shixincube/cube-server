/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.app.server.notice.Notice;
import cube.app.server.notice.NoticeManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 通知处理。
 */
public class NoticeHandler extends ContextHandler {

    public NoticeHandler(String httpOrigin, String httpsOrigin) {
        super("/notice/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {
        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data || !data.containsKey("token") || !data.containsKey("domain")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            String token = data.get("token");
            if (!AccountManager.getInstance().isValidToken(token)) {
                // 无效令牌
                this.respond(response, HttpStatus.FORBIDDEN_403);
                return;
            }

            String domain = data.get("domain");
            List<Notice> noticeList = NoticeManager.getInstance().getNotices(domain);
            JSONArray array = new JSONArray();
            for (Notice notice : noticeList) {
                array.put(notice.toJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("domain", domain);
            responseData.put("notices", array);

            this.respondOk(response, responseData);
        }
    }
}
