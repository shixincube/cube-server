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

package cube.service.robot;

import cube.robot.Account;
import cube.robot.Task;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RoboengineImpl implements Roboengine {

    private HttpClient client;

    private String host;
    private int port;

    private String token;

    public RoboengineImpl() {
        this.client = new HttpClient();
    }

    public void start(String host, int port, String token) {
        this.host = host;
        this.port = port;
        this.token = token;

        try {
            this.client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            this.client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Task getTask(String taskName) {
        String codedName = null;
        try {
            codedName = URLEncoder.encode(taskName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        String url = "http://" + this.host + ":" + this.port + "/task/get/" + this.token
                + "/?name=" + codedName;

        Task task = null;

        try {
            ContentResponse response = this.client.GET(url);
            if (response.getStatus() != HttpStatus.OK_200) {
                return null;
            }

            JSONObject data = new JSONObject(response.getContentAsString());
            task = new Task(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        return task;
    }

    @Override
    public List<Account> getOnlineAccounts() {
        String url = "http://" + this.host + ":" + this.port + "/account/online/" + this.token;

        List<Account> list = new ArrayList<>();
        try {
            ContentResponse response = this.client.GET(url);
            if (response.getStatus() != HttpStatus.OK_200) {
                return null;
            }

            JSONArray array = new JSONArray(response.getContentAsString());
            for (int i = 0; i < array.length(); ++i) {
                JSONObject item = array.getJSONObject(i);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        return list;
    }
}
