/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server;

import cell.carpet.log.RollFileLogger;
import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.app.server.container.ContainerManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 程序入口。
 */
public class Main {

    /**
     * 启动服务器。
     */
    public static void start() {
        LogManager.getInstance().setLevel(LogLevel.INFO);
        LogManager.getInstance().addHandle(LogManager.createSystemOutHandle());

        // 滚动文件日志
        RollFileLogger fileLogger = null;
        Path path = Paths.get("logs");
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }

            fileLogger = new RollFileLogger("FileLogger");
            fileLogger.open(path.toString() + File.separator + "app-server.log");

            // 设置日志操作器
            LogManager.getInstance().addHandle(fileLogger);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.i(Main.class, "--------------------------------");
        Logger.i(Main.class, "Version " + Version.toVersionString());
        Logger.i(Main.class, "--------------------------------");

        ContainerManager containerManager = new ContainerManager();

        // 启动容器
        containerManager.launch();

        LogManager.getInstance().removeAllHandles();
        fileLogger.close();
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
                Main.start();
            }
            else if (args[0].equalsIgnoreCase("stop")) {
                Main.stop(7777);
            }

            System.exit(0);
        }
        else {
            System.out.println("Please input args");
        }
    }
}
