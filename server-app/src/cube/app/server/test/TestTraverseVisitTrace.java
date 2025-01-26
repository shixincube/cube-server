/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.test;

import cell.util.Utils;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TestTraverseVisitTrace {

    public static void main(String[] args) {
        StringBuilder url = new StringBuilder("http://127.0.0.1:7777/file/traverse/trace/");
        url.append("?token=");
        url.append("dbSlHXBwBIcobOLHAgjxEvAvFXpdwlWA");
        url.append("&code=YaODOGiNhkhvhfEWAENYFoqbJWWkJwXT");

        JSONObject result = null;
        HttpClient client = new HttpClient();
        try {
            client.start();
            ContentResponse response = client.newRequest(url.toString())
                    .method(HttpMethod.GET)
                    .timeout(30, TimeUnit.SECONDS)
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
            System.out.println("Total: " + array.length());
            for (int i = 0; i <array.length(); ++i) {
                VisitTrace visitTrace = new VisitTrace(array.getJSONObject(i));
                System.out.println("Trace: " + visitTrace.platform + " - " + Utils.gsDateFormat.format(new Date(visitTrace.time)));
            }
        }
        else {
            System.out.println("Error");
        }
    }
}
