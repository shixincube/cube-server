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

public class ChainOfThought extends ContextHandler {

    private final static String PATH_SCRIPT = "script";
    private final static String PATH_PAINTING = "painting";

    private final String fileRoot = "assets/cot/";

    public ChainOfThought() {
        super("/aigc/cot/");
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
            String[] pathNames = pathInfo.split("/");
            String token = pathNames[1];
            String path = (pathNames.length > 2) ? pathNames[2] : null;
            String resource = (pathNames.length > 3) ? pathNames[3] : null;

            if (PATH_SCRIPT.equals(path)) {
                // 处理脚本文件
                String file = PATH_SCRIPT + "/" + pathNames[3];
                response.setHeader("Connection", "Keep-Alive");
                this.respondFile(response, file);
                this.complete();
                return;
            }
            else if (PATH_PAINTING.equals(path)) {

                this.complete();
                return;
            }

            if (null == token) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            long sn = Long.parseLong(request.getParameter("sn"));
            processIndexFile(response, "index.html", sn);
            this.complete();
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

        private void processIndexFile(HttpServletResponse response, String filename, long sn) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());

            long contentLength = 0;
            OutputStream os = null;
            File file = new File(fileRoot, filename);
            try {
                byte[] fileData = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                String fileContent = new String(fileData, StandardCharsets.UTF_8);
                fileContent = fileContent.replaceAll("\\$\\{sn\\}", Long.toString(sn));

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
