/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cell.util.log.Logger;
import cube.console.Console;
import cube.report.JVMReport;
import cube.report.LogReport;
import cube.report.PerformanceReport;
import cube.report.Report;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * 报告处理器。
 */
public class ReportHandler extends ContextHandler {

    private Console console;

    public ReportHandler(Console console) {
        super("/report");
        this.setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends AbstractHandler {

        public Handler() {
            super();
        }

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws IOException, ServletException {
            BufferedReader reader = httpServletRequest.getReader();

            StringBuilder buf = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }

            try {
                JSONObject reportJson = new JSONObject(buf.toString());

                // 获取报告名
                String name = Report.extractName(reportJson);
                if (LogReport.NAME.equals(name)) {
                    LogReport report = new LogReport(reportJson);
                    console.appendLogReport(report);
                }
                else if (JVMReport.NAME.equals(name)) {
                    JVMReport report = new JVMReport(reportJson);
                    console.appendJVMReport(report);
                }
                else if (PerformanceReport.NAME.equals(name)) {
                    try {
                        PerformanceReport report = new PerformanceReport(reportJson);
                        console.appendPerformanceReport(report);
                    } catch (Exception e) {
                        Logger.e(this.getClass(), "#handle", e);
                    }
                }

                httpServletResponse.setStatus(HttpStatus.OK_200);
                request.setHandled(true);
            } catch (JSONException e) {
                e.printStackTrace();
                httpServletResponse.setStatus(HttpStatus.BAD_REQUEST_400);
                request.setHandled(true);
            }
        }
    }
}
