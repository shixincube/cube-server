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

package cube.dispatcher.test;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AIGC 测试。
 */
public class AIGCTest {

    private HttpClient client;

    private String host = "127.0.0.1";
    private int port = 7010;
    private String code = "rkMINEsGlcdUXZCdDQXihdppQiokYmVF";

    protected AIGCTest() {
        this.client = new HttpClient();
    }

    public void setup() {
        try {
            this.client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void teardown() {
        try {
            this.client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBaseUrl() {
        return "http://" + this.host + ":" + this.port;
    }

    private String getUrl(String contextPath) {
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (!contextPath.endsWith("/")) {
            contextPath = contextPath + "/";
        }
        return this.getBaseUrl() + contextPath + this.code;
    }

    public void testRequestChannel() {
        String url = this.getUrl("/aigc/channel/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("participant", "来自火星的星星");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public void testChat() {
        String url = this.getUrl("/aigc/chat/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("code", "ftisfHxrdyxFIgGF");
            request.put("content", "大理旅游体验好吗");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public void testSentiment() {
        String url = this.getUrl("/aigc/nlp/sentiment/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("text", "天气不好呀");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public static void main(String[] args) {
        AIGCTest test = new AIGCTest();
        test.setup();

//        test.testRequestChannel();
//        test.testChat();

        test.testSentiment();

        test.teardown();
    }
}
