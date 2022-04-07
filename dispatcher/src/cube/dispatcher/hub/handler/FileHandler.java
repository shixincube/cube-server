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
import cube.dispatcher.hub.HubCellet;
import cube.dispatcher.util.FileLabelHandler;
import cube.hub.EventBuilder;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.data.ChannelCode;
import cube.hub.event.Event;
import cube.hub.event.FileLabelEvent;
import cube.hub.signal.GetFileLabelSignal;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 文件处理。
 */
public class FileHandler extends FileLabelHandler {

    public final static String CONTEXT_PATH = "/hub/file/";

    private Performer performer;

    public FileHandler(Performer performer) {
        super();
        this.performer = performer;
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
