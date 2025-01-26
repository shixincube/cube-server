/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.common.entity.FileResult;
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
        commandLine.add(processorContext.getLanguage());

        if (processorContext.isSingleLine()) {
            commandLine.add("--psm");
            commandLine.add("7");
        }

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

        if (-1 != status) {
            processorContext.setSuccessful(true);
            processorContext.setImageFileLabel(this.imageFileLabel);
            processorContext.readResult(this.outputText);

            File outputFile = new File(this.getWorkPath().toFile(), this.outputText.getName() + ".ocr");
            if (!outputFile.exists()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                processorContext.getOcrFile().outputFile(new FileOutputStream(outputFile));
                FileResult result = new FileResult(outputFile);
                processorContext.addResult(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Logger.w(OCRProcessor.class, "Process error: " + status);
        }
    }
}
