/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher.filestorage;

import cell.core.talk.dialect.ActionDialect;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件分享句柄。
 */
public class FileSharingHandler extends CrossDomainHandler {

    private final static boolean CACHE_FILE = true;

    public final static String PATH = "/sharing/";

    private final String TITLE = "${title}";

    private final String STATE = "${state}";

    private final String CONTENT = "${content}";

    private final String EVENT = "${event}";

    private String fileRoot = "assets/sharing/";

    private Map<String, FlexibleByteBuffer> fileCache;

    private Performer performer;

    public FileSharingHandler(Performer performer) {
        super();
        this.performer = performer;
        if (CACHE_FILE) {
            this.fileCache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String pathInfo = request.getPathInfo();

        if (pathInfo.indexOf(".css") > 0 || pathInfo.indexOf(".js") > 0 ||
            pathInfo.indexOf(".jpg") > 0 || pathInfo.equals(".jpeg") ||
            pathInfo.indexOf(".png") > 0) {
            String filename = pathInfo.substring(1);
            respondFile(response, filename);
        }
        else if (pathInfo.length() > 32) {
            // 显示页面
            String code = extractCode(pathInfo);
            processPage(code, request, response);
        }
        else {
            respond(response, HttpStatus.FORBIDDEN_403);
        }

        complete();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String pathInfo = request.getPathInfo();

        if (pathInfo.indexOf("/trace/browser") == 0) {
            JSONObject bodyJSON = readBodyAsJSONObject(request);

            String ip = request.getRemoteAddr();
            long time = System.currentTimeMillis();

            VisitTrace visitTrace = new VisitTrace(VisitTrace.PLATFORM_BROWSER, time, ip, bodyJSON);

            Packet packet = new Packet(FileStorageAction.Trace.name, visitTrace.toJSON());
            this.performer.transmit(FileStorageCellet.NAME, packet.toDialect());

            this.respond(response, HttpStatus.OK_200);
        }
        else if (pathInfo.indexOf("/trace/applet/wechat") == 0) {

        }
        else {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
        }

        this.complete();
    }

    private void respondFile(HttpServletResponse response, String filename) {
        OutputStream os = null;
        long contentLength = 0;

        FlexibleByteBuffer cache = CACHE_FILE ? this.fileCache.get(filename) : null;
        if (null != cache) {
            // 从缓存加载数据
            try {
                os = response.getOutputStream();
                os.write(cache.array(), 0, cache.limit());
            } catch (IOException e) {
                Logger.w(this.getClass(), "#respondFile", e);
            } finally {
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }

            contentLength = cache.limit();
        }
        else {
            File file = new File(this.fileRoot, filename);
            contentLength = file.length();
            cache = new FlexibleByteBuffer((int) contentLength);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                os = response.getOutputStream();
                byte[] buf = new byte[2048];
                int length = 0;
                while ((length = fis.read(buf)) > 0) {
                    os.write(buf, 0, length);
                    // 写入缓存
                    cache.put(buf, 0, length);
                }

                // 整理
                cache.flip();

                if (null != this.fileCache) {
                    this.fileCache.put(filename, cache);
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

                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        FileType fileType = FileUtils.verifyFileType(filename);
        response.setContentType(fileType.getMimeType());
        response.setContentLengthLong(contentLength);
        response.setStatus(HttpStatus.OK_200);
    }

    private void processPage(String code, HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = new JSONObject();
        data.put("code", code);
        data.put("refresh", true);
        Packet packet = new Packet(FileStorageAction.GetSharingTag.name, data);
        ActionDialect result = this.performer.syncTransmit(FileStorageCellet.NAME, packet.toDialect());
        if (null == result) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        File file = null;
        long contentLength = 0;

        Packet resultPacket = new Packet(result);
        int stateCode = Packet.extractCode(resultPacket);
        if (FileStorageStateCode.Ok.code != stateCode) {
            // 文件分享码不存在
            file = new File(this.fileRoot, "page.html");
            contentLength = processLossPageHtml(file, response);
        }
        else {
            JSONObject tagJson = Packet.extractDataPayload(resultPacket);
            SharingTag sharingTag = new SharingTag(tagJson);

            // 判断分享码是否已过期
            if (sharingTag.getExpiryDate() > 0
                    && System.currentTimeMillis() > sharingTag.getExpiryDate()) {
                // 已过期
                file = new File(this.fileRoot, "page.html");
                contentLength = processExpiredPageHtml(file, response);
            }
            else {
                file = new File(this.fileRoot, "index.html");
                contentLength = processIndexHtml(file, sharingTag, request, response);
            }
        }

        FileType fileType = FileUtils.verifyFileType(file.getName());
        response.setContentType(fileType.getMimeType());
        response.setContentLengthLong(contentLength);
        response.setStatus(HttpStatus.OK_200);
    }

    private long processLossPageHtml(File file, HttpServletResponse response) {
        long contentLength = 0;

        BufferedReader reader = null;
        OutputStream os = null;

        try {
            os = response.getOutputStream();
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains(TITLE)) {
                    line = line.replace(TITLE, "文件不存在");
                }
                else if (line.contains(STATE)) {
                    line = line.replace(STATE, "loss");
                }
                else if (line.contains(CONTENT)) {
                    line = line.replace(CONTENT, "文件不存在或已删除");
                }
                else if (line.contains(EVENT)) {
                    line = line.replace(EVENT, "ViewLoss");
                }

                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                contentLength += bytes.length;
                os.write(bytes);
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "#processLossPageHtml", e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
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

        return contentLength;
    }

    private long processExpiredPageHtml(File file, HttpServletResponse response) {
        long contentLength = 0;

        BufferedReader reader = null;
        OutputStream os = null;

        try {
            os = response.getOutputStream();
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains(TITLE)) {
                    line = line.replace(TITLE, "文件已过期");
                }
                else if (line.contains(STATE)) {
                    line = line.replace(STATE, "expired");
                }
                else if (line.contains(CONTENT)) {
                    line = line.replace(CONTENT, "文件已过期");
                }
                else if (line.contains(EVENT)) {
                    line = line.replace(EVENT, "ViewExpired");
                }

                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                contentLength += bytes.length;
                os.write(bytes);
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "#processExpiredPageHtml", e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
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

        return contentLength;
    }

    private long processIndexHtml(File file, SharingTag sharingTag, HttpServletRequest request, HttpServletResponse response) {
        long contentLength = 0;
        String pageTraceString = null;

        Cookie[] cookies = request.getCookies();
        if (null != cookies && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase("CubeTrace")) {
                    pageTraceString = cookie.getValue().trim();
                    break;
                }
            }
        }

        BufferedReader reader = null;
        OutputStream os = null;

        IndexTemplate template = new IndexTemplate(sharingTag, request.isSecure(), pageTraceString);

        try {
            os = response.getOutputStream();
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = reader.readLine())) {
                // 匹配行
                line = template.matchLine(line);

                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                contentLength += bytes.length;
                os.write(bytes);
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "#processIndexHtml", e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
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

        return contentLength;
    }

    private String extractCode(String pathInfo) {
        int start = 1;
        String path = pathInfo.substring(1);
        int end = path.indexOf("/");
        if (end < 0) {
            end = path.indexOf("?");
        }

        if (end > 0) {
            return pathInfo.substring(start, start + end);
        }
        else {
            return pathInfo.substring(start);
        }
    }
}
