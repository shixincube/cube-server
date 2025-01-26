/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
 * 抖音日常操作。
 *
 * 参数：
 * duration {Integer} 持续时长，单位：秒。
 */
public class DouYinDailyOperation extends AbstractMission {

    private Roboengine roboengine;

    public DouYinDailyOperation(Roboengine roboengine) {
        super(TaskNames.DouYinDailyOperation,
                "CubeDouYinDailyOperation.js",
                "CubeDouYinDailyOperation.zip");
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
            Logger.e(this.getClass(),
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
                "modules/StopApp.js"
        }, outputFile);

        // 上传数据
        return this.roboengine.uploadScript(this.taskFile, outputFile);
    }
}
