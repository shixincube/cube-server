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

package cube.service.fileprocessor.processor;

import cube.common.entity.FileLabel;
import cube.common.entity.ProcessResultStream;
import cube.file.EliminateColorOperation;
import cube.file.ImageOperation;
import cube.file.ReverseColorOperation;
import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * 图像数据处理器。
 */
public class ImageProcessor extends Processor {

    private File imageFile;

    private FileLabel imageFileLabel;

    public ImageProcessor(Path workPath) {
        super(workPath);
    }

    public void setImageFile(File file, FileLabel fileLabel) {
        this.imageFile = file;
        this.imageFileLabel = fileLabel;
    }

    public File getImageFile() {
        return this.imageFile;
    }

    @Override
    public void go(ProcessorContext context) {
        if (!(context instanceof ImageProcessorContext)) {
            return;
        }

        ImageProcessorContext ctx = (ImageProcessorContext) context;
        ctx.setInputFileLabel(this.imageFileLabel);

        ImageOperation imageOperation = ctx.getImageOperation();
        if (imageOperation instanceof EliminateColorOperation) {
            // 剔除颜色操作
            EliminateColorOperation operation = (EliminateColorOperation) imageOperation;
            String outputFilename = operation.getOutputFilename();
            if (null == outputFilename) {
                outputFilename = FileUtils.extractFileName(this.imageFileLabel.getFileName()) + "_eliminated.jpg";
            }
            // 使用 ImageMagick 操作
            boolean success = ImageMagick.eliminateColor(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename, operation.getReservedColor(), operation.getFillColor());

            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResultStream resultStream = new ProcessResultStream(outputFile);
                ctx.setResultStream(resultStream);
            }
        }
        else if (imageOperation instanceof ReverseColorOperation) {
            // 反转颜色
            ReverseColorOperation operation = (ReverseColorOperation) imageOperation;
            String outputFilename = operation.getOutputFilename();
            if (null == outputFilename) {
                outputFilename = FileUtils.extractFileName(this.imageFileLabel.getFileName()) + "_reversed.jpg";
            }
            // 使用 ImageMagick 操作
            boolean success = ImageMagick.reverseColor(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename);

            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResultStream resultStream = new ProcessResultStream(outputFile);
                ctx.setResultStream(resultStream);
            }
        }
    }
}
