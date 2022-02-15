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

package cube.dispatcher.fileprocessor;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AuthAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.AuthDomain;
import cube.common.entity.FileLabel;
import cube.common.state.FileProcessorStateCode;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.filestorage.FileHandler;
import cube.dispatcher.filestorage.FileStorageCellet;
import cube.dispatcher.filestorage.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 媒体文件管理器。
 */
public final class MediaFileManager {

    private final static MediaFileManager instance = new MediaFileManager();

    private String mediaPath;

    private Performer performer;

    private Map<String, AuthDomain> authDomainMap;

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

    public void setPerformer(Performer performer) {
        this.performer = performer;
    }

    public String getMediaSourceURL(String domainName, String fileCode, boolean secure) {
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
        buf.append(fileCode);
        buf.append(".m3u8");

        return buf.toString();
    }

    public FileLabel getFile(String token, String fileCode) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("fileCode", fileCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Long sn = Utils.generateSerialNumber();
        Packet packet = new Packet(sn, FileStorageAction.GetFile.name, payload);
        ActionDialect packetDialect = packet.toDialect();

        ActionDialect dialect = this.performer.syncTransmit(token, FileStorageCellet.NAME, packetDialect);
        if (null == dialect) {
            return null;
        }

        Packet responsePacket = new Packet(dialect);

        int code = -1;
        JSONObject fileLabelJson = null;
        try {
            code = responsePacket.data.getInt("code");
            if (code != FileStorageStateCode.Ok.code) {
                return null;
            }

            fileLabelJson = responsePacket.data.getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);
        return fileLabel;
    }

    public void checkAndLoad(String token, String fileCode) {
        Path path = Paths.get(this.mediaPath, fileCode);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileLabel fileLabel = this.getFile(token, fileCode);

            InputStreamResponseListener listener = new InputStreamResponseListener();

            HttpClient httpClient = HttpClientFactory.getInstance().createHttpClient();
            httpClient.newRequest(fileLabel.getDirectURL())
                    .timeout(10, TimeUnit.SECONDS)
                    .send(listener);

            InputStream content = listener.getInputStream();
            byte[] bytes = new byte[4096];
//            while () {
//
//            }
        }


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
}
