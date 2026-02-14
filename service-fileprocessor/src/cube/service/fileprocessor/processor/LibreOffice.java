/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cell.util.log.Logger;
import cube.processor.Processor;
import cube.processor.ProcessorContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Libre Office 操作处理器。
 */
public abstract class LibreOffice extends Processor {

    private AtomicBoolean running;

    public LibreOffice(Path workPath) {
        super(workPath);
        this.running = new AtomicBoolean(false);
    }

    protected boolean callConvertTo(String outputFormat, String inputFile, ProcessorContext context) {
        List<String> params = new ArrayList<>();
        params.add("--convert-to");
        params.add(outputFormat);
        params.add(inputFile);
        return this.call(params, context);
    }

    protected boolean call(List<String> params, ProcessorContext context) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("libreoffice");
        commandLine.addAll(params);

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(getWorkPath().toFile());

        try {
            process = pb.start();

            this.running.set(true);

            Runnable worker = this.buildInputStreamWorker(process.getErrorStream(), context);
            worker.run();

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "#call", e);
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        return (-1 != status);
    }
}
