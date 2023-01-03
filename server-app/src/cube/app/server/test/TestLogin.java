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

package cube.app.server.test;

import cell.util.log.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class TestLogin {

    public static void main(String[] args) {
        String url = "https://172.16.6.100:8140/account/login/";
        JSONObject data = new JSONObject();
        data.put("account", "cube1");
        data.put("password", "c7af98d321febe62e04d45e8806852e0");
        data.put("device", "Web/Mozilla/5.0+(Macintosh;+Intel+Mac+OS+X+10.15;+rv:103.0)+Gecko/20100101+Firefox/103.0");
        data.put("remember", true);

        StringContentProvider provider = new StringContentProvider(data.toString());

        JSONObject result = null;

        SslContextFactory sslContextFactory = new SslContextFactory.Client(true);
        HttpClient client = new HttpClient(sslContextFactory);
        try {
            client.start();
            ContentResponse response = client.newRequest(url)
                    .method(HttpMethod.POST)
                    .timeout(15, TimeUnit.SECONDS)
                    .content(provider)
                    .send();

            if (HttpStatus.OK_200 == response.getStatus()) {
                HttpFields fields = response.getHeaders();
                for (int i = 0; i < fields.size(); ++i) {
                    HttpField field = fields.getField(i);
                    System.out.println("H: " + field.getName() + ":" + field.getValue());
                }

                result = new JSONObject(response.getContentAsString().trim());
                System.out.println("Response: " + result.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
            }
        }
    }
}
