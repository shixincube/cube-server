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
import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.util.FormData;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 文件上传/下载处理。
 */
public class FileHandler extends CrossDomainHandler {

    public final static String PATH = "/filestorage/file/";

    private FileChunkStorage fileChunkStorage;

    private Performer performer;

    private int bufferSize = 10 * 1024 * 1024;

    /**
     * 构造函数。
     *
     * @param fileChunkStorage
     * @param performer
     */
    public FileHandler(FileChunkStorage fileChunkStorage, Performer performer) {
        super();
        this.fileChunkStorage = fileChunkStorage;
        this.performer = performer;
    }

    /**
     * 上传文件处理。
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // Token Code
        String token = request.getParameter("token");
        // SN
        Long sn = Long.parseLong(request.getParameter("sn"));

        // 读取流
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        byte[] bytes = new byte[4096];
        InputStream is = request.getInputStream();

        int length = 0;
        while ((length = is.read(bytes)) > 0) {
            buf.put(bytes, 0, length);
        }

        // 整理缓存
        buf.flip();

        // Contact ID
        Long contactId = null;
        // 域
        String domain = null;
        // 文件大小
        long fileSize = 0;
        // 文件修改时间
        long lastModified = 0;
        // 文件块所处的索引位置
        long cursor = 0;
        // 文件块大小
        int size = 0;
        // 文件名
        String fileName = null;
        // 文件块数据
        byte[] data = null;

        try {
            FormData formData = new FormData(buf.array(), 0, buf.limit());

            contactId = Long.parseLong(formData.getValue("cid"));
            domain = formData.getValue("domain");
            fileSize = Long.parseLong(formData.getValue("fileSize"));
            lastModified = Long.parseLong(formData.getValue("lastModified"));
            cursor = Long.parseLong(formData.getValue("cursor"));
            size = Integer.parseInt(formData.getValue("size"));
            fileName = formData.getFileName();
            data = formData.getFileChunk();
        } catch (Exception e) {
            Logger.w(this.getClass(), "#doPost", e);
            this.respond(response, HttpStatus.FORBIDDEN_403, new JSONObject());
            return;
        }

        buf = null;

        FileChunk chunk = new FileChunk(contactId, domain, token, fileName, fileSize, lastModified, cursor, size, data);
        String fileCode = this.fileChunkStorage.append(chunk);

        JSONObject responseData = new JSONObject();
        try {
            responseData.put("fileName", fileName);
            responseData.put("fileSize", fileSize);
            responseData.put("fileCode", fileCode);
            responseData.put("lastModified", lastModified);
            responseData.put("position", cursor + size);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("data", responseData);
            payload.put("code", FileStorageStateCode.Ok.code);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet packet = new Packet(sn, FileStorageAction.UploadFile.name, payload);

        this.respondOk(response, packet.toJSON());

        this.complete();
    }

    /**
     * 下载文件处理。
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // Token Code
        String token = request.getParameter("token");
        // File Code
        String fileCode = request.getParameter("fc");

        if (null == token || null == fileCode) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        // Type
        String typeDesc = request.getParameter("type");
        FileType type = FileType.matchExtension(typeDesc);

        // SN
        Long sn = null;
        if (null != request.getParameter("sn")) {
            sn = Long.parseLong(request.getParameter("sn"));
        }
        else {
            sn = Utils.generateSerialNumber();
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("fileCode", fileCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet(sn, FileStorageAction.GetFile.name, payload);
        ActionDialect packetDialect = packet.toDialect();
        packetDialect.addParam("token", token);

        ActionDialect dialect = this.performer.syncTransmit(token, FileStorageCellet.NAME, packetDialect);
        if (null == dialect) {
            this.respond(response, HttpStatus.FORBIDDEN_403, packet.toJSON());
            return;
        }

        Packet responsePacket = new Packet(dialect);

        int code = -1;
        JSONObject fileLabelJson = null;
        try {
            code = responsePacket.data.getInt("code");
            if (code != FileStorageStateCode.Ok.code) {
                // 状态错误
                this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
                return;
            }

            fileLabelJson = responsePacket.data.getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);

        if (fileLabel.getFileSize() > (long) this.bufferSize) {
            this.processByNonBlocking(request, response, fileLabel, type);
        }
        else {
            this.processByBlocking(request, response, fileLabel, type);
        }
    }

    private void processByBlocking(HttpServletRequest request, HttpServletResponse response,
                                   FileLabel fileLabel, FileType type)
            throws IOException, ServletException {
        final Object mutex = new Object();

        final FlexibleByteBuffer buf = new FlexibleByteBuffer((int)fileLabel.getFileSize());

        HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
        httpClient.newRequest(fileLabel.getDirectURL())
                .send(new BufferingResponseListener(this.bufferSize) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            byte[] responseContent = getContent();
                            buf.put(responseContent);
                        }

                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                });

        synchronized (mutex) {
            try {
                mutex.wait(10 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        buf.flip();

        // 填写头信息
        this.fillHeaders(response, fileLabel, buf.limit(), type);

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(buf.array(), 0, buf.limit());

        buf.clear();

        response.setStatus(HttpStatus.OK_200);

        HttpClientFactory.getInstance().returnHttpClient(httpClient);

        this.complete();
    }

    private void processByNonBlocking(HttpServletRequest request, HttpServletResponse response,
                                      FileLabel fileLabel, FileType type)
            throws IOException, ServletException {
        InputStreamResponseListener listener = new InputStreamResponseListener();

        HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
        httpClient.newRequest(fileLabel.getDirectURL())
                .timeout(10, TimeUnit.SECONDS)
                .send(listener);

        Response clientResponse = null;
        try {
            clientResponse = listener.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (null != clientResponse && clientResponse.getStatus() == HttpStatus.OK_200) {
            InputStream content = listener.getInputStream();

            // Async output
            AsyncContext async = request.startAsync();
            ServletOutputStream output = response.getOutputStream();
            StandardDataStream dataStream = new StandardDataStream(content, async, output);
            async.addListener(new AsyncListener() {
                @Override
                public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onStartAsync");
                }

                @Override
                public void onComplete(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onComplete");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }

                @Override
                public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onTimeout");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }

                @Override
                public void onError(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onError");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }
            });

            // 设置数据写入监听器
            output.setWriteListener(dataStream);

            // 填充 Header
            fillHeaders(response, fileLabel, fileLabel.getFileSize(), type);
            response.setStatus(HttpStatus.OK_200);
        }
        else {
            this.respond(response, HttpStatus.BAD_REQUEST_400, fileLabel.toCompactJSON());
            HttpClientFactory.getInstance().returnHttpClient(httpClient);
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

    /**
     * 标准数据流输出。
     */
    private final class StandardDataStream implements WriteListener {

        private final InputStream content;
        private final AsyncContext async;
        private final ServletOutputStream output;

        protected long contentLength = 0;

        private StandardDataStream(InputStream content, AsyncContext async, ServletOutputStream output) {
            this.content = content;
            this.async = async;
            this.output = output;
        }

        @Override
        public void onWritePossible() throws IOException {
            byte[] buffer = new byte[4096];

            // 输出流是否就绪
            while (this.output.isReady()) {
                int len = this.content.read(buffer);
                if (len < 0) {
                    this.async.complete();
                    try {
                        this.content.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                // 将数据写入输出流
                this.output.write(buffer, 0, len);
                this.contentLength += len;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Logger.w(this.getClass(), "Async Error", throwable);
            this.async.complete();

            try {
                this.content.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
