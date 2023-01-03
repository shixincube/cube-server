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

package cube.util;

import org.eclipse.jetty.client.HttpClient;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * HTTP 客户端工厂。
 */
public final class HttpClientFactory {

    private final static HttpClientFactory instance = new HttpClientFactory();

    private ConcurrentLinkedQueue<HttpClient> queue;

    private HttpClientFactory() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public static HttpClientFactory getInstance() {
        return HttpClientFactory.instance;
    }

    public HttpClient borrowHttpClient() {
        HttpClient client = this.queue.poll();
        if (null != client) {
            if (!client.isStarted()) {
                try {
                    client.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return client;
        }

        client = new HttpClient();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client;
    }

    public void returnHttpClient(HttpClient client) {
        this.queue.offer(client);
    }

    public void close() {
        Iterator<HttpClient> iter = this.queue.iterator();
        while (iter.hasNext()) {
            try {
                iter.next().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.queue.clear();
    }
}
