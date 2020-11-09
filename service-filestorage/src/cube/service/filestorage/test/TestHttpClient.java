/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.filestorage.test;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;

import java.nio.ByteBuffer;

public class TestHttpClient {

    public static void main(String[] args) {

        HttpClient client = new HttpClient();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Request start");

        Object mutex = new Object();

        client.newRequest("http://127.0.0.1:6080/filestorage/xwwwwrLABQBGvNuxuvHFrzxRoxFAuIGzrxwwwwmNkRPMiKOxaPZJJZPaxOKiMPRk")
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
                        System.out.println("Size: " + buffer.limit());
                    }
                });

        synchronized (mutex) {
            try {
                mutex.wait(5000L);
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
