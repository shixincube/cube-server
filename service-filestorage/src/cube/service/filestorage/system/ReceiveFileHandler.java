/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.filestorage.system;

import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.FileUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 接收文件。
 */
public class ReceiveFileHandler extends ContextHandler {

    private FileStorageService service;

    public ReceiveFileHandler() {
        super("/files/receive/");
        setHandler(new Handler());
    }

    public void activate(FileStorageService service) {
        this.service = service;
    }

    private class Handler extends CrossDomainHandler {

        public Handler() {
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            if (null == service) {
                this.respond(response, HttpStatus.INTERNAL_SERVER_ERROR_500);
                this.complete();
                return;
            }

            // Token Code
            String token = request.getParameter("token");
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
            }

            Contact contact = ContactManager.getInstance().getContact(token);
            if (null == contact) {
                Logger.w(ReceiveFileHandler.class, "#doPost - Token is invalid: " + token);
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            String contentType = request.getHeader(HttpHeader.CONTENT_TYPE.asString()).toLowerCase();
            if (contentType.contains("multipart/form-data")) {
                // 客户端以 Form 数据格式上传
                // TODO
                Logger.e(ReceiveFileHandler.class, "TODO Form");
                this.respond(response, HttpStatus.NOT_IMPLEMENTED_501);
                this.complete();
            }
            else if (contentType.contains("binary") || contentType.contains("application/octet-stream")) {
                // 客户端以二进制流形式上传
                // 文件名
                String filename = request.getParameter("filename");
                if (null == filename) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                String fileCode = FileUtils.makeFileCode(contact.getId(), contact.getDomain().getName(), filename);
                // 写入文件数据
                File file = service.writeFile(fileCode, request.getInputStream());
                if (null == file) {
                    Logger.e(ReceiveFileHandler.class, "#doPost - Write file failed: " + filename);
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                FileLabel fileLabel = FileUtils.makeFileLabel(contact.getDomain().getName(), fileCode, contact.getId(), file);
                // 修正文件类型
                fileLabel.setFileType(FileType.matchExtension(FileUtils.extractFileExtension(filename)));
                // 推文件到存储服务
                FileLabel newFileLabel = service.putFile(fileLabel);
                if (null == newFileLabel) {
                    Logger.e(ReceiveFileHandler.class, "#doPost - Put file failed: " + filename);
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                this.respondOk(response, newFileLabel.toJSON());
                this.complete();
            }
            else {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
            }
        }
    }
}