/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
