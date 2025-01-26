/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.fileprocessor;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AuthAction;
import cube.common.action.ClientAction;
import cube.common.entity.AuthDomain;
import cube.common.entity.FileLabel;
import cube.common.state.AuthStateCode;
import cube.common.state.FileProcessorStateCode;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.util.HLSTools;
import cube.dispatcher.util.Tickable;
import cube.util.FileUtils;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 媒体文件管理器。
 */
public final class MediaFileManager implements Tickable {

    private final static MediaFileManager instance = new MediaFileManager();

    private String mediaPath;

    private Performer performer;

    private Map<String, AuthDomain> authDomainMap;

    /**
     * 上一次检测目录时间。
     */
    private long lastCheckPath = 0;

    /**
     * 检查目录周期，默认 4 小时。
     */
    private long checkPathPeriod = 4 * 60 * 60 * 1000;

    /**
     * 媒体目录超期时间，默认 7 天。
     */
    private long mediaPathTimeout = 7L * 24 * 60 * 60 * 1000;

    private MediaFileManager() {
        this.mediaPath = "cube-media-files/";
        File path = new File(this.mediaPath);
        if (!path.exists()) {
            path.mkdirs();
        }

        this.authDomainMap = new HashMap<>();
    }

    public final static MediaFileManager getInstance() {
        return MediaFileManager.instance;
    }

    public void check() {
        boolean enabled = HLSTools.checkEnabled();
        if (!enabled) {
            Logger.w(this.getClass(), "HLS tools is NOT enabled");
        }
        else {
            Logger.i(this.getClass(), "HLS tools is enabled");
        }
    }

    public void setPerformer(Performer performer) {
        this.performer = performer;
        this.performer.addTickable(this);
        this.lastCheckPath = System.currentTimeMillis();
    }

    /**
     * 罗列当前媒体数据的所有目录。
     *
     * @return
     */
    private File[] listAllMediaPath() {
        File path = new File(this.mediaPath);
        File[] files = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                if (path.isDirectory()) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        return files;
    }

    /**
     * 获取指定文件码媒体文件的回放流链接。
     *
     * @param domainName
     * @param fileCode
     * @param secure
     * @return
     */
    public String getMediaSourceURL(String domainName, String fileCode, boolean secure) {
        if (null == fileCode || fileCode.length() == 0) {
            return null;
        }

        AuthDomain authDomain = this.authDomainMap.get(domainName);
        if (null == authDomain) {
            authDomain = this.requestAuthDomain(domainName);
            if (null == authDomain) {
                return null;
            }

            this.authDomainMap.put(domainName, authDomain);
        }

        StringBuilder buf = new StringBuilder();
        if (secure) {
            buf.append("https://");
            buf.append(authDomain.getHttpsEndpoint().getHost());
            buf.append(":");
            buf.append(authDomain.getHttpsEndpoint().getPort());
        }
        else {
            buf.append("http://");
            buf.append(authDomain.getHttpEndpoint().getHost());
            buf.append(":");
            buf.append(authDomain.getHttpEndpoint().getPort());
        }
        buf.append("/file/media/");
        // 以文件码为路径
        buf.append(fileCode);
        buf.append("/");
        buf.append("hls.m3u8");

        return buf.toString();
    }

    public FileLabel getFile(String domainName, String fileCode) {
        ActionDialect requestDialect = new ActionDialect(ClientAction.GetFile.name);
        requestDialect.addParam("domain", domainName);
        requestDialect.addParam("fileCode", fileCode);

        ActionDialect dialect = this.performer.syncTransmit("Client", requestDialect);
        if (null == dialect) {
            return null;
        }

        int code = dialect.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getFile - Response code : " + code);
            return null;
        }

        JSONObject data = dialect.getParamAsJson("fileLabel");
        FileLabel fileLabel = new FileLabel(data);
        return fileLabel;
    }

    /**
     * 获取对应文件的 M3U8 文件。
     *
     * @param tokenCode
     * @param fileCode
     * @return
     */
    public File getM3U8File(String tokenCode, String fileCode) {
        File mediaFile = this.checkAndLoad(tokenCode, fileCode);

        File path = mediaFile.getParentFile();
        File m3u8File = new File(path, "media.m3u8");

        if (!m3u8File.exists()) {
            Logger.d(this.getClass(), "#getM3U8File - HLS : " + path.getAbsolutePath()
                    + " - " + mediaFile.getName() + " -> " + m3u8File.getName());
            // 没有发现 HLS 流文件，生成流文件
            boolean result = HLSTools.toHLS(path, mediaFile, m3u8File);
            if (!result) {
                return null;
            }
        }

        return m3u8File;
    }

    /**
     * 定位 TS 文件。
     *
     * @param fileCode
     * @param filename
     * @return
     */
    public File locateTransportStream(String fileCode, String filename) {
        return Paths.get(this.mediaPath, fileCode, filename).toFile();
    }

    private File checkAndLoad(String tokenCode, String fileCode) {
        AuthToken authToken = this.requestAuthToken(tokenCode);
        if (null == authToken) {
            Logger.w(this.getClass(), "Can NOT find auth token: " + tokenCode);
            return null;
        }

        FileLabel fileLabel = this.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "Can NOT find file label: " + fileCode);
            return null;
        }

        Path path = Paths.get(this.mediaPath, fileCode);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File localFile = new File(path.toFile(), "media." + fileLabel.getFileType().getPreferredExtension());
        if (!localFile.exists()) {
            InputStreamResponseListener listener = new InputStreamResponseListener();

            HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
            httpClient.newRequest(fileLabel.getDirectURL())
                    .header("Connection", "close")
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
                FileOutputStream fos = null;

                InputStream content = listener.getInputStream();

                byte[] bytes = new byte[4096];
                int length = 0;
                try {
                    fos = new FileOutputStream(localFile);

                    while ((length = content.read(bytes)) > 0) {
                        fos.write(bytes, 0, length);
                    }

                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            HttpClientFactory.getInstance().returnHttpClient(httpClient);
        }

        return localFile;
    }

    private AuthDomain requestAuthDomain(String domainName) {
        JSONObject data = new JSONObject();
        data.put("domain", domainName);

        Packet packet = new Packet(AuthAction.GetDomain.name, data);

        ActionDialect response = this.performer.syncTransmit("Auth", packet.toDialect());
        Packet responsePacket = new Packet(response);
        if (FileProcessorStateCode.Ok.code == Packet.extractCode(responsePacket)) {
            AuthDomain authDomain = new AuthDomain(Packet.extractDataPayload(responsePacket));
            return authDomain;
        }
        else {
            return null;
        }
    }

    private AuthToken requestAuthToken(String tokenCode) {
        AuthToken authToken = null;

        ActionDialect requestDialect = new ActionDialect(ClientAction.GetAuthToken.name);
        requestDialect.addParam("tokenCode", tokenCode);

        ActionDialect response = this.performer.syncTransmit("Client", requestDialect);
        if (null == response || response.getParamAsInt("code") != AuthStateCode.Ok.code) {
            return null;
        }

        authToken = new AuthToken(response.getParamAsJson("token"));
        return authToken;
    }

    @Override
    public void onTick(long now) {
        if (now - this.lastCheckPath > this.checkPathPeriod) {
            this.lastCheckPath = now;

            File[] files = listAllMediaPath();
            if (null != files && files.length > 0) {
                for (File path : files) {
                    if (now - path.lastModified() > this.mediaPathTimeout) {
                        // 清空目录
                        FileUtils.emptyPath(path);
                        // 删除空目录
                        path.delete();
                    }
                }
            }
        }
    }
}
