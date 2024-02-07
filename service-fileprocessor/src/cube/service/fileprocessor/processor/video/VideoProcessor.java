/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.fileprocessor.processor.video;

import cube.common.entity.FileLabel;
import cube.service.fileprocessor.processor.FFmpeg;
import cube.service.fileprocessor.processor.ProcessorContext;
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
