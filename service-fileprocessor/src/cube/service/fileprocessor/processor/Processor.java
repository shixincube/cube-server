/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cell.util.log.Logger;

import java.io.*;
import java.nio.file.Path;

/**
 * 处理器基类。
 */
public abstract class Processor {

    private Path workPath;

    public Processor(Path workPath) {
        this.workPath = workPath;
    }

    public Path getWorkPath() {
        return this.workPath;
    }

    protected Runnable buildInputStreamWorker(InputStream inputStream, ProcessorContext context) {
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        context.appendStdOutput(line);
                    }
                } catch (Exception e) {
                    Logger.w(Processor.class, "#buildInputStreamWorker", e);
                } finally {
                    if (null != reader) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // Nothing
                        }
                    }
                }
            }
        };

        return worker;
    }

    public abstract void go(ProcessorContext context);
}
