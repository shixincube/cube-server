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
import cube.common.entity.ProcessResult;
import cube.common.entity.TextConstraint;
import cube.file.ImageOperation;
import cube.file.operation.CropOperation;
import cube.file.operation.EliminateColorOperation;
import cube.file.operation.ReverseColorOperation;
import cube.file.operation.SteganographyOperation;
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

    public void setImageFile(File file) {
        this.imageFile = file;
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

        String outputFilename = imageOperation.getOutputFilename();

        if (CropOperation.Operation.equals(imageOperation.getOperation())) {
            // 裁剪图像区域
            CropOperation operation = (CropOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("cropped");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.crop(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename, operation.getCropRect());
            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResult result = new ProcessResult(outputFile);
                ctx.setResult(result);
            }
        }
        else if (EliminateColorOperation.Operation.equals(imageOperation.getOperation())) {
            // 剔除颜色操作
            EliminateColorOperation operation = (EliminateColorOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("eliminated");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.eliminateColor(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename, operation.getReservedColor(), operation.getFillColor());

            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResult result = new ProcessResult(outputFile);
                ctx.setResult(result);
            }
        }
        else if (ReverseColorOperation.Operation.equals(imageOperation.getOperation())) {
            // 反转颜色
            ReverseColorOperation operation = (ReverseColorOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("reversed");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.reverseColor(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename);

            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResult result = new ProcessResult(outputFile);
                ctx.setResult(result);
            }
        }
        else if (SteganographyOperation.Operation.equals(imageOperation.getOperation())) {
            // 隐写数据到图像
            SteganographyOperation operation = (SteganographyOperation) imageOperation;
            boolean success = false;

            if (operation.isRecover()) {
                if (null == outputFilename) {
                    outputFilename = makeOutputFilename("watermark");
                }

                // 使用 ImageMagick 操作
                success = ImageMagick.recoverSteganography(this.getWorkPath().toFile(), operation.getWatermarkSize(),
                        this.imageFile.getName(), outputFilename);
            }
            else {
                if (null == outputFilename) {
                    outputFilename = makeOutputFilename("stegano");
                }

                TextConstraint textConstraint = operation.getTextConstraint();
                if (null == textConstraint) {
                    textConstraint = new TextConstraint();
                }
                // 使用 ImageMagick 操作
                success = ImageMagick.steganography(this.getWorkPath().toFile(), operation.getHiddenText(),
                        operation.getWatermarkSize(), textConstraint, this.imageFile.getName(), outputFilename);
            }

            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                ProcessResult result = new ProcessResult(outputFile);
                ctx.setResult(result);
            }
        }
    }

    private String makeOutputFilename(String suffix) {
        String outputFilename = null;
        if (null != this.imageFileLabel) {
            outputFilename = FileUtils.extractFileName(this.imageFileLabel.getFileName()) + "_" + suffix + ".png";
        }
        else {
            outputFilename = FileUtils.extractFileName(this.imageFile.getName()) + "_" + suffix + ".png";
        }
        return outputFilename;
    }
}