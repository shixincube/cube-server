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

import cube.common.entity.ProcessResult;
import cube.file.operation.OfficeConvertToOperation;
import cube.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Office 文档转换。
 */
public class OfficeConvertToProcessor extends LibreOffice {

    private static String NotFindFileSubstitutePath = "assets/CanNotFindFile.pdf";

    private File inputFile;

    public OfficeConvertToProcessor(Path workPath, File inputFile) {
        super(workPath);
        this.inputFile = inputFile;
    }

    @Override
    public void go(ProcessorContext context) {
        OfficeConvertToProcessorContext ctx = (OfficeConvertToProcessorContext) context;
        final String outputFormat = ctx.getOperation().getOutputFormat();

        boolean success = false;
        if (OfficeConvertToOperation.OUTPUT_FORMAT_PDF.equals(outputFormat)) {
            // 执行命令
            success = this.callConvertTo(OfficeConvertToOperation.OUTPUT_FORMAT_PDF, this.inputFile.getName(), ctx);
        }
        else if (OfficeConvertToOperation.OUTPUT_FORMAT_PNG.equals(outputFormat)) {
            // 执行命令
            // 先转 PDF
            success = this.callConvertTo(OfficeConvertToOperation.OUTPUT_FORMAT_PDF, this.inputFile.getName(), ctx);
        }

        // 判断输出文件是否存在
        // 输出文件名
        String outputFilename = FileUtils.extractFileName(this.inputFile.getName())
                + "." + OfficeConvertToOperation.OUTPUT_FORMAT_PDF;
        File outputFile = new File(this.getWorkPath().toFile(), outputFilename);
        if (!outputFile.exists()) {
            // 文件不存在
            success = false;
        }

        // 没有成功生成文件
        if (!success) {
            File substitute = new File(NotFindFileSubstitutePath);
            // 将替身拷贝到工作目录
            try {
                Files.copy(Paths.get(substitute.getAbsolutePath()), Paths.get(outputFile.getAbsolutePath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 再次判断文件是否存在
            outputFile = new File(outputFile.getAbsolutePath());
            success = outputFile.exists();
        }

        if (success) {
            if (OfficeConvertToOperation.OUTPUT_FORMAT_PNG.equals(outputFormat)) {
                // 将 PDF 转为图片
                List<File> files = ImageMagick.pdf2png(this.getWorkPath().toFile(), outputFile.getName());
                if (null == files || files.isEmpty()) {
                    success = false;
                }
                else {
                    for (File file : files) {
                        ProcessResult result = new ProcessResult(file);
                        ctx.addResult(result);
                    }
                }
            }
            else {
                ProcessResult result = new ProcessResult(outputFile);
                ctx.addResult(result);
            }
        }

        // 设置结果
        ctx.setSuccessful(success);
    }
}
