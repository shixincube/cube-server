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

package cube.app.server.test;

import cube.common.entity.TraceEvent;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class TestTrace {

    public static void main(String[] args) {
        StringBuilder url = new StringBuilder("http://127.0.0.1:7777/trace/sharing/applet/wechat");

        JSONObject data = new JSONObject();
        data.put("domain", "192.168.1.121");
        data.put("url", "http://192.168.0.110:7010/sharing/YaODOGiNhkhvhfEWAENYFoqbJWWkJwXT?s=a92929293929293092&p=9691919192919192091");
        data.put("title", "Test-HIT-CIR_Sharing_Protocol.doc");

        JSONObject screen = new JSONObject("{\"orientation\":\"landscape-primary\",\"width\":1680,\"colorDepth\":30,\"height\":1050}");
        data.put("screen", screen);

        data.put("language", "zh-CN");

        JSONObject agent = new JSONObject();
        agent.put("SDKVersion", "2.25.0");
        agent.put("deviceModel", "iPhone X");
        data.put("agent", agent);

        data.put("event", TraceEvent.View);

        StringContentProvider provider = new StringContentProvider(data.toString());

        JSONObject result = null;

        HttpClient client = new HttpClient();
        try {
            client.start();
            ContentResponse response = client.newRequest(url.toString())
                    .method(HttpMethod.POST)
                    .timeout(5, TimeUnit.SECONDS)
                    .content(provider)
                    .send();

            if (HttpStatus.OK_200 == response.getStatus()) {
                result = new JSONObject(response.getContentAsString().trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
            }
        }

        if (null != result) {
            System.out.println("OK");
        }
        else {
            System.out.println("Error");
        }
    }
}