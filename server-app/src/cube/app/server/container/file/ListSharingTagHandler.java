/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container.file;

import cell.util.log.Logger;
import cube.app.server.Manager;
import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.util.DefenseStrategy;
import cube.client.Client;
import cube.common.entity.SharingTag;
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
 * 批量获取分享标签。
 */
public class ListSharingTagHandler extends ContextHandler {

    private DefenseStrategy strategy;

    public ListSharingTagHandler(String httpOrigin, String httpsOrigin) {
        super("/file/list/sharing/");
        // 冷却时间 500ms
        this.strategy = new DefenseStrategy(500);
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
            if (null == data || !data.containsKey("token")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            String tokenCode = data.get("token");

            if (!strategy.verify(tokenCode)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            int begin = Integer.parseInt(data.get("begin"));
            int end = Integer.parseInt(data.get("end"));
            boolean valid = !data.containsKey("valid") || data.get("valid").equals("1") || data.get("valid").equals("true");

            // 参数校验
            if (end - begin > 9) {
                Logger.d(this.getClass(), "#doGet - Parameter index range out-of-limit (not recommend): " + begin + "-" + end);
            }
            else if (end - begin > 29) {
                this.respond(response, HttpStatus.LENGTH_REQUIRED_411);
                this.complete();
                return;
            }

            Account account = AccountManager.getInstance().getOnlineAccount(tokenCode);
            if (null == account) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            JSONObject responseData = new JSONObject();
            responseData.put("begin", begin);
            responseData.put("end", end);
            responseData.put("valid", valid);

            Client client = Manager.getInstance().getClient();

            // 总数量
            responseData.put("total", client.getFileStorage().getCachedSharingTagTotal(account.id, valid));

            JSONArray array = new JSONArray();

            List<SharingTag> list = client.getFileStorage().listSharingTags(account.id, account.domain, begin, end, valid);
            if (null != list) {
                for (SharingTag tag : list) {
                    array.put(tag.toCompactJSON());
                }
            }

            responseData.put("list", array);

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
