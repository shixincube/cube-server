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

import cell.util.collection.FlexibleByteBuffer;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Packet;
import cube.common.action.FileStorageActions;
import cube.common.state.FileStorageStateCode;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件上传处理器。
 */
public class FileUploadHandler extends CrossDomainHandler {

    private FileChunkStorage fileChunkStorage;

    public FileUploadHandler(FileChunkStorage fileChunkStorage) {
        super();
        this.fileChunkStorage = fileChunkStorage;
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.getMethod();
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        byte[] bytes = new byte[4096];
        InputStream is = request.getInputStream();

        int length = 0;
        while ((length = is.read(bytes)) > 0) {
            buf.put(bytes, 0, length);
        }

        // 整理缓存
        buf.flip();

        FormData formData = new FormData(buf.array());

        buf = null;

        // Contact ID
        Long contact = null;
        // Token Code
        String token = null;
        // 文件大小
        long fileSize = 0;
        // 文件块所处的索引位置
        int cursor = 0;
        // 文件块大小
        int size = 0;
        // 文件名
        String fileName = null;
        // 文件块数据
        byte[] data = null;

        try {
            contact = Long.parseLong(formData.getValue("cid"));
            token = formData.getValue("token");
            fileSize = Long.parseLong(formData.getValue("fileSize"));
            cursor = Integer.parseInt(formData.getValue("cursor"));
            size = Integer.parseInt(formData.getValue("size"));
            fileName = formData.getFileName();
            data = formData.getFileChunk();
        } catch (Exception e) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            return;
        }

        FileChunk chunk = new FileChunk(contact, token, fileName, fileSize, cursor, size, data);
        this.fileChunkStorage.append(contact, chunk);

        JSONObject responseData = new JSONObject();
        try {
            responseData.put("fileName", fileName);
            responseData.put("cursor", cursor);
            responseData.put("size", size);
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
        Packet packet = new Packet(FileStorageActions.UploadFile.name, payload);

        this.respondOk(response, packet.toJSON());
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        this.doPut(request, response);
    }
}
