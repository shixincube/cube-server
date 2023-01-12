/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.robot.mission;

import cube.robot.Task;

public abstract class AbstractMission {

    protected String taskName;

    protected long timeInMillis = 0;
    protected int timeFlag = 0;
    protected String mainFile;
    protected String taskFile;

    protected Task task;

    public AbstractMission(String taskName, String mainFile, String taskFile) {
        this.taskName = taskName;
        this.mainFile = mainFile;
        this.taskFile = taskFile;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public long getTimeInMillis() {
        return this.timeInMillis;
    }

    public int getTimeFlag() {
        return this.timeFlag;
    }

    public String getMainFile() {
        return this.mainFile;
    }

    public String getTaskFile() {
        return this.taskFile;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return this.task;
    }

    public boolean isTaskReady() {
        return (null != this.task);
    }

    public abstract void checkMission();
}
