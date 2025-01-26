/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.audio;

import cube.common.entity.FileLabel;
import cube.service.fileprocessor.processor.FFmpeg;
import cube.service.fileprocessor.processor.ProcessorContext;
import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * 视频处理器。
 */
public abstract class AudioProcessor extends FFmpeg {

    protected File inputFile;

    protected FileLabel inputFileLabel;

    protected boolean deleteSourceFile = false;

    public AudioProcessor(Path workPath) {
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
