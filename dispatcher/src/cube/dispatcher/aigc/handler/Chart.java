/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Chart extends ContextHandler {

    private final static String PATH_PSYCHOLOGY_PAINTING_INFERENCE = "ppi";

    private final String fileRoot = "assets/chart/";

    public Chart() {
        super("/aigc/chart/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String pathInfo = request.getPathInfo();
            if (pathInfo.endsWith("js")) {
                String file = pathInfo.replace("/" + PATH_PSYCHOLOGY_PAINTING_INFERENCE, "");
                response.setHeader("Connection", "keep-alive");
                this.respondFile(response, file);
                this.complete();
                return;
            }

            String path = this.getRequestPath(request);
            String token = this.getLastRequestPath(request);
            if (null == token) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            if (PATH_PSYCHOLOGY_PAINTING_INFERENCE.equals(path)) {
                try {
                    long sn = Long.parseLong(request.getParameter("sn"));
                    JSONObject data = Manager.getInstance().getPsychologyPaintingChart(token, sn);
                    processIndexFile(response, "index.html", data);
                    this.complete();
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                }
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }

        private void respondFile(HttpServletResponse response, String filename) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());

            OutputStream os = null;
            long contentLength = 0;

            File file = new File(fileRoot, filename);
            contentLength = file.length();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                os = response.getOutputStream();
                byte[] buf = new byte[20480];
                int length = 0;
                while ((length = fis.read(buf)) > 0) {
                    os.write(buf, 0, length);
                }
            } catch (IOException e) {
                Logger.w(this.getClass(), "#respondFile", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }

            response.setContentLengthLong(contentLength);
            response.setStatus(HttpStatus.OK_200);
        }

        private void processIndexFile(HttpServletResponse response, String filename, JSONObject data) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());

            long contentLength = 0;
            OutputStream os = null;
            File file = new File(fileRoot, filename);
            try {
                byte[] fileData = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                String fileContent = new String(fileData, StandardCharsets.UTF_8);
                String segment = data.toString(4) + ";\n";
                fileContent = fileContent.replace("${data}", segment);

                byte[] responseData = fileContent.getBytes(StandardCharsets.UTF_8);
                contentLength = responseData.length;
                os = response.getOutputStream();
                os.write(responseData);
            } catch (Exception e) {
                Logger.w(this.getClass(), "#processIndexFile", e);
            } finally {
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }

            response.setContentLengthLong(contentLength);
            response.setStatus(HttpStatus.OK_200);
        }
    }
}
