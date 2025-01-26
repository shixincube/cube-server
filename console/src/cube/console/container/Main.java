/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container;

import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.console.Console;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;

public class Main {

    public Main() {
    }

    /**
     * 启动容器。
     *
     * @param port
     */
    public static void start(int port) {
        LogManager.getInstance().setLevel(LogLevel.DEBUG);
        LogManager.getInstance().addHandle(LogManager.createSystemOutHandle());

        // 创建 Console
        Console console = new Console();
        console.launch();

        // 创建服务器
        Server server = new Server(port);

        HandlerList handlers = Handlers.createHandlerList(server, console);
        server.setHandler(handlers);

        Logger.i(Main.class, "Start cube console server # " + port);

        System.out.println("" +
                "                                             \n" +
                "  ,----..                                    \n" +
                " /   /   \\                ,---,              \n" +
                "|   :     :         ,--,,---.'|              \n" +
                ".   |  ;. /       ,'_ /||   | :              \n" +
                ".   ; /--`   .--. |  | ::   : :      ,---.   \n" +
                ";   | ;    ,'_ /| :  . |:     |,-.  /     \\  \n" +
                "|   : |    |  ' | |  . .|   : '  | /    /  | \n" +
                ".   | '___ |  | ' |  | ||   |  / :.    ' / | \n" +
                "'   ; : .'|:  | : ;  ; |'   : |: |'   ;   /| \n" +
                "'   | '/  :'  :  `--'   \\   | '/ :'   |  / | \n" +
                "|   :    / :  ,      .-./   :    ||   :    | \n" +
                " \\   \\ .'   `--`----'   /    \\  /  \\   \\  /  \n" +
                "  `---`                 `-'----'    `----'   \n" +
                "                                             ");

        System.out.println("Enter \"http://Your-Server-IP:" + port + "\" in your browser to login Cube Console.");
        System.out.println();
        System.out.println("在浏览器中输入 \"http://您的服务器IP:" + port + "\" 登录 Cube Console 。");
        System.out.println();

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭容器。
     *
     * @param port
     */
    public static void stop(int port) {
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
            ContentResponse response = httpClient.GET("http://127.0.0.1:" + port + "/stop/");
            if (HttpStatus.OK_200 == response.getStatus()) {
                System.out.println("Cube Console - stop server # " + port);
            }
            else {
                System.out.println("Cube Console - STOP FAILED # " + port);
            }
        } catch (Exception e) {
            System.err.println("Cube Console - STOP FAILED # " + port + " - " + e.getMessage());
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
                Main.start(7080);
            }
            else if (args[0].equalsIgnoreCase("stop")) {
                Main.stop(7080);
            }
        }
        else {
            System.out.println("Please input args");
        }
    }
}
