/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.utils;

import cell.util.log.Logger;
import cube.processor.FFmpeg;
import cube.processor.ProcessorContext;

import java.nio.file.Path;
import java.util.ArrayList;

public class AudioProcessor extends FFmpeg {

    public AudioProcessor(Path workPath) {
        super(workPath);
    }

    @Override
    public void go(ProcessorContext context) {
        if (context instanceof WavToMp3Context) {
            WavToMp3Context ctx = (WavToMp3Context) context;
            ArrayList<String> params = new ArrayList<>();
            params.add("-i");
            params.add(ctx.getInputFile());

        }
        else {
            Logger.w(this.getClass(), "#go - Unknown context");
        }
    }
}
