/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 进程管理器。
 */
public final class ProcessManager {

    private final static ProcessManager instance = new ProcessManager();

    private ExecutorService executor;

    private ProcessManager() {
        this.executor = Executors.newCachedThreadPool();
    }

    public final static ProcessManager getInstance() {
        return ProcessManager.instance;
    }

    public void destroy() {
        this.executor.shutdown();
    }

    public ProcessOutput run(List<String> commandLine) {
        return this.run(commandLine, false);
    }

    public ProcessOutput run(List<String> commandLine, boolean outputError) {
        ProcessBuilder pb = new ProcessBuilder(commandLine);

        ProcessOutput output = null;
        Process process = null;

        try {
            // 启动进程
            process = pb.start();

            output = new ProcessOutput(process);
            // 监听输入流
            output.monitorInputStream();

            if (outputError) {
                output.monitorErrorStream();
            }

            try {
                output.status = process.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            process = null;
        }

        return output;
    }

    public class ProcessOutput {

        protected int status = 1;

        /**
         * 0 - 未执行
         * 1 - 正在执行
         * 2 - 结束
         */
        protected int phase = 0;

        protected Process process;

        protected BufferedReader stdInput = null;

        protected BufferedReader errorInput = null;

        private List<String> stdInputList = null;

        private List<String> errorInputList = null;

        protected ProcessOutput(Process process) {
            this.process = process;
        }

        protected void monitorInputStream() {
            this.stdInputList = new ArrayList<>();
            this.stdInput = new BufferedReader(new InputStreamReader(this.process.getInputStream()));

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    phase = 1;

                    try {
                        String line = null;
                        while ((line = stdInput.readLine()) != null) {
                            if (line.length() > 0) {
                                stdInputList.add(line);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            stdInput.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        phase = 2;
                    }
                }
            });

        }

        protected void monitorErrorStream() {
            this.errorInputList = new ArrayList<>();
            this.errorInput = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line = null;
                        while ((line = errorInput.readLine()) != null) {
                            if (line.length() > 0) {
                                errorInputList.add(line);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            errorInput.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        phase = 2;
                    }
                }
            });
        }

        public int getStatus() {
            return this.status;
        }

        public boolean isFinish() {
            return this.phase == 2;
        }

        public List<String> getStdInput() {
            return this.stdInputList;
        }

        public List<String> getErrorInput() {
            return this.errorInputList;
        }
    }
}
