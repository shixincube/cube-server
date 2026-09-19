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
import cube.dispatcher.Director;
import cube.dispatcher.Performer;
import cube.dispatcher.auth.AuthCellet;
import cube.dispatcher.util.FormData;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.FileUtils;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件上传/下载处理。
 */
public class FileHandler extends CrossDomainHandler {

    public final static String PATH = "/filestorage/file/";

    protected final int maxUploadConcurrency;

    protected final int maxDownloadConcurrency;

    protected final AtomicInteger uploadConcurrency = new AtomicInteger(0);

    protected final AtomicInteger downloadConcurrency = new AtomicInteger(0);

    private FileChunkStorage fileChunkStorage;

    private Performer performer;

    private int bufferSize = 10 * 1024 * 1024;

    private File tempPath = new File("cube-fs-tmp/");

    // 上传文件分块大小限制
    private int cacheLimit = 5 * 1024 * 1024;

    /**
     * 等待文件转储到文件存储服务的超时时长（毫秒）。
     * 用于在响应客户端之前确认文件已经完成入库，避免客户端拿到 fileCode 后
     * 立即查询 AIGCService#getFile 时因异步转储未完成而查询失败。
     */
    private long uploadWaitTimeout = 30 * 1000L;

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
        this.maxUploadConcurrency = performer.getConcurrentFileInputLimit();
        this.maxDownloadConcurrency = performer.getConcurrentFileOutputLimit();
        if (!this.tempPath.exists()) {
            this.tempPath.mkdirs();
        }

        try {
            this.uploadWaitTimeout = Long.parseLong(
                    performer.getProperties().getProperty("filestorage.upload.timeout", "30000"));
        } catch (Exception e) {
            // 使用默认值
        }
        if (this.uploadWaitTimeout <= 0) {
            this.uploadWaitTimeout = 30 * 1000L;
        }

        Logger.i(this.getClass(), "The file handler concurrency config (U/D): "
                + this.maxUploadConcurrency + "/" + this.maxDownloadConcurrency
                + " - upload wait timeout: " + this.uploadWaitTimeout + " ms");
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
        // 判断并发数量
        if (this.uploadConcurrency.get() >= this.maxUploadConcurrency) {
            Logger.w(this.getClass(), "#doPost - The upload connection reaches the maximum concurrent number : " +
                    uploadConcurrency.get() + "/" + maxUploadConcurrency);
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
            this.complete();
            return;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#doPost - The realtime upload concurrency: " + uploadConcurrency.get());
        }

        this.uploadConcurrency.incrementAndGet();

        try {
            // Token Code
            String tokenParam = request.getParameter("token");
            if (null == tokenParam) {
                tokenParam = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
                if (null == tokenParam) {
                    this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                    this.complete();
                    return;
                }
            }
            final String token = tokenParam;

            // 回传方式，是否使用 HTTP 流方式
            boolean streamTransmission = true;
            String st = request.getParameter("st");
            if (null != st && (st.equalsIgnoreCase("false") || st.equals("0"))) {
                streamTransmission = false;
                Logger.d(this.getClass(), "#doPost - Close stream transmission - " + token);
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
            byte[] bytes = new byte[8 * 1024];
            InputStream is = request.getInputStream();

            final ArrayList<File> tempFiles = new ArrayList<>();

            long total = 0;
            int length = 0;
            boolean error = false;
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
                        tempFiles.clear();
                        error = true;
                        break;
                    }

                    tempFiles.add(file);
                    buf = new FlexibleByteBuffer();
                }
            }

            if (error) {
                Logger.w(this.getClass(), "#doPost - No file data");
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            buf.flip();
            File lastFile = this.writeToTempFile(buf, token);
            tempFiles.add(lastFile);

            String fileNameParam = request.getParameter("filename");
            if (null != fileNameParam) {
                fileNameParam = URLDecoder.decode(fileNameParam, "UTF-8");
            }
            // 文件名
            final String fileName = fileNameParam;

            String contentType = request.getHeader(HttpHeader.CONTENT_TYPE.asString());
            if (null == contentType) {
                FileType fileType = FileUtils.extractFileExtensionType(fileName);
                contentType = fileType.getMimeType();
            }

            if (null != fileName && !contentType.contains("multipart/form-data")) {
                // 非 Form 格式
                String sizeStr = request.getParameter("filesize");
                long fsize = 0;
                if (null == sizeStr) {
                    fsize = total;
                }
                else {
                    fsize = Long.parseLong(sizeStr);
                }

                String modifiedStr = request.getParameter("modified");
                long lm = 0;
                if (null == modifiedStr) {
                    lm = System.currentTimeMillis();
                }
                else {
                    lm = Long.parseLong(modifiedStr);
                }

                // 文件大小
                final long fileSize = fsize;
                // 文件修改时间
                final long lastModified = lm;

                // 校验 Token
                AuthToken authToken = this.performer.verifyToken(token);

                final long contactId = authToken.getContactId();
                final String domain = authToken.getDomain();

                final String fileCode = FileUtils.makeFileCode(contactId, domain, fileName);

                // 文件转储完成信号。用于在应答客户端之前确认文件已经在文件存储服务完成入库，
                // 避免客户端使用 fileCode 立即查询时因异步转储未完成而查不到文件。
                final UploadCompletion completion = new UploadCompletion();

                if (streamTransmission) {
                    // 流式传输：本次请求即为完整文件，等待转储完成后再应答
                    completion.enableWaiting();

                    try {
                        this.performer.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Director director = performer.selectDirector(token, FileStorageCellet.NAME);
                                if (null == director || null == director.fileEndpoint) {
                                    Logger.e(this.getClass(), "#doPost - No available file storage director: " + fileCode);
                                    completion.failed();
                                    clearTempFiles(tempFiles);
                                    return;
                                }

                                Logger.d(this.getClass(), "#doPost - stream transmission on HTTP: " + fileCode + " - "
                                        + director.fileEndpoint.toString());

                                HttpURLConnection conn = null;
                                OutputStream os = null;
                                BufferedInputStream bis = null;
                                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                                try {
                                    URL url = new URL("http://" + director.fileEndpoint.toString() + "/files/receive/"
                                            + "?token=" + token + "&filename=" + URLEncoder.encode(fileName, "UTF-8"));
                                    conn = (HttpURLConnection) url.openConnection();
                                    conn.setDoInput(true);
                                    conn.setDoOutput(true);
                                    conn.setRequestMethod("POST");
                                    conn.setUseCaches(false);
                                    conn.setRequestProperty("Content-Type", "binary");
                                    conn.setRequestProperty("Connection", "Keep-Alive");
                                    conn.setRequestProperty("Accept", "*/*");
                                    conn.setRequestProperty("Cache-Control", "no-cache");
                                    // Connect
                                    conn.connect();
                                    os = conn.getOutputStream();
                                    byte[] fb = new byte[8 * 1024];
                                    for (File file : tempFiles) {
                                        FileInputStream fis = null;
                                        try {
                                            fis = new FileInputStream(file);
                                            int len = 0;
                                            while ((len = fis.read(fb)) > 0) {
                                                os.write(fb, 0, len);
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
                                    }

                                    os.flush();

                                    int stateCode = conn.getResponseCode();
                                    if (stateCode == 200) {
                                        bis = new BufferedInputStream(conn.getInputStream());
                                        int len = 0;
                                        while ((len = bis.read(fb)) > 0) {
                                            buffer.write(fb, 0, len);
                                            buffer.flush();
                                        }

                                        String responseString = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                                        FileLabel fileLabel = new FileLabel(new JSONObject(responseString));
                                        Logger.d(this.getClass(), "#doPost - Upload file: " + fileLabel.getFileCode());
                                        // 文件已完成入库，通知等待线程
                                        completion.completed(fileLabel);
                                    }
                                    else {
                                        Logger.e(this.getClass(), "#doPost - Upload file failed - code: " + stateCode);
                                        completion.failed();
                                    }
                                } catch (Exception e) {
                                    Logger.e(this.getClass(), "#doPost - Upload file failed", e);
                                    completion.failed();
                                } finally {
                                    try {
                                        if (null != os) {
                                            os.close();
                                        }
                                    } catch (Exception e) {
                                        // Nothing
                                    }

                                    try {
                                        if (null != bis) {
                                            bis.close();
                                        }
                                    } catch (Exception e) {
                                        // Nothing
                                    }

                                    try {
                                        buffer.close();
                                    } catch (Exception e) {
                                        // Nothing
                                    }

                                    try {
                                        if (null != conn) {
                                            conn.disconnect();
                                        }
                                    } catch (Exception e) {
                                        // Nothing
                                    }

                                    clearTempFiles(tempFiles);

                                    // 兜底释放等待线程，避免异常分支下请求被挂起
                                    completion.finish();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(this.getClass(), "#doPost - Submit stream transmission task failed: " + fileCode, e);
                        completion.failed();
                    }

                    // 有界等待文件转储完成
                    completion.waitFor(this.uploadWaitTimeout);
                }
                else {
                    // 非流式传输：仅当本次请求已包含完整文件数据时才等待转储完成，
                    // 分片上传的中间分片不能等待，否则请求会被阻塞到超时。
                    final boolean wholeFile = (fileSize > 0 && total >= fileSize);
                    if (wholeFile) {
                        completion.enableWaiting();

                        // 注册完成监听，文件区块组装完成后由 FileChunkStorage 回调
                        this.fileChunkStorage.addListener(fileCode, new FileChunkEventListener() {
                            @Override
                            public void onCompleted(FileLabel fileLabel) {
                                completion.completed(fileLabel);
                            }

                            @Override
                            public void onFailed(String fileCode) {
                                completion.failed();
                            }
                        });
                    }

                    try {
                        this.performer.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                int size = 0;
                                long cursor = 0;

                                byte[] fb = new byte[8 * 1024];

                                try {
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
                                        fileChunkStorage.append(chunk, fileCode);
                                        cursor += size;
                                    }
                                } catch (Exception e) {
                                    Logger.e(this.getClass(), "#doPost - Append file chunk failed: " + fileCode, e);
                                    completion.failed();
                                }

                                clearTempFiles(tempFiles);

                                if (wholeFile) {
                                    // 兜底释放等待线程，避免异常分支下请求被挂起
                                    completion.finish();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(this.getClass(), "#doPost - Submit chunk transmission task failed: " + fileCode, e);
                        completion.failed();
                    }

                    if (wholeFile) {
                        // 有界等待文件组装并入库完成
                        completion.waitFor(this.uploadWaitTimeout);
                        if (!completion.isDone()) {
                            // 等待超时，移除监听器避免无效回调驻留
                            this.fileChunkStorage.removeListener(fileCode);
                        }
                    }
                }

                JSONObject responseData = new JSONObject();
                try {
                    responseData.put("fileName", fileName);
                    responseData.put("fileSize", fileSize);
                    responseData.put("fileCode", fileCode);
                    responseData.put("lastModified", lastModified);
                    responseData.put("position", fileSize);

                    // 回填文件转储状态。completed 表示文件已在文件存储服务完成入库，
                    // 客户端可以立即使用 fileCode 进行查询。
                    if (completion.isWaiting()) {
                        FileLabel uploadedLabel = completion.getFileLabel();
                        if (null != uploadedLabel) {
                            responseData.put("state", "completed");
                            responseData.put("fileLabel", uploadedLabel.toJSON());
                        }
                        else if (completion.isDone()) {
                            Logger.w(this.getClass(), "#doPost - Upload file to storage failed: " + fileCode);
                            responseData.put("state", "failed");
                        }
                        else {
                            Logger.w(this.getClass(), "#doPost - Wait file dump timeout: " + fileCode
                                    + " - " + this.uploadWaitTimeout + " ms");
                            responseData.put("state", "pending");
                        }
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass(), "#doPost", e);
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    this.complete();
                    return;
                }

                if (version.equalsIgnoreCase("v1")) {
                    this.respondOk(response, responseData);
                    this.complete();
                }
                else {
                    Packet packet = new Packet(sn, FileStorageAction.UploadFile.name, responseData);
                    this.respondOk(response, packet.toJSON());
                    this.complete();
                }
            }
            else {
                // 文件块数据
                byte[] data = null;
                // Contact ID
                long contactId = 0;
                // 域
                String domain = null;
                // 文件块所处的索引位置
                long cursor = 0;
                // 文件块大小
                int size = 0;
                long fileSize = 0;
                long lastModified = 0;

                String pFileName = fileName;
                try {
                    buf = this.readFiles(tempFiles);
                    FormData formData = new FormData(buf.array(), 0, buf.limit());

                    contactId = Long.parseLong(formData.getValue("cid"));
                    domain = formData.getValue("domain");
                    fileSize = Long.parseLong(formData.getValue("fileSize"));
                    lastModified = Long.parseLong(formData.getValue("lastModified"));
                    cursor = Long.parseLong(formData.getValue("cursor"));
                    size = Integer.parseInt(formData.getValue("size"));
                    pFileName = formData.getFileName();
                    data = formData.getFileChunk();
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#doPost", e);
                    clearTempFiles(tempFiles);
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                final String fileCode = FileUtils.makeFileCode(contactId, domain, pFileName);
                FileChunk chunk = new FileChunk(contactId, domain, token, pFileName, fileSize, lastModified, cursor, size, data);
                this.fileChunkStorage.append(chunk, fileCode);

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
                    this.complete();
                }
                else {
                    Packet packet = new Packet(sn, FileStorageAction.UploadFile.name, responseData);
                    this.respondOk(response, packet.toJSON());
                    this.complete();
                }

                clearTempFiles(tempFiles);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#doPost", e);
        } finally {
            this.uploadConcurrency.decrementAndGet();
        }
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
        try {
            for (File file : files) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        if (this.downloadConcurrency.get() >= this.maxDownloadConcurrency) {
            Logger.w(this.getClass(), "#doGet - The download connection reaches the maximum concurrent number : " +
                    downloadConcurrency.get() + "/" + maxDownloadConcurrency);
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
            this.complete();
            return;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#doGet - The realtime download concurrency: " + downloadConcurrency.get());
        }

        this.downloadConcurrency.incrementAndGet();

        try {
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
        } catch (Exception e) {
            Logger.e(this.getClass(), "#doGet", e);
        } finally {
            this.downloadConcurrency.decrementAndGet();
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
     * 文件转储完成信号。
     *
     * 上传请求在应答客户端之前，通过该类等待文件转储到文件存储服务完成，
     * 从而保证客户端拿到响应后即可使用 fileCode 查询到文件标签。
     */
    protected final class UploadCompletion {

        private final CountDownLatch latch = new CountDownLatch(1);

        private volatile FileLabel fileLabel = null;

        private volatile boolean waiting = false;

        private volatile boolean done = false;

        /**
         * 是否需要等待转储完成。分片上传的中间分片不等待。
         */
        protected void enableWaiting() {
            this.waiting = true;
        }

        protected boolean isWaiting() {
            return this.waiting;
        }

        /**
         * 转储成功。
         */
        protected void completed(FileLabel fileLabel) {
            this.fileLabel = fileLabel;
            this.done = true;
            this.latch.countDown();
        }

        /**
         * 转储失败。
         */
        protected void failed() {
            this.done = true;
            this.latch.countDown();
        }

        /**
         * 释放等待线程，用于异常分支兜底。
         */
        protected void finish() {
            this.latch.countDown();
        }

        /**
         * 转储动作是否已结束，成功或失败均返回 true。
         */
        protected boolean isDone() {
            return this.done;
        }

        protected FileLabel getFileLabel() {
            return this.fileLabel;
        }

        /**
         * 有界等待转储完成。
         */
        protected void waitFor(long timeout) {
            if (!this.waiting) {
                return;
            }

            try {
                this.latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Logger.w(FileHandler.class, "#waitFor - Wait file dump interrupted");
            }
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
