/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.processor.Processor;

import java.io.File;
import java.nio.file.Path;

/**
 * OCR 处理器。
 */
public abstract class OpticalCharacterRecognition extends Processor {

    protected File inputImage;

    protected File outputText;

    //protected String language = "chi_sim+eng";

    public OpticalCharacterRecognition(Path workPath) {
        super(workPath);
    }

    public void setInputImage(File file) {
        this.inputImage = file;
    }

    public void setOutputText(File file) {
        this.outputText = file;
    }

    public File getInputImage() {
        return this.inputImage;
    }

    public File getOutputText() {
        return this.outputText;
    }
}
