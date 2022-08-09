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

package cube.util;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 服务器。
 */
public class HttpServer {

    private Server server;

    private int plainPort;

    private int securePort;

    private Path keystorePath;

    private String keyStorePassword;

    private String keyManagerPassword;

    private List<ContextHandler> handlers;

    private HandlerList handlerList;

    public HttpServer() {
        this.handlers = new ArrayList<>();
    }

    public void setKeystorePath(String path) throws FileNotFoundException {
        Path keystorePath = Paths.get(path).toAbsolutePath();
        if (!Files.exists(keystorePath)) {
            throw new FileNotFoundException(keystorePath.toString());
        }

        this.keystorePath = keystorePath;
    }

    public void setKeystorePassword(String keyStorePassword, String keyManagerPassword) {
        this.keyStorePassword = keyStorePassword;
        this.keyManagerPassword = keyManagerPassword;
    }

    public void setPort(int plainPort, int securePort) {
        this.plainPort = plainPort;
        this.securePort = securePort;
    }

    public void addContextHandler(ContextHandler handler) {
        this.handlers.add(handler);
    }

    public void setHandler(HandlerList handlerList) {
        this.handlerList = handlerList;
    }

    /**
     * 启动服务器。
     */
    public void start() {
        this.start(this.plainPort, this.securePort);
    }

    /**
     * 启动服务。
     *
     * @param joinThread
     */
    public void start(boolean joinThread) {
        this.start(this.plainPort, this.securePort, joinThread);
    }

    /**
     * 启动服务器。
     *
     * @param plainPort
     */
    public void start(int plainPort) {
        this.plainPort = plainPort;
        this.start(plainPort, 0);
    }

    /**
     * 启动服务器。
     *
     * @param plainPort
     * @param securePort
     */
    public void start(int plainPort, int securePort) {
        this.start(plainPort, securePort, false);
    }

    /**
     * 启动服务器。
     *
     * @param plainPort
     * @param securePort
     * @param joinThread
     */
    public void start(int plainPort, int securePort, boolean joinThread) {
        if (null != this.server) {
            return;
        }

        this.plainPort = plainPort;
        this.securePort = securePort;

        this.server = new Server();

        ServerConnector https = null;

        HttpConfiguration httpConfig = new HttpConfiguration();

        if (securePort > 0 && null != this.keystorePath && null != this.keyStorePassword) {
            // 启用 SSL 支持
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(securePort);
            httpConfig.setOutputBufferSize(32768);

            SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(this.keystorePath.toString());
            sslContextFactory.setKeyStorePassword(this.keyStorePassword);
            sslContextFactory.setKeyManagerPassword(this.keyManagerPassword);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            SecureRequestCustomizer src = new SecureRequestCustomizer();
            src.setStsMaxAge(5000);
            src.setStsIncludeSubDomains(true);
            httpsConfig.addCustomizer(src);

            https = new ServerConnector(this.server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            https.setPort(securePort);
            https.setIdleTimeout(500000);
        }
        else {
            httpConfig.setOutputBufferSize(32768);
        }

        ServerConnector http = new ServerConnector(this.server, new HttpConnectionFactory(httpConfig));
        http.setPort(plainPort);
        http.setIdleTimeout(30000);

        if (null != https) {
            this.server.setConnectors(new Connector[]{ http, https });
        }
        else {
            this.server.setConnectors(new Connector[]{ http });
        }

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        for (ContextHandler handler : this.handlers) {
            contexts.addHandler(handler);
        }

        // 设置处理句柄
        this.server.setHandler(contexts);

        if (null != this.handlerList) {
            this.server.setHandler(this.handlerList);
        }

        if (joinThread) {
            try {
                this.server.start();
                this.server.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            (new Thread() {
                @Override
                public void run() {
                    try {
                        server.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * 停止服务器，并退出线程。
     */
    public void stop() {
        if (null == this.server) {
            return;
        }

        try {
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.server = null;
    }

    public int getPlainPort() {
        return this.plainPort;
    }

    public int getSecurePort() {
        return this.securePort;
    }
}
