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

import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.CacheCenter;
import cube.dispatcher.util.FileLabelHandler;
import cube.hub.dao.ChannelCode;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        ChannelCode channelCode = Helper.checkChannelCode(request, response, this.performer);
        if (null == channelCode) {
            this.complete();
            return;
        }

        String fileCode = request.getParameter("fc");
        if (null == fileCode) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            this.complete();
            return;
        }

        FileLabel fileLabel = CacheCenter.getInstance().getFileLabel(fileCode);
        if (null == fileLabel) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            this.complete();
            return;
        }

        try {
            if (fileLabel.getFileSize() <= this.getBufferSize()) {
                this.processByBlocking(request, response, fileLabel, fileLabel.getFileType());
            }
            else {
                this.processByNonBlocking(request, response, fileLabel, fileLabel.getFileType());
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "", e);
        } catch (ServletException e) {
            Logger.w(this.getClass(), "", e);
        }

        this.complete();
    }
}
