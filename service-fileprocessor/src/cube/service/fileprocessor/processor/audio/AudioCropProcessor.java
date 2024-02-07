/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.fileprocessor.processor.audio;

import cell.util.log.Logger;
import cube.common.entity.FileResult;
import cube.file.operation.AudioCropOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 音频文件裁剪处理器。
 */
public class AudioCropProcessor extends AudioProcessor {

    private AudioCropOperation operation;

    public AudioCropProcessor(Path workPath, AudioCropOperation operation) {
        super(workPath);
        this.operation = operation;
    }

    @Override
    public void go(ProcessorContext context) {
        AudioCropContext cropContext = (AudioCropContext) context;
        cropContext.setAudioOperation(this.operation);

        List<File> files = new ArrayList<>();

        long time = System.currentTimeMillis();

        JSONObject info = probe(this.inputFile.getName());

        String durationStr = info.getJSONObject("format").getString("duration");
        int duration = (int) Math.round(Double.parseDouble(durationStr));

        if (duration > this.operation.time) {
            int num = (int) Math.floor((float) duration / (float) this.operation.time);
            num += (duration % this.operation.time == 0) ? 0 : 1;
            int timePos = this.operation.start;

            for (int i = 0; i < num; ++i) {
                String outputName = this.inputFile.getName() + "_" + i
                        + "." + this.operation.outputType.getPreferredExtension();
                File outputFile = new File(this.getWorkPath().toFile(), outputName);
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                ArrayList<String> params = new ArrayList<>();
                params.add("-i");
                params.add(this.inputFile.getName());
                params.add("-acodec");
                params.add("copy");
                params.add("-ss");
                params.add(Integer.toString(timePos));
                params.add("-t");
                params.add(Integer.toString(this.operation.time));
                params.add(outputName);

                boolean result = this.call(params, cropContext);
                if (!result) {
                    Logger.w(this.getClass(), "#go - ffmpeg command failed");
                    return;
                }

                files.add(outputFile);

                timePos += this.operation.time;
            }
        }
        else {
            files.add(this.inputFile);
        }

        File outputFile = this.zipFiles(files);

        cropContext.setElapsedTime(System.currentTimeMillis() - time);
        cropContext.setSuccessful(true);

        FileResult fileResult = new FileResult(outputFile);
        cropContext.addResult(fileResult);

        if (this.deleteSourceFile) {
            // 删除输入文件
            for (File file : files) {
                file.delete();
            }

            if (this.inputFile.exists()) {
                this.inputFile.delete();
            }
        }
    }

    private File zipFiles(List<File> list) {
        String name = FileUtils.extractFileName(this.inputFile.getName());
        String outputFilename = name + ".zip";
        File output = new File(this.getWorkPath().toFile(), outputFilename);

        int len = 0;
        byte[] buf = new byte[2048];
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(output));
            for (File file : list) {
                FileInputStream input = new FileInputStream(file);
                zos.putNextEntry(new ZipEntry(name + File.separator + file.getName()));
                while ((len = input.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                input.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != zos) {
                try {
                    zos.close();
                } catch (IOException e) {
                }
            }
        }

        return output;
    }
}
