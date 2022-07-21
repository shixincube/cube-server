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
