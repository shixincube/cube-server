/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.video;

import cube.common.entity.FileLabel;
import cube.processor.FFmpeg;
import cube.processor.ProcessorContext;
import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * 视频处理器。
 */
public abstract class VideoProcessor extends FFmpeg {

    protected File inputFile;

    protected FileLabel inputFileLabel;

    protected boolean deleteSourceFile = false;

    public VideoProcessor(Path workPath) {
        super(workPath);
    }

    public void setInputFile(File file, FileLabel fileLabel) {
        this.inputFile = file;
        this.inputFileLabel = fileLabel;
    }

    public void setInputFile(File file) {
        this.inputFile = file;
    }

    protected String getFilename() {
        if (null != this.inputFileLabel) {
            return FileUtils.extractFileName(this.inputFileLabel.getFileName());
        }
        else {
            return FileUtils.extractFileName(this.inputFile.getName());
        }
    }

    public void setDeleteSourceFile(boolean value) {
        this.deleteSourceFile = value;
    }

    public abstract void go(ProcessorContext context);
}
