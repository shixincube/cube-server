/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 验证码图片。
 */
public class CaptchaHandler extends ContextHandler {

    public CaptchaHandler(String httpOrigin, String httpsOrigin) {
        super("/captcha/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {
        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            File file = AccountManager.getInstance().consumeCaptcha();
            if (null == file) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            response.setStatus(HttpStatus.OK_200);
            response.setContentType(FileType.JPEG.getMimeType());
            response.setContentLengthLong(file.length());

            FileInputStream fis = null;
            ServletOutputStream os = response.getOutputStream();

            try {
                fis = new FileInputStream(file);

                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = fis.read(bytes)) > 0) {
                    os.write(bytes, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    fis.close();
                }

                os.close();
            }

            // 删除文件
            file.delete();

            this.complete();
        }
    }
}
