/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Report;
import cube.dispatcher.aigc.Manager;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 心理学绘画报告状态操作。
 */
public class PsychologyReportPage extends ContextHandler {

    private final String fileRoot = "assets/page/";

    public PsychologyReportPage() {
        super("/aigc/psychology/report/page");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                long sn = Long.parseLong(request.getParameter("sn"));
                String page = request.getParameter("page").toLowerCase();
                Report report = Manager.getInstance().getPsychologyReport(token, sn, false);
                if (report instanceof PaintingReport) {
                    if (page.contains("indicator") || page.contains("default")) {
                        this.respondFile(response, "indicator.html", report.toJSON());
                    }
                    else if (page.contains("bigfive") || page.contains("personality")) {
                        this.respondFile(response, "bigfive.html", report.toJSON());
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    }
                }
                else {
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }

        private void respondFile(HttpServletResponse response, String filename, JSONObject data) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());
            response.setStatus(HttpStatus.OK_200);

            long contentLength = 0;
            OutputStream os = null;
            File file = new File(fileRoot, filename);
            try {
                byte[] fileData = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                String fileContent = new String(fileData, StandardCharsets.UTF_8);
                String segment = data.toString() + ";\n";
                fileContent = fileContent.replace("${data}", segment);

                byte[] responseData = fileContent.getBytes(StandardCharsets.UTF_8);
                contentLength = responseData.length;
                response.setContentLengthLong(contentLength);

                os = response.getOutputStream();
                os.write(responseData);
            } catch (Exception e) {
                Logger.w(this.getClass(), "#respondFile", e);
            } finally {
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
