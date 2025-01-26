/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.Console;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Auth 操作相关接口。
 */
public class AuthHandler extends ContextHandler {

    private Console console;

    public AuthHandler(Console console) {
        super("/auth");
        this.setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends CrossDomainHandler {

        public Handler() {
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (target.equals("/domain")) {
                List<String> list = console.getStatisticDataManager().getDomains();
                JSONArray array = new JSONArray();
                for (String domain : list) {
                    array.put(domain);
                }

                JSONObject data = new JSONObject();
                data.put("tag", console.getTag());
                data.put("list", array);

                respondOk(response, data);
            }
        }
    }
}
