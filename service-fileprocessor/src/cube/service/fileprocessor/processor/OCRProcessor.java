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

import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.common.entity.ProcessResultStream;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 处理器。
 */
public class OCRProcessor extends OpticalCharacterRecognition {

    private FileLabel imageFileLabel;

    public OCRProcessor(Path workPath) {
        super(workPath);
    }

    public void setImageFileLabel(FileLabel imageFileLabel) {
        this.imageFileLabel = imageFileLabel;
    }

    private boolean check() {
        if (null == this.inputImage) {
            return false;
        }

        if (null == this.outputText) {
            String name = FileUtils.extractFileName(this.inputImage.getName());
            this.outputText = new File(this.getWorkPath().toString(), name);
        }

        return true;
    }

    @Override
    public void go(ProcessorContext context) {
        if (!this.check()) {
            Logger.w(OCRProcessor.class, "Check failed");
            return;
        }

        OCRProcessorContext processorContext = (OCRProcessorContext) context;

        List<String> commandLine = new ArrayList<>();
        commandLine.add("tesseract");
        commandLine.add(this.inputImage.getAbsolutePath());
        commandLine.add(this.outputText.getAbsolutePath());
        commandLine.add("-l");
        commandLine.add(this.language);
        commandLine.add("hocr");

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(getWorkPath().toFile());

        try {
            process = pb.start();

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        if (0 == status || 1 == status) {
            processorContext.setSuccessful(true);
            processorContext.setImageFileLabel(this.imageFileLabel);
            processorContext.readResult(this.outputText);

            File outputFile = new File(this.getWorkPath().toFile(), this.outputText.getName() + ".ocr");
            try {
                processorContext.getOcrFile().outputFile(new FileOutputStream(outputFile));
                ProcessResultStream resultStream = new ProcessResultStream(outputFile);
                processorContext.setResultStream(resultStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Logger.w(OCRProcessor.class, "Process error: " + status);
        }
    }
}
