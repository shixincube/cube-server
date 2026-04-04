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
import java.nio.file.Paths;

public class InterfaceDocument extends ContextHandler {

    private final String root = "assets/doc/";

    public InterfaceDocument() {
        super("/doc/api/");
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
                response.setStatus(HttpStatus.OK_200);
                if (pathInfo.equals("/") || pathInfo.equalsIgnoreCase("/index.html")) {
                    response.setContentType(FileType.HTML.getMimeType());
                    byte[] buf = Files.readAllBytes(Paths.get(root, "index.html"));
                    response.getOutputStream().write(buf);
                    response.getOutputStream().flush();
                }
                else {
                    FileType fileType = matchFileType(pathInfo);
                    if (fileType != FileType.UNKNOWN) {
                        response.setContentType(fileType.getMimeType());
                    }
                    byte[] buf = Files.readAllBytes(Paths.get(root, pathInfo));
                    response.getOutputStream().write(buf);
                    response.getOutputStream().flush();
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "", e);
                response.setStatus(HttpStatus.NOT_FOUND_404);
            }

            complete();
        }

        private FileType matchFileType(String pathInfo) {
            if (pathInfo.toLowerCase().endsWith("css")) {
                return FileType.CSS;
            }
            else if (pathInfo.toLowerCase().endsWith("js")) {
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
                return FileType.UNKNOWN;
            }
        }
    }
}
