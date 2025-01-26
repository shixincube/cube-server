/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.test;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class TestHttpClient {

    public static void main(String[] args) {

        HttpClient client = new HttpClient();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Request start");

        AtomicLong length = new AtomicLong(0L);

        Object mutex = new Object();

        String url1 = "http://127.0.0.1:6080/filestorage/xwwwwrLABQBGvNuxuvHFrzxRoxFAuIGzrxwwwwmNkRPMiKOxaPZJJZPaxOKiMPRk";

        String url2 = "http://127.0.0.1:7010/filestorage/file/?file=xwwwwrLABQBGvNuxuvHFrzxRoxFAuIGzrxwwwwmNkRPMiKOxaPZJJZPaxOKiMPRk&token=KHQMIUBjZLHanDVYnIYsIscnMemkUjHT&sn=1604923264097";

        client.newRequest(url2)
                .method("GET")
                .onComplete(new Response.CompleteListener() {
                    @Override
                    public void onComplete(Result result) {
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                })
                .send(new Response.Listener.Adapter() {
                    @Override
                    public void onContent(Response serverResponse, ByteBuffer buffer) {
                        length.addAndGet(buffer.limit());
                        System.out.println("Size: " + length.get());
                    }
                });

        synchronized (mutex) {
            try {
                mutex.wait(30000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Request stop");

        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
