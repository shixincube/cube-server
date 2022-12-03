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

import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FilterHandler extends CrossDomainHandler {

    private volatile boolean completed = false;

    private int minInterval = 0;
    private long lastTimestamp = 0;

    public FilterHandler() {
        super();
    }

    protected boolean isCompleted() {
        return this.completed;
    }

    public void setMinInterval(int value) {
        this.minInterval = value;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.completed = false;

        // 检查最小访问间隔
        if (this.checkMinInterval(response)) {
            return;
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.completed = false;

        // 检查最小访问间隔
        if (this.checkMinInterval(response)) {
            return;
        }
    }

    private boolean checkMinInterval(HttpServletResponse response) {
        if (this.minInterval > 0) {
            long now = System.currentTimeMillis();
            if (now - this.lastTimestamp < this.minInterval) {
                // 访问间隔小于最小间隔
                this.respond(response, HttpStatus.SERVICE_UNAVAILABLE_503);
                this.complete();
                this.completed = true;
            }

            this.lastTimestamp = now;
        }

        return this.completed;
    }
}
