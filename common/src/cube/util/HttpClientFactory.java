/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
