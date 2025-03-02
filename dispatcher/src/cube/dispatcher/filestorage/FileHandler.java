/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Base64;
import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AuthAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.FileLabel;
import cube.common.entity.SharingTag;
import cube.common.state.AuthStateCode;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.auth.AuthCellet;
import cube.dispatcher.util.FormData;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
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

    private File tempPath = new File("cube-fs-tmp/");

    // 上传文件分块大小限制
    private int cacheLimit = 5 * 1024 * 1024;

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
        if (!this.tempPath.exists()) {
            this.tempPath.mkdirs();
        }
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
        if (null == token) {
            token = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }
        }

        // Version
        String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
        if (null == version) {
            version = "v0";
        }

        // SN
        Long sn = (null != request.getParameter("sn")) ?
                Long.parseLong(request.getParameter("sn")) : Utils.generateSerialNumber();

        // 读取流
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        byte[] bytes = new byte[4096];
        InputStream is = request.getInputStream();

        ArrayList<File> tempFiles = new ArrayList<>();

        long total = 0;
        int length = 0;
        while ((length = is.read(bytes)) > 0) {
            buf.put(bytes, 0, length);
            total += length;

            if (buf.position() >= this.cacheLimit) {
                // 整理缓存
                buf.flip();
                File file = this.writeToTempFile(buf, token);
                if (null == file) {
                    for (File f : tempFiles) {
                        f.delete();
                    }
                    tempFiles = null;
                    break;
                }

                tempFiles.add(file);
                buf = new FlexibleByteBuffer();
            }
        }

        if (null == tempFiles) {
            this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
            this.complete();
            return;
        }

        buf.flip();
        File lastFile = this.writeToTempFile(buf, token);
        tempFiles.add(lastFile);

        // Contact ID
        long contactId = 0;
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

        fileName = request.getParameter("filename");
        if (null != fileName) {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        }

        String contentType = request.getHeader(HttpHeader.CONTENT_TYPE.asString());
        if (null == contentType) {
            contentType = "image/jpeg";
        }

        if (null != fileName && !contentType.contains("multipart/form-data")) {
            // 非 Form 格式
//            fileName = URLDecoder.decode(fileName, "UTF-8");
            String sizeStr = request.getParameter("filesize");
            if (null == sizeStr) {
                fileSize = total;
            }
            else {
                fileSize = Long.parseLong(sizeStr);
            }

            String modifiedStr = request.getParameter("modified");
            if (null == modifiedStr) {
                lastModified = System.currentTimeMillis();
            }
            else {
                lastModified = Long.parseLong(modifiedStr);
            }

            // 校验 Token 是否存在
            JSONObject payload = new JSONObject();
            payload.put("code", token);
            Packet packet = new Packet(AuthAction.GetToken.name, payload);
            ActionDialect dialect = this.performer.syncTransmit(AuthCellet.NAME, packet.toDialect());
            if (null == dialect) {
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(dialect);
            if (Packet.extractCode(responsePacket) != AuthStateCode.Ok.code) {
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            // 存在合法令牌
            AuthToken authToken = new AuthToken(Packet.extractDataPayload(responsePacket));
            if (authToken.getExpiry() < System.currentTimeMillis()) {
                // 令牌过期
                Logger.d(this.getClass(), "Token have expired - " + authToken.getCode());
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            contactId = authToken.getContactId();
            domain = authToken.getDomain();

            String fileCode = null;

            byte[] fb = new byte[2048];
            for (File file : tempFiles) {
                FileInputStream fis = null;
                FlexibleByteBuffer fbuf = new FlexibleByteBuffer();
                try {
                    fis = new FileInputStream(file);
                    int len = 0;
                    while ((len = fis.read(fb)) > 0) {
                        fbuf.put(fb, 0, len);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass(), "Read temp file failed", e);
                } finally {
                    if (null != fis) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            // Nothing
                        }
                    }
                }

                fbuf.flip();
                byte[] data = new byte[fbuf.limit()];
                System.arraycopy(fbuf.array(), 0, data, 0, fbuf.limit());
                size = data.length;
                FileChunk chunk = new FileChunk(contactId, domain, token, fileName, fileSize, lastModified, cursor, size, data);
                fileCode = this.fileChunkStorage.append(chunk);
                cursor += size;
            }

            JSONObject responseData = new JSONObject();
            try {
                responseData.put("fileName", fileName);
                responseData.put("fileSize", fileSize);
                responseData.put("fileCode", fileCode);
                responseData.put("lastModified", lastModified);
                responseData.put("position", cursor);
            } catch (JSONException e) {
                Logger.w(this.getClass(), "#doPost", e);
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            if (version.equalsIgnoreCase("v1")) {
                this.respondOk(response, responseData);
            }
            else {
                packet = new Packet(sn, FileStorageAction.UploadFile.name, responseData);
                this.respondOk(response, packet.toJSON());
            }
        }
        else {
            // 文件块数据
            byte[] data = null;

            try {
                buf = this.readFiles(tempFiles);
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
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

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
                Logger.w(this.getClass(), "#doPost", e);
                clearTempFiles(tempFiles);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            if (version.equalsIgnoreCase("v1")) {
                this.respondOk(response, responseData);
            }
            else {
                Packet packet = new Packet(sn, FileStorageAction.UploadFile.name, responseData);
                this.respondOk(response, packet.toJSON());
            }
        }

        clearTempFiles(tempFiles);
        this.complete();
    }

    private File writeToTempFile(FlexibleByteBuffer buf, String token) {
        File file = new File(this.tempPath, token + "_" + Utils.randomInt(100000, 999999) + ".tmp");

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(buf.array(), 0, buf.limit());
        } catch (Exception e) {
            Logger.e(this.getClass(), "#writeToTempFile", e);
            return null;
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        return file;
    }

    private FlexibleByteBuffer readFiles(ArrayList<File> fileList) {
        FlexibleByteBuffer buf = new FlexibleByteBuffer();

        byte[] data = new byte[1024];
        for (File file : fileList) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                int len = 0;
                while ((len = fis.read(data)) > 0) {
                    buf.put(data, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }
        }

        // 整理
        buf.flip();

        return buf;
    }

    private void clearTempFiles(ArrayList<File> files) {
        for (File file : files) {
            file.delete();
        }
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
        // Version
        String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
        if (null == version) {
            version = "v0";
        }

        // Sharing Code
        String sharingCode = request.getParameter("sc");

        FileLabel fileLabel = null;
        FileType type = null;

        if (null != sharingCode) {
            // TODO 支持版本选择和 makeError
            JSONObject payload = new JSONObject();
            payload.put("code", sharingCode);
            payload.put("refresh", false);
            payload.put("full", true);
            Packet packet = new Packet(FileStorageAction.GetSharingTag.name, payload);
            // 请求数据
            ActionDialect dialect = this.performer.syncTransmit(FileStorageCellet.NAME, packet.toDialect());
            if (null == dialect) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(dialect);

            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                // 状态错误
                this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
                this.complete();
                return;
            }

            JSONObject tagJson = Packet.extractDataPayload(responsePacket);
            SharingTag sharingTag = new SharingTag(tagJson);

            // 识别权限
            if (sharingTag.getConfig().isTraceDownload()) {
                // 需要用户进行登录
                String token = request.getParameter("token");
                if (null == token) {
                    Logger.d(this.getClass(), "Need token to download: " + sharingTag.getCode());
                    response.setStatus(HttpStatus.FORBIDDEN_403);
                    this.complete();
                    return;
                }

                // 校验令牌
                payload = new JSONObject();
                payload.put("code", token);
                packet = new Packet(AuthAction.GetToken.name, payload);
                dialect = this.performer.syncTransmit(AuthCellet.NAME, packet.toDialect());
                if (null == dialect) {
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    this.complete();
                    return;
                }

                responsePacket = new Packet(dialect);
                if (Packet.extractCode(responsePacket) == AuthStateCode.Ok.code) {
                    // 存在合法令牌
                    AuthToken authToken = new AuthToken(Packet.extractDataPayload(responsePacket));
                    if (authToken.getExpiry() < System.currentTimeMillis()) {
                        // 令牌过期
                        Logger.d(this.getClass(), "Token have expired - " + authToken.getCode());
                        response.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
                        this.complete();
                        return;
                    }
                }
            }

            // 文件标签
            fileLabel = sharingTag.getConfig().getFileLabel();
            // 文件类型
            type = fileLabel.getFileType();
        }
        else {
            // Token Code
            String token = request.getParameter("token");
            // File Code
            String fileCode = request.getParameter("fc");
            // Domain, optional
            String domain = request.getParameter("domain");
            // Device, optional
            String device = request.getParameter("device");

            if (null == token) {
                token = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
                if (null == token) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }
            }
            if (null == fileCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            // Type
            String typeDesc = request.getParameter("type");
            type = FileType.matchExtension(typeDesc);

            // SN
            Long sn = null;
            if (null != request.getParameter("sn")) {
                sn = Long.parseLong(request.getParameter("sn"));
            }
            else {
                sn = Utils.generateSerialNumber();
            }

            JSONObject payload = new JSONObject();
            payload.put("fileCode", fileCode);

            if (null != domain) {
                domain = URLDecoder.decode(domain, "UTF-8");
                domain = new String(Base64.decode(domain), StandardCharsets.UTF_8);
                payload.put("domain", domain);
            }

            Packet packet = new Packet(sn, FileStorageAction.GetFile.name, payload);
            ActionDialect packetDialect = packet.toDialect();

            if (null != device) {
                // 设置设备信息用于判定是否产生下载事件
                packetDialect.addParam("device", device);
            }

            ActionDialect responseDialect = null;
            token = token.trim();
            packetDialect.addParam("token", token);
            if (this.performer.existsTokenCode(token)) {
                responseDialect = this.performer.syncTransmit(token, FileStorageCellet.NAME, packetDialect);
            }
            else {
                responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
            }
            if (null == responseDialect) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(responseDialect);

            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                Logger.w(this.getClass(), "#doGet - Service state code : " + stateCode);
                // 状态错误
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            JSONObject fileLabelJson = Packet.extractDataPayload(responsePacket);
            // 文件标签
            fileLabel = new FileLabel(fileLabelJson);
            // 文件类型
            type = fileLabel.getFileType();
        }

        // 识别文件类型
        if (type == FileType.UNKNOWN) {
            type = FileType.matchExtension(fileLabel.getFileExtension());
        }

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
        try {
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
            response.setStatus(HttpStatus.OK_200);

            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(buf.array(), 0, buf.limit());
            outputStream.flush();

            buf.clear();
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        } finally {
            HttpClientFactory.getInstance().returnHttpClient(httpClient);
        }
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

            // 填充 Header
            fillHeaders(response, fileLabel, fileLabel.getFileSize(), type);
            response.setStatus(HttpStatus.OK_200);

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
