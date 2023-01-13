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

import cell.util.log.Logger;
import cube.robot.Task;
import cube.robot.TaskNames;
import cube.service.robot.Roboengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 报告抖音账号数据。
 * 参数：
 * word {String}
 */
public class ReportDouYinAccountData extends AbstractMission {

    private Roboengine roboengine;

    public ReportDouYinAccountData(Roboengine roboengine) {
        super(TaskNames.ReportDouYinAccountData,
                "CubeReportDouYinAccountData.js",
                "CubeReportDouYinAccountData.zip");
        this.roboengine = roboengine;
    }

    @Override
    public void checkMission() {
        Task task = this.roboengine.getTask(getTaskName());
        if (null == task) {
            // 尝试创建任务
            task = this.roboengine.createTask(getTaskName(), getTimeInMillis(),
                    getTimeFlag(), getMainFile(), getTaskFile());
        }

        if (null == task) {
            Logger.e(ReportDouYinAccountData.class,
                    "#checkMission - No task data in Roboengine, task name: " + getTaskName());
            return;
        }

        setTask(task);
    }

    @Override
    public boolean uploadScriptFiles() {
        Path tmpPath = Paths.get(sWorkingPath.toString() + "/tmp/");
        if (!Files.exists(tmpPath)) {
            try {
                Files.createDirectories(tmpPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Path outputFile = Paths.get(tmpPath.toString() + "/" + this.taskFile);

        this.packScriptFiles(new String[] {
                this.mainFile,
                "modules/DouYinAuthorList.js",
                "modules/DouYinVideoInfo.js",
                "modules/StopApp.js"
        }, outputFile);

        // 上传数据
        return this.roboengine.uploadScript(this.taskFile, outputFile);
    }
}
