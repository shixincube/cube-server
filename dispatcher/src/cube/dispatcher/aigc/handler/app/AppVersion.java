/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 应用程序版本信息。
 */
public class AppVersion extends ContextHandler {

    public AppVersion() {
        super("/app/version/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = getApiToken(request);
                if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                String lastPath = this.getLastRequestPath(request);
                if (lastPath.equalsIgnoreCase("android")) {
                    JSONObject responseJson = Manager.getInstance().getAppVersion(token);
                    String version = responseJson.getString("version");
                    File file = new File("assets/app/MindEcho-" + version + ".apk");
                    response.setContentType("application/vnd.android.package-archive");
                    response.setContentLengthLong(file.length());
                    response.setHeader("Content-Disposition", "attachment; filename=\"MindEcho-" + version + ".apk\"");
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        byte[] bytes = new byte[8 * 1024];
                        int length = 0;
                        while ((length = fis.read(bytes)) > 0) {
                            response.getOutputStream().write(bytes, 0, length);
                        }
                        response.getOutputStream().flush();
                    } catch (IOException ioe) {
                        Logger.e(this.getClass(), "#doGet", ioe);
                    } finally {
                        if (null != fis) {
                            fis.close();
                        }
                    }
                    this.complete();
                }
                else {
                    JSONObject responseJson = Manager.getInstance().getAppVersion(token);
                    this.respondOk(response, responseJson);
                    this.complete();
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = this.getApiToken(request);
                if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                JSONObject responseJson = Manager.getInstance().getAppVersion(token);
                this.respondOk(response, responseJson);
                this.complete();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
