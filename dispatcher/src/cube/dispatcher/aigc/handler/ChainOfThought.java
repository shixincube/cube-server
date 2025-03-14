/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.common.entity.FileLabel;
import cube.dispatcher.aigc.CacheCenter;
import cube.dispatcher.aigc.Manager;
import cube.util.FileType;
import cube.util.FileUtils;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChainOfThought extends ContextHandler {

    private final static String PATH_IMAGE = "image";
    private final static String PATH_SCRIPT = "script";
    private final static String PATH_PAINTING = "painting";
    private final static String PATH_THOUGHT = "thought";

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
            if (pathNames.length < 2) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            String token = pathNames[1];
            String path = (pathNames.length > 2) ? pathNames[2] : null;
            String resource = (pathNames.length > 3) ? pathNames[3] : null;

            if (PATH_SCRIPT.equals(path) || PATH_IMAGE.equals(path)) {
                // 处理脚本文件
                String file = path + "/" + pathNames[3];
                response.setHeader("Connection", "Keep-Alive");
                this.respondFile(response, file);
                this.complete();
                return;
            }
            else if (PATH_PAINTING.equals(path)) {
                if (resource.toLowerCase().endsWith(".jpg") ||
                        resource.toLowerCase().endsWith(".jpeg") ||
                        resource.toLowerCase().endsWith(".png")) {
                    String fileCode = FileUtils.extractFileName(resource);
                    Painting painting = CacheCenter.getInstance().getPainting(fileCode);
                    if (null == painting) {
                        try {
                            JSONObject result = Manager.getInstance().getPsychologyPainting(token, fileCode);
                            painting = new Painting(result);
                            painting.timestamp = System.currentTimeMillis();
                            CacheCenter.getInstance().cache(fileCode, painting);
                        } catch (Exception e) {
                            this.respond(response, HttpStatus.NOT_FOUND_404);
                            this.complete();
                            return;
                        }
                    }
                    this.respondFile(response, painting.fileLabel);
                }
                else if (resource.toLowerCase().endsWith(".json")) {
                    String fileCode = FileUtils.extractFileName(resource);
                    Painting painting = CacheCenter.getInstance().getPainting(fileCode);
                    if (null != painting) {
                        this.respondOk(response, painting.toFullJson());
                    }
                    else {
                        response.setStatus(HttpStatus.NOT_FOUND_404);
                    }
                }
                else {
                    response.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
                }
                this.complete();
                return;
            }
            else if (PATH_THOUGHT.equals(path)) {
                String snString = FileUtils.extractFileName(resource);
                JSONObject json = Manager.getInstance().getPsychologyReportPart(token,
                        Long.parseLong(snString), false, false, true, false, false);
                if (null != json) {
                    this.respondOk(response, json);
                }
                else {
                    response.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
                }
                this.complete();
                return;
            }

            if (null == token) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                long sn = Long.parseLong(request.getParameter("sn"));
                String fileCode = request.getParameter("fc");
                processIndexFile(response, "index.html", token, sn, fileCode);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        }

        private void respondFile(HttpServletResponse response, FileLabel fileLabel) {
            HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
            try {
                byte[] fileData = CacheCenter.getInstance().getData(fileLabel.getFileCode());
                if (null == fileData) {
                    ContentResponse fileResponse = httpClient.GET(fileLabel.getDirectURL());
                    fileData = fileResponse.getContent();
                    // 缓存文件
                    CacheCenter.getInstance().cache(fileLabel.getFileCode(), fileData);
                }

                // 填写头信息
                fillHeaders(response, fileLabel, fileData.length, fileLabel.getFileType());
                response.setStatus(HttpStatus.OK_200);

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(fileData);
                outputStream.flush();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#respondFile", e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            } finally {
                HttpClientFactory.getInstance().returnHttpClient(httpClient);
            }
        }

        private synchronized void respondFile(HttpServletResponse response, String filename) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());

            OutputStream os = null;

            File file = new File(fileRoot, filename);
            long contentLength = file.length();

            response.setContentLengthLong(contentLength);
            response.setStatus(HttpStatus.OK_200);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                os = response.getOutputStream();
                byte[] buf = new byte[20480];
                int length = 0;
                while ((length = fis.read(buf)) > 0) {
                    os.write(buf, 0, length);
                }
                os.flush();
            } catch (IOException e) {
                Logger.w(this.getClass(), "#respondFile", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        private void processIndexFile(HttpServletResponse response, String filename,
                                      String token, long sn, String fileCode) {
            FileType fileType = FileUtils.verifyFileType(filename);
            response.setContentType(fileType.getMimeType());

            OutputStream os = null;
            File file = new File(fileRoot, filename);
            try {
                byte[] fileData = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                String fileContent = new String(fileData, StandardCharsets.UTF_8);
                fileContent = fileContent.replaceAll("\\$\\{token\\}", token);
                fileContent = fileContent.replaceAll("\\$\\{sn\\}", Long.toString(sn));
                fileContent = fileContent.replaceAll("\\$\\{fileCode\\}", fileCode);
                fileContent = fileContent.replaceAll("\\$\\{timestamp\\}",
                        Long.toString(System.currentTimeMillis()));

                byte[] responseData = fileContent.getBytes(StandardCharsets.UTF_8);
                long contentLength = responseData.length;

                response.setContentLengthLong(contentLength);
                response.setStatus(HttpStatus.OK_200);

                os = response.getOutputStream();
                os.write(responseData);
                os.flush();
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
        }

        private void fillHeaders(HttpServletResponse response, FileLabel fileLabel, long length, FileType type) {
            if (FileType.FILE == type) {
                try {
                    StringBuilder buf = new StringBuilder("attachment;");
                    buf.append("filename=").append(URLEncoder.encode(fileLabel.getFileName(), "UTF-8"));
                    response.setHeader("Content-Disposition", buf.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                response.setContentType(type.getMimeType());
            }
            else if (FileType.UNKNOWN == type) {
                response.setContentType(fileLabel.getFileType().getMimeType());
            }
            else if (null != type) {
                response.setContentType(type.getMimeType());
            }
            else {
                response.setContentType(fileLabel.getFileType().getMimeType());
            }

            if (length > 0) {
                response.setContentLengthLong(length);
            }
        }
    }
}
