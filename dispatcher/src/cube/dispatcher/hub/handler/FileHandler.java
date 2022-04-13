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

package cube.dispatcher.hub.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.CacheCenter;
import cube.dispatcher.hub.Controller;
import cube.dispatcher.hub.HubCellet;
import cube.dispatcher.util.FileLabelHandler;
import cube.dispatcher.util.FormData;
import cube.hub.EventBuilder;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.data.DataHelper;
import cube.hub.event.Event;
import cube.hub.event.FileLabelEvent;
import cube.hub.signal.GetFileLabelSignal;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 文件处理。
 */
public class FileHandler extends FileLabelHandler {

    public final static String CONTEXT_PATH = "/hub/file/";

    private Performer performer;

    private Controller controller;

    public FileHandler(Performer performer, Controller controller) {
        super();
        this.performer = performer;
        this.controller = controller;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = request.getPathInfo();
        String code = path.substring(1).trim();

        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        File formFile = new File(CacheCenter.getInstance().getWorkPath().toFile(), code + ".form");
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(formFile);

            // 读取流
            byte[] bytes = new byte[4096];
            InputStream is = request.getInputStream();

            int length = 0;
            while ((length = is.read(bytes)) > 0) {
                fos.write(bytes, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                fos.close();
            }
        }

        try {
            CacheCenter.getInstance().lock(code);

            File file = new File(CacheCenter.getInstance().getWorkPath().toFile(), code);
            // 读取 Form 文件数据
            FormData formData = new FormData(formFile, file);
            String filename = formData.getFileName();
            if (null == filename || !file.exists() || file.length() == 0) {
                this.respond(response, HttpStatus.INTERNAL_SERVER_ERROR_500);
                this.complete();
                return;
            }

            // 修改文件名
            String extName = FileUtils.extractFileExtension(filename);
            File newFile = new File(CacheCenter.getInstance().getWorkPath().toFile(),
                    FileUtils.fastHash(code + filename) + (extName.length() > 0 ? "." + extName : ""));
            file.renameTo(newFile);

            // 生成文件码
            String fileCode = FileUtils.makeFileCode(code, DataHelper.DEFAULT_DOMAIN_NAME, newFile.getName());

            // 发送文件数据
            String[] hashCodes = this.performer.transmit(HubCellet.NAME, fileCode, new FileInputStream(newFile));

            // 判断文件类型
            FileType fileType = FileType.matchExtension(extName);

            // 生成文件标签
            FileLabel fileLabel = new FileLabel(DataHelper.DEFAULT_DOMAIN_NAME, fileCode,
                    DataHelper.DEFAULT_OWNER_ID, newFile);
            // 需要重置文件名
            fileLabel.setFileName(filename);
            fileLabel.setFileType(fileType);
            fileLabel.setMD5Code(hashCodes[0]);
            fileLabel.setSHA1Code(hashCodes[1]);

            // 设置有效期
            fileLabel.setExpiryTime(fileLabel.getTimestamp() + 24 * 60 * 60 * 1000);

            // 放置文件
            ActionDialect actionDialect = new ActionDialect(HubAction.PutFile.name);
            actionDialect.addParam("fileLabel", fileLabel.toJSON());
            ActionDialect result = this.performer.syncTransmit(HubCellet.NAME, actionDialect, 60 * 1000);
            if (result.containsParam("event")) {
                Event event = EventBuilder.build(result.getParamAsJson("event"));
                FileLabelEvent fileLabelEvent = (FileLabelEvent) event;
                FileLabel resultFileLabel = fileLabelEvent.getFileLabel();
                // 将文件进行缓存
                file = CacheCenter.getInstance().putFileLabel(resultFileLabel);
                newFile.renameTo(file);

                this.respondOk(response, resultFileLabel.toCompactJSON());
                this.complete();
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            formFile.delete();
            CacheCenter.getInstance().unlock(code);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        String[] path = pathInfo.substring(1).trim().split("/");
        if (path.length != 2) {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        String code = path[0];
        String fileCode = path[1];

        ChannelCode channelCode = Helper.checkChannelCode(code, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        // 尝试从缓存里读取文件
        CacheCenter.CachedFile cachedFile = CacheCenter.getInstance().tryGetFile(fileCode);
        if (null != cachedFile) {
            // 填充响应头
            this.fillHeaders(response, cachedFile.fileLabel, cachedFile.file.length(),
                    cachedFile.fileLabel.getFileType());

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(cachedFile.file);
                ServletOutputStream os = response.getOutputStream();
                byte[] data = new byte[4096];
                int length = 0;
                while ((length = fis.read(data)) > 0) {
                    os.write(data, 0, length);
                }
            } catch (IOException e) {
                Logger.e(this.getClass(), "#doGet", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }

            response.setStatus(HttpStatus.OK_200);
            this.complete();
            return;
        }

        FileLabel fileLabel = null;
        File outputFile = null;

        // 没有缓存，从服务节点获取
        GetFileLabelSignal requestSignal = new GetFileLabelSignal(channelCode.code, fileCode);
        ActionDialect actionDialect = new ActionDialect(HubAction.Channel.name);
        actionDialect.addParam("signal", requestSignal.toJSON());
        ActionDialect result = this.performer.syncTransmit(HubCellet.NAME, actionDialect);
        if (null == result) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        int stateCode = result.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.w(this.getClass(), "#doGet - state : " + stateCode);
            JSONObject data = new JSONObject();
            data.put("code", stateCode);
            this.respond(response, HttpStatus.UNAUTHORIZED_401, data);
            this.complete();
            return;
        }

        if (result.containsParam("event")) {
            Event event = EventBuilder.build(result.getParamAsJson("event"));
            if (event instanceof FileLabelEvent) {
                fileLabel = event.getFileLabel();
                // 加入新标签，返回缓存文件名
                outputFile = CacheCenter.getInstance().putFileLabel(fileLabel);
            }
            else {
                JSONObject data = new JSONObject();
                data.put("code", HubStateCode.ControllerError.code);
                this.respond(response, HttpStatus.NOT_FOUND_404, data);
                this.complete();
                return;
            }
        }
        else {
            JSONObject data = new JSONObject();
            data.put("code", HubStateCode.Failure.code);
            this.respond(response, HttpStatus.NOT_FOUND_404, data);
            this.complete();
            return;
        }

        try {
            if (fileLabel.getFileSize() <= this.getBufferSize()) {
                this.processByBlocking(request, response, fileLabel, fileLabel.getFileType(), outputFile);
            }
            else {
                this.processByNonBlocking(request, response, fileLabel, fileLabel.getFileType(), outputFile);
            }
        } catch (IOException | ServletException e) {
            Logger.w(this.getClass(), "", e);
        }

        this.complete();
    }
}
