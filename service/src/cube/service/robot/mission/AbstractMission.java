/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.robot.mission;

import cube.robot.Task;
import cube.util.FileUtils;
import cube.util.ZipUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象任务。
 */
public abstract class AbstractMission {

    public final static Path sWorkingPath = Paths.get("assets/robot/");

    public final static Path sModulePath = Paths.get("assets/robot/modules/");

    protected String taskName;

    protected long timeInMillis = 0;
    protected int timeFlag = 0;
    protected String mainFile;
    protected String taskFile;

    protected Task task;

    protected JSONObject realtimeParameter;

    public AbstractMission(String taskName, String mainFile, String taskFile) {
        this.taskName = taskName;
        this.mainFile = mainFile;
        this.taskFile = taskFile;
    }

    public void setParameter(JSONObject data) {
        this.realtimeParameter = data;
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

    protected Path packScriptFiles(String[] inputFiles, Path outputFile) {
        File paramFile = createParameterScriptFile();
        FileOutputStream fos = null;

        try {
            List<File> fileList = new ArrayList<>();
            fileList.add(paramFile);
            for (String filename : inputFiles) {
                fileList.add(new File(sWorkingPath.toFile(), filename));
            }

            // 删除旧文件
            if (Files.exists(outputFile)) {
                Files.delete(outputFile);
            }

            fos = new FileOutputStream(outputFile.toFile());
            ZipUtils.toZip(fileList, fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }

            if (paramFile.exists()) {
                paramFile.delete();
            }
        }

        return outputFile;
    }

    public abstract void checkMission();

    public abstract boolean uploadScriptFiles();

    private File createParameterScriptFile() {
        String filename = FileUtils.extractFileName(this.taskFile);
        filename = filename + "Parameter.js";

        StringBuilder content = new StringBuilder();
        content.append("module.exports = ");
        if (null != this.realtimeParameter) {
            content.append(this.realtimeParameter.toString());
        }
        else {
            content.append("{}");
        }
        content.append("\n");

        Path file = Paths.get(sWorkingPath.toString(), filename);
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }

            Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file.toFile();
    }
}
