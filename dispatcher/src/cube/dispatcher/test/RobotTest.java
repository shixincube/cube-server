/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.test;

import cube.robot.Account;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Robot 测试。
 */
public class RobotTest {

    private HttpClient client;

    private String host = "127.0.0.1";
    private int port = 7010;
    private String code = "vGbtTRjwtcYFAHxPqgAeeSpuZxVuPLgE";

    protected RobotTest() {
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

    public void testGetOnlineList() {
        String url = this.getUrl("/robot/online/");

        System.out.println("GET - " + url);

        try {
            ContentResponse response = this.client.GET(url);
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                JSONArray list = data.getJSONArray("list");
                System.out.println("Total: " + data.getInt("total") + "/" + list.length());
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

    public void testGetAccountList() {
        String url = this.getUrl("/robot/account/list/");
        url = url + "?begin=0&end=19";

        System.out.println("GET - " + url);

        try {
            ContentResponse response = this.client.GET(url);
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                int begin = data.getInt("begin");
                int end = data.getInt("end");
                JSONArray list = data.getJSONArray("list");
                System.out.println("Position : " + begin + "-" + end + ", total: " + list.length());
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

    public void testGetAccount() {
        String url = this.getUrl("/robot/account/");
        url = url + "/?id=7";

        System.out.println("GET - " + url);

        try {
            ContentResponse response = this.client.GET(url);
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                Account account = new Account(data);
                System.out.println("Account:\n" + account.toJSON().toString(4));
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
        RobotTest test = new RobotTest();
        test.setup();

//        test.testGetOnlineList();

        test.testGetAccountList();

//        test.testGetAccount();

        test.teardown();
    }
}
