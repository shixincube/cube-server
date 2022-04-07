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

package cube.dispatcher.util;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.util.CrossDomainHandler;
import cube.util.FileType;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 文件标签句柄。
 */
public class FileLabelHandler extends CrossDomainHandler {

    private int bufferSize = 5 * 1024 * 1024;

    public FileLabelHandler() {
        super();
    }

    protected int getBufferSize() {
        return this.bufferSize;
    }

    protected void processByBlocking(HttpServletRequest request, HttpServletResponse response,
                                   FileLabel fileLabel, FileType type, File file)
            throws IOException, ServletException {
        final Object mutex = new Object();

        final FlexibleByteBuffer buf = new FlexibleByteBuffer((int)fileLabel.getFileSize());

        HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
        httpClient.newRequest(fileLabel.getDirectURL())
                .send(new BufferingResponseListener(this.bufferSize) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            byte[] responseContent = getContent();
                            buf.put(responseContent);
                        }

                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                });

        synchronized (mutex) {
            try {
                mutex.wait(10 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        buf.flip();

        if (null != file) {
            // 写入文件
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(buf.array(), 0, buf.limit());
            } catch (IOException e) {
                Logger.e(this.getClass(), "#processByBlocking", e);
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (Exception e) {
                        // Nothing
                    }
                }
            }
        }

        // 填写头信息
        this.fillHeaders(response, fileLabel, buf.limit(), type);

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(buf.array(), 0, buf.limit());

        buf.clear();

        response.setStatus(HttpStatus.OK_200);

        HttpClientFactory.getInstance().returnHttpClient(httpClient);

        this.complete();
    }

    protected void processByNonBlocking(HttpServletRequest request, HttpServletResponse response,
                                      FileLabel fileLabel, FileType type, File file)
            throws IOException, ServletException {
        InputStreamResponseListener listener = new InputStreamResponseListener();

        HttpClient httpClient = HttpClientFactory.getInstance().borrowHttpClient();
        httpClient.newRequest(fileLabel.getDirectURL())
                .timeout(10, TimeUnit.SECONDS)
                .send(listener);

        Response clientResponse = null;
        try {
            clientResponse = listener.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (null != clientResponse && clientResponse.getStatus() == HttpStatus.OK_200) {
            InputStream content = listener.getInputStream();

            // Async output
            AsyncContext async = request.startAsync();
            ServletOutputStream output = response.getOutputStream();
            StandardDataStream dataStream = new StandardDataStream(content, async, output, file);
            async.addListener(new AsyncListener() {
                @Override
                public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onStartAsync");
                }

                @Override
                public void onComplete(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onComplete");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }

                @Override
                public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onTimeout");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }

                @Override
                public void onError(AsyncEvent asyncEvent) throws IOException {
                    Logger.d(this.getClass(), "onError");
                    HttpClientFactory.getInstance().returnHttpClient(httpClient);
                    complete();
                }
            });

            // 设置数据写入监听器
            output.setWriteListener(dataStream);

            // 填充 Header
            fillHeaders(response, fileLabel, fileLabel.getFileSize(), type);
            response.setStatus(HttpStatus.OK_200);
        }
        else {
            this.respond(response, HttpStatus.BAD_REQUEST_400, fileLabel.toCompactJSON());
            HttpClientFactory.getInstance().returnHttpClient(httpClient);
        }
    }

    protected void fillHeaders(HttpServletResponse response, FileLabel fileLabel, long length, FileType type) {
        if (FileType.FILE == type) {
            try {
                StringBuilder buf = new StringBuilder("attachment;");
                buf.append("filename=").append(URLEncoder.encode(fileLabel.getFileName(), "UTF-8"));
                response.setHeader("Content-Disposition", buf.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            response.setContentType(type.getMimeType());
        }
        else if (FileType.UNKNOWN == type) {
            response.setContentType(fileLabel.getFileType().getMimeType());
        }
        else if (null != type) {
            response.setContentType(type.getMimeType());
        }
        else {
            response.setContentType(fileLabel.getFileType().getMimeType());
        }

        if (length > 0) {
            response.setContentLengthLong(length);
        }
    }

    /**
     * 标准数据流输出。
     */
    private final class StandardDataStream implements WriteListener {

        private final InputStream content;
        private final AsyncContext async;
        private final ServletOutputStream output;
        private FileOutputStream fileOutputStream;

        protected long contentLength = 0;

        private StandardDataStream(InputStream content, AsyncContext async,
                                   ServletOutputStream output, File file) {
            this.content = content;
            this.async = async;
            this.output = output;

            if (null != file) {
                try {
                    this.fileOutputStream = new FileOutputStream(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onWritePossible() throws IOException {
            byte[] buffer = new byte[4096];

            // 输出流是否就绪
            while (this.output.isReady()) {
                int len = this.content.read(buffer);
                if (len < 0) {
                    this.async.complete();

                    try {
                        this.content.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (null != this.fileOutputStream) {
                        try {
                            this.fileOutputStream.close();
                        } catch (IOException e) {
                            // Nothing
                        }
                    }

                    return;
                }

                // 将数据写入输出流
                this.output.write(buffer, 0, len);
                this.contentLength += len;

                if (null != this.fileOutputStream) {
                    try {
                        this.fileOutputStream.write(buffer, 0, len);
                    } catch (IOException e) {
                        Logger.w(this.getClass(), "#onWritePossible", e);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Logger.w(this.getClass(), "Async Error", throwable);
            this.async.complete();

            try {
                this.content.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null != this.fileOutputStream) {
                try {
                    this.fileOutputStream.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }
}
