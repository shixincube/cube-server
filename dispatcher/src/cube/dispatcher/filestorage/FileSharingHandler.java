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
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;
import cube.util.FileSize;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 文件分享句柄。
 */
public class FileSharingHandler extends CrossDomainHandler {

    public final static String PATH = "/sharing/";

    private final String TITLE = "${title}";

    private final String FILE_NAME = "${file_name}";

    private final String FILE_SIZE = "${file_size}";

    private final String FILE_URI_PATH = "${file_uri_path}";

    private String fileRoot = "assets/sharing/";

    private Performer performer;

    public FileSharingHandler(Performer performer) {
        super();
        this.performer = performer;
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
        else if (pathInfo.length() > 16) {
            String code = extractCode(pathInfo);
            processIndexHtml(code, response);
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

        if (pathInfo.equalsIgnoreCase("/trace")) {
            JSONObject bodyJSON = readBodyAsJSONObject(request);

            String ip = request.getRemoteAddr();
            long time = System.currentTimeMillis();

            VisitTrace visitTrace = new VisitTrace(time, ip, bodyJSON);

            Packet packet = new Packet(FileStorageAction.Trace.name, visitTrace.toJSON());
            this.performer.transmit(FileStorageCellet.NAME, packet.toDialect());

            this.respond(response, HttpStatus.OK_200);
        }
    }

    private void respondFile(HttpServletResponse response, String filename) {
        File file = new File(this.fileRoot, filename);
        FileInputStream fis = null;
        OutputStream os = null;

        try {
            fis = new FileInputStream(file);
            os = response.getOutputStream();
            byte[] buf = new byte[2048];
            int length = 0;
            while ((length = fis.read(buf)) > 0) {
                os.write(buf, 0, length);
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "#readFile", e);
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

        FileType fileType = FileUtils.verifyFileType(filename);
        response.setContentType(fileType.getMimeType());
        response.setContentLengthLong(file.length());
        response.setStatus(HttpStatus.OK_200);
    }

    private void processIndexHtml(String code, HttpServletResponse response) {
        JSONObject data = new JSONObject();
        data.put("code", code);
        Packet packet = new Packet(FileStorageAction.GetSharingTag.name, data);
        ActionDialect result = this.performer.syncTransmit(FileStorageCellet.NAME, packet.toDialect());
        if (null == result) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        Packet resultPacket = new Packet(result);
        int stateCode = Packet.extractCode(resultPacket);
        if (FileStorageStateCode.Ok.code != stateCode) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
            return;
        }

        JSONObject tagJson = Packet.extractDataPayload(resultPacket);
        SharingTag sharingTag = new SharingTag(tagJson);

        File file = new File(this.fileRoot, "index.html");
        BufferedReader reader = null;
        OutputStream os = null;

        try {
            os = response.getOutputStream();
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains(TITLE)) {
                    line = line.replace(TITLE, sharingTag.getConfig().getFileLabel().getFileName());
                }
                else if (line.contains(FILE_NAME)) {
                    line = line.replace(FILE_NAME, sharingTag.getConfig().getFileLabel().getFileName());
                }
                else if (line.contains(FILE_SIZE)) {
                    FileSize size = FileUtils.scaleFileSize(sharingTag.getConfig().getFileLabel().getFileSize());
                    line = line.replace(FILE_SIZE, size.toString());
                }
                else if (line.contains(FILE_URI_PATH)) {
                    String uri = FileHandler.PATH + "?sc=" + sharingTag.getCode();
                    line = line.replace(FILE_URI_PATH, uri);
                }

                os.write(line.getBytes(StandardCharsets.UTF_8));
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

        FileType fileType = FileUtils.verifyFileType(file.getName());
        response.setContentType(fileType.getMimeType());
        response.setContentLengthLong(file.length());
        response.setStatus(HttpStatus.OK_200);
    }

    private String extractCode(String pathInfo) {
        int start = 1;
        int end = pathInfo.substring(1).indexOf("/");
        if (end > 0) {
            return pathInfo.substring(start, start + end);
        }
        else {
            return pathInfo.substring(start);
        }
    }
}
