/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.app.server;

import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cube.app.server.container.ContainerManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;

/**
 * 程序入口。
 */
public class Main {

    /**
     * 启动服务器。
     *
     * @param port
     */
    public static void start(int port) {
        LogManager.getInstance().setLevel(LogLevel.DEBUG);
        LogManager.getInstance().addHandle(LogManager.createSystemOutHandle());

        ContainerManager containerManager = new ContainerManager();

        // 启动容器
        containerManager.launch(port);
    }

    /**
     * 停止服务器。
     *
     * @param port
     */
    public static void stop(int port) {
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
            ContentResponse response = httpClient.GET("http://127.0.0.1:" + port + "/stop/");
            if (HttpStatus.OK_200 == response.getStatus()) {
                System.out.println("Cube App Server - stop server # " + port);
            }
            else {
                System.out.println("Cube App Server - STOP FAILED # " + port);
            }
        } catch (Exception e) {
            System.err.println("Cube App Server - STOP FAILED # " + port + " - " + e.getMessage());
            //e.printStackTrace();
        } finally {
            try {
                httpClient.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("start")) {
                Main.start(7777);
            }
            else if (args[0].equalsIgnoreCase("stop")) {
                Main.stop(7777);
            }
        }
        else {
            System.out.println("Please input args");
        }
    }
}
