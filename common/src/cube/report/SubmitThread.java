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

package cube.report;

import cell.util.log.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 提交线程。
 */
public class SubmitThread extends Thread {

    private List<String> hostUrls;

    private ConcurrentLinkedQueue<Report> queue;

    private AtomicBoolean running;

    public SubmitThread(List<String> hostUrls, ConcurrentLinkedQueue<Report> queue, AtomicBoolean running) {
        super("SubmitThread");
        this.hostUrls = hostUrls;
        this.queue = queue;
        this.running = running;
    }

    @Override
    public void run() {
        HttpClient client = new HttpClient();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Report report = null;
        while ((report = this.queue.peek()) != null) {
            boolean submitted = false;

            for (String url : this.hostUrls) {
                StringContentProvider provider = new StringContentProvider(report.toJSON().toString());

                try {
                    long time = System.currentTimeMillis();
                    ContentResponse response = client.POST(url).content(provider).timeout(10, TimeUnit.SECONDS).send();
                    long duration = System.currentTimeMillis() - time;
                    if (response.getStatus() == HttpStatus.OK_200) {
                        submitted = true;
                        Logger.i(this.getClass(), "Report: \"" + report.getName() + "\" (" + report.getReporter() + ") submitted - " + url + " - " + duration);
                    }
                    else {
                        Logger.w(this.getClass(), "Report: \"" + report.getName() + "\" (" + report.getReporter() + ") submit failed - " + url + " - " + duration);
                    }
                } catch (InterruptedException e) {
                    Logger.w(this.getClass(), "Submitting report \"" + report.getName() + "\" (" + report.getReporter() + ") failed", e);
                } catch (TimeoutException e) {
                    Logger.w(this.getClass(), "Submitting report \"" + report.getName() + "\" (" + report.getReporter() + ") failed", e);
                } catch (ExecutionException e) {
                    //Logger.d(this.getClass(), "Submitting report \"" + report.getName() + "\" (" + report.getReporter() + ") failed", e);
                } catch (ArithmeticException e) {
                    // Nothing
                }
            }

            if (submitted) {
                // 发送成功从队列中移除
                this.queue.poll();
            }
            else {
                // 没有提交成功
                break;
            }
        }

        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.running.set(false);
    }
}
