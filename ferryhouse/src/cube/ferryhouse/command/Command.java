/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse.command;

import cell.util.log.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 命令描述。
 */
public class Command {

    private AtomicBoolean running;

    private final String shell;
    private final String shellParam;

    private Path workPath;

    public Command() {
        this.running = new AtomicBoolean(false);
        this.shell = "sh";
        this.shellParam = "-c";
    }

    public void setWorkPath(Path workPath) {
        this.workPath = workPath;
    }

    public boolean isRunning() {
        return this.running.get();
    }

    protected Runnable buildInputStreamWorker(InputStream inputStream, List<String> content) {
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = null;
                String line = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = reader.readLine()) != null) {
                        content.add(line);
                    }
                } catch (Exception e) {
                    Logger.w(Command.class, "#buildInputStreamWorker", e);
                } finally {
                    if (null != reader) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // Nothing
                        }
                    }
                }
            }
        };

        return worker;
    }

    public boolean execute(String command, List<String> params) {
        return this.execute(command, params, null);
    }

    public boolean execute(String command, List<String> params, List<String> output) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(command);
        if (null != params) {
            commandLine.addAll(params);
        }

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);

        if (null != this.workPath) {
            pb.directory(this.workPath.toFile());
        }
        else {
            pb.directory(new File(System.getProperty("user.home")));
        }

        try {
            process = pb.start();

            this.running.set(true);

            if (null != output) {
                Runnable worker = this.buildInputStreamWorker(process.getInputStream(), output);
                worker.run();
            }

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Logger.w(this.getClass(), "#execute", e);
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        this.running.set(false);

        return (0 == status || 1 == status);
    }
}
