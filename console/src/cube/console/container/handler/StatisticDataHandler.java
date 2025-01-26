/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.Console;
import cube.console.Utils;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Auth 操作相关接口。
 */
public class StatisticDataHandler extends ContextHandler {

    private Console console;

    public StatisticDataHandler(Console console) {
        super("/statistic");
        this.setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends CrossDomainHandler {

        public Handler() {
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (target.equals("/recent")) {
                String query = URLDecoder.decode(request.getQueryString(), "UTF-8");
                Map<String, String> params = Utils.parseQueryStringParams(query);

                String domain = params.get("domain");

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -1);

                JSONObject statistic = console.getStatisticDataManager().queryStatisticData(domain, calendar);
                if (null == statistic) {
                    respond(response, HttpStatus.NOT_FOUND_404);
                    return;
                }

                // 查找前一天数据，计算新增用户数
                JSONObject prevStatistic = console.getStatisticDataManager().queryStatisticData(domain,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DATE) - 1);
                // 计算 DNU
                if (null != prevStatistic) {
                    statistic.put("DNU", statistic.getInt("TNU") - prevStatistic.getInt("TNU"));
                }

                JSONObject data = new JSONObject();
                data.put("tag", console.getTag());
                data.put("statistic", statistic);
                data.put("year", calendar.get(Calendar.YEAR));
                data.put("month", calendar.get(Calendar.MONTH) + 1);
                data.put("date", calendar.get(Calendar.DATE));

                respondOk(response, data);
            }
            else if (target.equals("/daily")) {
                String query = URLDecoder.decode(request.getQueryString(), "UTF-8");
                Map<String, String> params = Utils.parseQueryStringParams(query);

                String domain = params.get("domain");

                Calendar calendar = Calendar.getInstance();

                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int date = calendar.get(Calendar.DAY_OF_MONTH);

                if (params.containsKey("year")) {
                    year = Integer.parseInt(params.get("year"));
                }
                if (params.containsKey("month")) {
                    month = Integer.parseInt(params.get("month"));
                }
                if (params.containsKey("date")) {
                    date = Integer.parseInt("date");
                }

                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month - 1);
                calendar.set(Calendar.DAY_OF_MONTH, date);

                JSONObject statistic = console.getStatisticDataManager().queryStatisticData(domain, calendar);
                if (null == statistic) {
                    respond(response, HttpStatus.NOT_FOUND_404);
                    return;
                }

                JSONObject data = new JSONObject();
                data.put("tag", console.getTag());
                data.put("statistic", statistic);
                data.put("year", calendar.get(Calendar.YEAR));
                data.put("month", calendar.get(Calendar.MONTH) + 1);
                data.put("date", calendar.get(Calendar.DATE));

                respondOk(response, data);
            }
        }
    }
}
