/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
