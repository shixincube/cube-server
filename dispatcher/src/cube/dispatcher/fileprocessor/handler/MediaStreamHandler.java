/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.fileprocessor.handler;

import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.fileprocessor.MediaFileManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 媒体列表句柄。
 */
public class MediaStreamHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/file/media/";

    private final String m3u8MIME = "application/x-mpegURL";

    private final String tsMIME = "video/MP2T";

    private Performer performer;

    public MediaStreamHandler(Performer performer) {
        this.performer = performer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String uri = request.getRequestURI();
        String[] path = uri.split("/");
        String fileCode = path[path.length - 2];
        String target = path[path.length - 1];

        if (target.endsWith(".ts")) {
            // 设置 ContentType
            response.setContentType(tsMIME);

            // 读取流文件
            File file = MediaFileManager.getInstance().locateTransportStream(fileCode, target);

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[4096];
                int length = 0;
                while ((length = fis.read(bytes)) > 0) {
                    response.getOutputStream().write(bytes, 0, length);
                }
            } catch (IOException e) {
                Logger.w(this.getClass(), "#doGet", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }
        }
        else {
            // 设置 ContentType
            response.setContentType(m3u8MIME);

            String token = request.getParameter("t");
            if (null == token || token.length() == 0) {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            // 获取指定文件的 M3U8 清单
            File m3u8File = MediaFileManager.getInstance().getM3U8File(token, fileCode);
            if (null == m3u8File) {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(m3u8File);
                byte[] bytes = new byte[4096];
                int length = 0;
                while ((length = fis.read(bytes)) > 0) {
                    response.getOutputStream().write(bytes, 0, length);
                }
            } catch (IOException e) {
                Logger.w(this.getClass(), "#doGet", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }
        }

        this.complete();
    }
}
