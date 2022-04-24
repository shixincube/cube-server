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

package cube.dispatcher.fileprocessor.handler;

import cube.auth.AuthToken;
import cube.dispatcher.Performer;
import cube.dispatcher.fileprocessor.MediaFileManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 获取媒体源。
 */
public class GetMediaSourceHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/file/source/";

    private Performer performer;

    public GetMediaSourceHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getParameter("t");
        if (null == token || token.length() == 0) {
            token = request.getParameter("token");
        }

        if (null == token || token.length() == 0) {
            // 参数错误
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        AuthToken authToken = this.performer.verifyToken(token);
        if (null == authToken) {
            // 令牌码无效
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            this.complete();
            return;
        }

        String domain = authToken.getDomain();
        String fileCode = request.getParameter("fc");

        String url = MediaFileManager.getInstance().getMediaSourceURL(domain, fileCode, isHttps());
        if (null == url) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            this.complete();
            return;
        }

        JSONObject data = new JSONObject();
        data.put("fileCode", fileCode);
        data.put("url", url);
        this.respondOk(response, data);

        this.complete();
    }
}
