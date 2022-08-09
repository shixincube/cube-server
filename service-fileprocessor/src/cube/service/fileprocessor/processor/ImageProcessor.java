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
import cube.common.entity.FileResult;
import cube.common.entity.TextConstraint;
import cube.file.ImageOperation;
import cube.file.operation.*;
import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
        else if (GrayscaleOperation.Operation.equals(imageOperation.getOperation())) {
            // 转灰度图
            GrayscaleOperation operation = (GrayscaleOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("grayscale");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.grayscale(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename);

            // 处理结果
            ctx.setSuccessful(success);
            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
        else if (BrightnessOperation.Operation.equals(imageOperation.getOperation())) {
            // 调整亮度和对比度
            BrightnessOperation operation = (BrightnessOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("brightness");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.brightness(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename, operation.getBrightness(), operation.getContrast());

            // 处理结果
            ctx.setSuccessful(success);
            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
        else if (SharpeningOperation.Operation.equals(imageOperation.getOperation())) {
            // 锐化
            SharpeningOperation operation = (SharpeningOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("brightness");
            }

            // 使用 ImageMagick 操作
            boolean success = ImageMagick.sharpen(this.getWorkPath().toFile(), this.imageFile.getName(),
                    outputFilename, operation.getSigma());

            // 处理结果
            ctx.setSuccessful(success);
            if (success) {
                File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
        else if (ReplaceColorOperation.Operation.equals(imageOperation.getOperation())) {
            // 替换颜色
            ReplaceColorOperation operation = (ReplaceColorOperation) imageOperation;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("replaced");
            }

            List<ReplaceColorOperation> list = operation.getOperationList();
            if (null == list) {
                // 使用 ImageMagick 操作
                boolean success = ImageMagick.replaceColor(this.getWorkPath().toFile(), this.imageFile.getName(),
                        outputFilename, operation.getTargetColor(), operation.getReplaceColor(), operation.getFuzzFactor());

                // 处理结果
                ctx.setSuccessful(success);
                if (success) {
                    File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                    FileResult result = new FileResult(outputFile);
                    ctx.addResult(result);
                }
            }
            else {
                String tmpInputFilename = this.imageFile.getName();
                String tmpOutputFilename = makeOutputFilename("" + System.currentTimeMillis());

                List<String> tmpList = new ArrayList<>();

                boolean success = false;
                for (ReplaceColorOperation rco : list) {
                    success = ImageMagick.replaceColor(this.getWorkPath().toFile(), tmpInputFilename,
                            tmpOutputFilename, rco.getTargetColor(), rco.getReplaceColor(), rco.getFuzzFactor());

                    if (!success) {
                        break;
                    }

                    tmpList.add(tmpInputFilename);

                    // 将输出文件设置为新的输入文件
                    tmpInputFilename = tmpOutputFilename;
                    tmpOutputFilename = makeOutputFilename("" + System.currentTimeMillis());
                }

                // 清空临时文件
                if (tmpList.size() > 1) {
                    tmpList.remove(0);
                    for (String filename : tmpList) {
                        File tmpFile = new File(this.getWorkPath().toFile(), filename);
                        tmpFile.delete();
                    }
                }

                // 处理结果
                ctx.setSuccessful(success);
                if (success) {
                    File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                    // 修改文件名
                    File tmpFile = new File(this.getWorkPath().toFile(), tmpInputFilename);
                    tmpFile.renameTo(outputFile);
                    FileResult result = new FileResult(outputFile);
                    ctx.addResult(result);
                }
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
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
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
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
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
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
        else if (WatermarkOperation.Operation.equals(imageOperation.getOperation())) {
            // 覆盖水印
            WatermarkOperation operation = (WatermarkOperation) imageOperation;
            boolean success = false;

            if (null == outputFilename) {
                outputFilename = makeOutputFilename("watermark");
            }

            File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
            outputFile.delete();

            // 使用 ImageMagick 操作
            success = ImageMagick.coverWatermark(this.getWorkPath().toFile(), operation.getText(),
                    operation.getTextConstraint(), this.imageFile.getName(), outputFilename);
            // 处理结果
            ctx.setSuccessful(success);

            if (success) {
                outputFile = new File(this.getWorkPath().toFile(), outputFilename);
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }
    }

    private String makeOutputFilename(String suffix) {
        String outputFilename = null;
        if (null != this.imageFileLabel) {
            outputFilename = FileUtils.extractFileName(this.imageFileLabel.getFileName()) + "_" + suffix + ".png";
        }
        else {
            String filename = this.imageFile.getName();
            int index = filename.lastIndexOf("_");
            if (index > 0) {
                filename = filename.substring(0, index);
                outputFilename = filename + "_" + suffix + ".png";
            }
            else {
                outputFilename = FileUtils.extractFileName(this.imageFile.getName()) + "_" + suffix + ".png";
            }
        }

        // 将文件名中间的空格转为下划线
        outputFilename = outputFilename.replaceAll(" ", "_");

        return outputFilename;
    }
}
