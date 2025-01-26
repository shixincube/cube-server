/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.test;

import cube.common.entity.SharingTag;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class TestListSharingTags {

    public static void main(String[] args) {
        StringBuilder url = new StringBuilder("http://127.0.0.1:7777/file/list/sharing/");
        url.append("?token=");
        url.append("dbSlHXBwBIcobOLHAgjxEvAvFXpdwlWA");
        url.append("&begin=0");
        url.append("&end=19");

        JSONObject result = null;
        HttpClient client = new HttpClient();
        try {
            client.start();
            ContentResponse response = client.newRequest(url.toString())
                    .method(HttpMethod.GET)
                    .timeout(15, TimeUnit.SECONDS)
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
            JSONArray array = result.getJSONArray("list");
            System.out.println("number: " + array.length());
            for (int i = 0; i <array.length(); ++i) {
                SharingTag tag = new SharingTag(array.getJSONObject(i));
                System.out.println("ST: " + tag.getConfig().getFileLabel().getFileName());
            }
        }
        else {
            System.out.println("Error");
        }
    }
}
