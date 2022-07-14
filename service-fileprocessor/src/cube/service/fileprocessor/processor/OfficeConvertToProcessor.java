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
import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * Office 文档转换。
 */
public class OfficeConvertToProcessor extends LibreOffice {

    private File inputFile;

    public OfficeConvertToProcessor(Path workPath, File inputFile) {
        super(workPath);
        this.inputFile = inputFile;
    }

    @Override
    public void go(ProcessorContext context) {
        OfficeConvertToProcessorContext ctx = (OfficeConvertToProcessorContext) context;
        String outputFormat = ctx.getOperation().getOutputFormat();

        // 执行命令
        boolean success = this.callConvertTo(outputFormat, this.inputFile.getName(), ctx);

        File outputFile = null;

        if (success) {
            // 判断输出文件是否存在
            // 输出文件名
            String outputFilename = FileUtils.extractFileName(this.inputFile.getName()) + "." + outputFormat;
            outputFile = new File(this.getWorkPath().toFile(), outputFilename);
            if (!outputFile.exists()) {
                // 文件不存在
                success = false;
            }
        }

        // 处理结果
        ctx.setSuccessful(success);

        if (success) {
            ProcessResult result = new ProcessResult(outputFile);
            ctx.setResult(result);
        }
    }
}
