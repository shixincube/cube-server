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

package cube.util;

import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 进程管理器。
 */
public final class ProcessManager {

    private final static ProcessManager instance = new ProcessManager();

    private ExecutorService executor;

    private ProcessManager() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);
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