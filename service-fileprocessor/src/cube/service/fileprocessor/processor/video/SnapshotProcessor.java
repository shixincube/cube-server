/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.video;

import cell.util.log.Logger;
import cube.common.entity.FileResult;
import cube.file.operation.SnapshotOperation;
import cube.processor.ProcessorContext;
import cube.util.TimeUtils;
import cube.util.ZipUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * 视频帧处理器。
 */
public class SnapshotProcessor extends VideoProcessor {

    private SnapshotOperation snapshotOperation;

    public SnapshotProcessor(Path workPath, SnapshotOperation operation) {
        super(workPath);
        this.snapshotOperation = operation;
    }

    private void postHandle(File path, SnapshotContext snapshotContext) {
        if (!path.isDirectory()) {
            return;
        }

        File outputFile = new File(path, "snapshot_" + TimeUtils.formatDateForPathSymbol(System.currentTimeMillis()) + "_"
                        + this.getFilename() + ".zip");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        File[] files = path.listFiles();
        if (null == files || files.length == 0) {
            return;
        }

        ArrayList<File> list = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                if (file.getName().endsWith("zip")) {
                    continue;
                }

                list.add(file);
            }
        }

        if (snapshotContext.copyToWorkPath) {
            // 复制到工作目录
            String filenamePrefix = "snapshot_" + path.getName() + "_";
            ArrayList<File> newList = new ArrayList<>();
            for (File file : list) {
                File targetFile = new File(getWorkPath().toFile(), filenamePrefix + file.getName());
                try {
                    Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(targetFile.getAbsolutePath()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                newList.add(targetFile);
            }

            // 设置输出结果
            snapshotContext.setOutputFiles(newList);
        }
        else {
            // 设置输出结果
            snapshotContext.setOutputFiles(list);
        }

        // 文件打包
        if (snapshotContext.packToZip) {
            try {
                ZipUtils.toZip(list, new FileOutputStream(outputFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (outputFile.exists()) {
                FileResult result = new FileResult(outputFile);
                snapshotContext.addResult(result);
            }
        }
    }

    @Override
    public void go(ProcessorContext context) {
        if (!(context instanceof SnapshotContext)) {
            return;
        }

        long time = System.currentTimeMillis();

        SnapshotContext snapshotContext = (SnapshotContext) context;
        // 当前上下文的输入文件
        snapshotContext.setInputFile(this.inputFile);
        // 设置参数
        snapshotContext.setVideoOperation(this.snapshotOperation);

        String output = (null != this.inputFileLabel) ? this.inputFileLabel.getFileCode() :
                this.getFilename();

        // 创建输出目录
        Path outputPath = Paths.get(getWorkPath().toString(), output);
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> params = new ArrayList<>();
        params.add("-ss");
        params.add(snapshotContext.offset.formatHMSMs());
        params.add("-i");
        params.add(this.inputFile.getName());
        params.add("-f");
        params.add("image2");
        params.add("-r");
        params.add(String.format("%.2f", snapshotContext.rate));

        if (!snapshotContext.duration.isZero()) {
            params.add("-t");
            params.add(snapshotContext.duration.formatHMSMs());
        }

        params.add("-y");
        params.add(output + "/%05d." + snapshotContext.outputType.getPreferredExtension());

        boolean result = this.call(params, snapshotContext);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        snapshotContext.setElapsedTime(System.currentTimeMillis() - time);
        snapshotContext.setSuccessful(true);

        // 后处理
        File path = new File(getWorkPath().toFile(), output);
        this.postHandle(path, snapshotContext);
    }
}
