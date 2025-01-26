/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
