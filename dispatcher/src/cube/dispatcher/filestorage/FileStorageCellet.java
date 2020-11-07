/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cube.dispatcher.Performer;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * 文件存储模块的 Cellet 单元。
 */
public class FileStorageCellet extends Cellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "FileStorage";

    private FileChunkStorage fileChunkStorage;

    public FileStorageCellet() {
        super(NAME);
        this.fileChunkStorage = new FileChunkStorage("cube-fs-files");
    }

    @Override
    public boolean install() {
        Performer performer = (Performer) nucleus.getParameter("performer");

        // 打开存储管理器
        this.fileChunkStorage.open(performer);

        // 配置 HTTP/HTTPS 服务的句柄
        HttpServer httpServer = performer.getHttpServer();

        // 添加句柄
        ContextHandler upload = new ContextHandler();
        upload.setContextPath("/filestorage/file/");
        upload.setHandler(new FileHandler(this.fileChunkStorage));
        httpServer.addContextHandler(upload);

        return true;
    }

    @Override
    public void uninstall() {
        this.fileChunkStorage.close();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {

    }
}
