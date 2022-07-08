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

import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.VisitTrace;
import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 文件分享句柄。
 */
public class FileSharingHandler extends CrossDomainHandler {

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
            String code = pathInfo.substring(1);
            respondFile(response, "index.html");
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
}
