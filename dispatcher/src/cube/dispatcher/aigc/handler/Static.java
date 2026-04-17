/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.util.FileType;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Static extends ContextHandler {

    private final String root = "assets/static/";

    public Static() {
        super("/static/");
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

            try {
                Path path = Paths.get(root, pathInfo);
                if (path.toFile().exists()) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#doGet - Response file: " + path.getFileName().toString());
                    }

                    response.setStatus(HttpStatus.OK_200);
                    FileType fileType = matchFileType(pathInfo);
                    response.setContentType(fileType.getMimeType());
                    byte[] buf = Files.readAllBytes(path);
                    response.getOutputStream().write(buf);
                    response.getOutputStream().flush();
                }
                else {
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "", e);
                response.setStatus(HttpStatus.UNAUTHORIZED_401);
            }

            complete();
        }

        private FileType matchFileType(String pathInfo) {
            if (pathInfo.toLowerCase().endsWith("css")) {
                return FileType.CSS;
            }
            else if (pathInfo.toLowerCase().endsWith("js") || pathInfo.toLowerCase().endsWith("mjs")) {
                return FileType.JS;
            }
            else if (pathInfo.toLowerCase().endsWith("png")) {
                return FileType.PNG;
            }
            else if (pathInfo.toLowerCase().endsWith("jpg") || pathInfo.toLowerCase().endsWith("jpeg")) {
                return FileType.JPEG;
            }
            else if (pathInfo.toLowerCase().endsWith("ico")) {
                return FileType.ICO;
            }
            else {
                return FileType.BIN;
            }
        }
    }
}
