/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */


package cube.service.aigc.utils;

import cube.processor.ProcessorContext;
import org.json.JSONObject;

/**
 * WAV 转 MP3
 */
public class WavToMp3Context extends ProcessorContext {

    private String inputFile;

    private String outputFile;

    private int sampleRate;

    private int channels;

    public WavToMp3Context(String inputFile, String outputFile, int sampleRate, int channels) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public String getInputFile() {
        return this.inputFile;
    }

    public String getOutputFile() {
        return this.outputFile;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public int getChannels() {
        return this.channels;
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
