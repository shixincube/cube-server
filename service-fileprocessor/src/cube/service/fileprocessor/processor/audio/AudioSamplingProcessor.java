/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.audio;

import cell.util.log.Logger;
import cube.common.entity.FileResult;
import cube.file.operation.AudioSamplingOperation;
import cube.service.fileprocessor.processor.ProcessorContext;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 音频文件采样处理器。
 */
public class AudioSamplingProcessor extends AudioProcessor {

    private AudioSamplingOperation operation;

    public AudioSamplingProcessor(Path workPath, AudioSamplingOperation operation) {
        super(workPath);
        this.operation = operation;
    }

    @Override
    public void go(ProcessorContext context) {
        AudioSamplingContext samplingContext = (AudioSamplingContext) context;
        samplingContext.setAudioOperation(this.operation);

        long time = System.currentTimeMillis();

        String outputName = this.getFilename() + "_" + System.currentTimeMillis()
                + "." + this.operation.outputType.getPreferredExtension();
        File outputFile = new File(this.getWorkPath().toFile(), outputName);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        ArrayList<String> params = new ArrayList<>();
        params.add("-i");
        params.add(this.inputFile.getName());
        params.add("-ac");
        params.add(Integer.toString(this.operation.channel));
        params.add("-ar");
        params.add(Integer.toString(this.operation.rate));
        params.add(outputName);

        boolean result = this.call(params, samplingContext);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        samplingContext.setElapsedTime(System.currentTimeMillis() - time);
        samplingContext.setSuccessful(true);

        FileResult fileResult = new FileResult(outputFile);
        samplingContext.addResult(fileResult);

        if (this.deleteSourceFile) {
            // 删除输入文件
            this.inputFile.delete();
        }
    }
}
