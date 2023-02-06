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

package cube.dispatcher.robot;

import cell.util.log.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 接口管理器。
 */
public class Manager {

    private final static Manager instance = new Manager();

    private HttpClient client;

    private List<String> callbackList;

    private Manager() {
        this.callbackList = new ArrayList<>();
    }

    public static Manager getInstance() {
        return Manager.instance;
    }

    public void start() {
        this.client = new HttpClient();

        try {
            this.client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (null != this.client) {
            try {
                this.client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerCallback(String url) {
        synchronized (this.callbackList) {
            if (this.callbackList.contains(url)) {
                return;
            }

            this.callbackList.add(url);
        }
    }

    public void deregisterCallback(String url) {
        synchronized (this.callbackList) {
            this.callbackList.remove(url);
        }
    }

    public void callback(String name, JSONObject data) {
        synchronized (this.callbackList) {
            for (String url : this.callbackList) {
                JSONObject event = new JSONObject();
                event.put("name", name);
                event.put("data", data);

                StringContentProvider provider = new StringContentProvider(event.toString());
                try {
                    ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.SECONDS).send();
                    if (response.getStatus() != HttpStatus.OK_200) {
                        if (Logger.isDebugLevel()) {
                            Logger.d(this.getClass(), "Callback \"" + url + "\" failed");
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
