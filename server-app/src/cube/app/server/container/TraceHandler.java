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

package cube.app.server.container;

import cell.util.log.Logger;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 访问痕迹记录。
 */
public class TraceHandler extends ContextHandler {

    private String address;

    private int port = 7010;

    private ConcurrentLinkedQueue<HttpClient> clientQueue;

    public TraceHandler(String httpOrigin, String httpsOrigin, JSONObject config) {
        super("/trace/");
        this.address = config.getString("address");
        setHandler(new Handler(httpOrigin, httpsOrigin));
        this.clientQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    protected void stopContext() throws Exception {
        super.stopContext();

        while (!this.clientQueue.isEmpty()) {
            HttpClient client = this.clientQueue.poll();
            try {
                client.stop();
            } catch (Exception e) {
                // Nothing
            }
        }
    }

    protected class Handler extends CrossDomainHandler {

        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String path = request.getPathInfo();
            if (path.indexOf("/sharing/applet/wechat") == 0) {
                JSONObject data = this.readBodyAsJSONObject(request);

                // 设置访问地址
                data.put("address", request.getRemoteHost());

                StringBuilder url = new StringBuilder("http://");
                url.append(address);
                url.append(":");
                url.append(port);
                url.append("/sharing/trace/applet/wechat/");

                HttpClient client = borrowClient();

                try {
                    StringContentProvider provider = new StringContentProvider(data.toString());

                    ContentResponse result = client.POST(url.toString()).content(provider).timeout(5, TimeUnit.SECONDS).send();
                    if (result.getStatus() == HttpStatus.OK_200) {
                        if (Logger.isDebugLevel()) {
                            Logger.d(TraceHandler.class, "Post trace ok");
                        }
                    }
                    else {
                        Logger.w(TraceHandler.class, "Post trace error: " + result.getStatus());
                    }
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#doPost", e);
                } finally {
                    returnClient(client);
                }

                JSONObject responseData = new JSONObject();
                responseData.put("time", System.currentTimeMillis());

                this.respond(response, HttpStatus.OK_200, responseData);
                this.complete();
            }
            else {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        }
    }

    private HttpClient borrowClient() {
        HttpClient client = this.clientQueue.poll();
        if (null != client) {
            return client;
        }

        SslContextFactory factory = new SslContextFactory.Client(true);
        client = new HttpClient(factory);
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client;
    }

    private void returnClient(HttpClient client) {
        this.clientQueue.offer(client);
    }
}
