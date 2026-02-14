/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */


package cube.service.aigc.utils;

import cube.processor.ProcessorContext;
import org.json.JSONObject;

public class WavToMp3Context extends ProcessorContext {

    private String inputFile;

    private String outputFile;

    public WavToMp3Context(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public String getInputFile() {
        return this.inputFile;
    }

    public String getOutputFile() {
        return this.outputFile;
    }

    @Override
    public JSONObject toJSON() {
        return null;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
