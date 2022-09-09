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

package cube.file;

import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.common.entity.FileResult;
import cube.file.operation.EliminateColorOperation;
import cube.file.operation.ReverseColorOperation;
import cube.file.operation.SnapshotOperation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件处理结果。
 */
public class FileProcessResult {

    public final String process;

    public final boolean success;

    private SubmitWorkflowResult submitWorkflowResult;

    private ImageProcessResult imageResult;

    private VideoProcessResult videoResult;

    private OCRProcessResult ocrResult;

    private List<String> logs;

    private List<FileResult> resultList;

    private List<File> localFileList;

    public FileProcessResult(JSONObject json) {
        this.process = json.getString("process");
        this.success = json.has("success") ? json.getBoolean("success") : false;

        if (FileProcessorAction.Image.name.equals(this.process)) {
            this.imageResult = new ImageProcessResult(json);
        }
        else if (FileProcessorAction.Video.name.equals(this.process)) {
            this.videoResult = new VideoProcessResult(json);
        }
        else if (FileProcessorAction.OCR.name.equals(this.process)) {
            this.ocrResult = new OCRProcessResult(json);
        }
        else if (FileProcessorAction.SubmitWorkflow.name.equals(this.process)) {
            this.submitWorkflowResult = new SubmitWorkflowResult(json);
        }

        if (json.has("resultList")) {
            JSONArray array = json.getJSONArray("resultList");
            this.resultList = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); ++i) {
                FileResult result = new FileResult(array.getJSONObject(i));
                this.resultList.add(result);
            }
        }

        if (json.has("logs")) {
            JSONArray array = json.getJSONArray("logs");
            this.logs = new ArrayList<>(array.length());

            for (int i = 0; i < array.length(); ++i) {
                this.logs.add(array.getString(i));
            }
        }
    }

    public List<String> getLogs() {
        return this.logs;
    }

    public boolean hasResult() {
        return (null != this.resultList);
    }

    public List<FileResult> getResultList() {
        return this.resultList;
    }

    public SubmitWorkflowResult getSubmitWorkflowResult() {
        return this.submitWorkflowResult;
    }

    public ImageProcessResult getImageResult() {
        return this.imageResult;
    }

    public VideoProcessResult getVideoResult() {
        return this.videoResult;
    }

    public OCRProcessResult getOCRResult() {
        return this.ocrResult;
    }

    public void addLocalFile(File file) {
        synchronized (this) {
            if (null == this.localFileList) {
                this.localFileList = new ArrayList<>();
            }

            this.localFileList.add(file);
        }
    }

    public List<File> getLocalFileList() {
        return this.localFileList;
    }

    /**
     * 提交工作流结果。
     */
    public class SubmitWorkflowResult {
        public final boolean successful;

        public SubmitWorkflowResult(JSONObject json) {
            this.successful = json.getBoolean("success");
        }
    }

    /**
     * 图像处理结果。
     */
    public class ImageProcessResult {

        public final boolean successful;

        private FileLabel inputFileLabel;

        private ImageOperation operation;

        public ImageProcessResult(JSONObject json) {
            this.successful = json.getBoolean("success");

            this.inputFileLabel = json.has("inputFileLabel") ?
                    new FileLabel(json.getJSONObject("inputFileLabel")) : null;

            String operation = json.getJSONObject("operation").getString("operation");
            if (EliminateColorOperation.Operation.equals(operation)) {
                this.operation = new EliminateColorOperation(json.getJSONObject("operation"));
            }
            else if (ReverseColorOperation.Operation.equals(operation)) {
                this.operation = new ReverseColorOperation(json.getJSONObject("operation"));
            }
        }

        public ImageOperation getOperation() {
            return this.operation;
        }

        public FileLabel getInputFileLabel() {
            return this.inputFileLabel;
        }
    }

    /**
     * 视频处理结果。
     */
    public class VideoProcessResult {

        public final boolean successful;

        private VideoOperation operation;

        public VideoProcessResult(JSONObject json) {
            this.successful = json.getBoolean("success");

            String operation = json.getJSONObject("operation").getString("operation");
            if (SnapshotOperation.Operation.equals(operation)) {
                this.operation = new SnapshotOperation(json.getJSONObject("operation"));


            }
        }

        public VideoOperation getOperation() {
            return this.operation;
        }
    }

    /**
     * OCR 处理结果。
     */
    public class OCRProcessResult {

        public final boolean successful;

        private List<String> resultText;

        private OCRFile ocr;

        private FileLabel imageFile;

        public OCRProcessResult(JSONObject json) {
            this.successful = json.getBoolean("success");

            if (json.has("text")) {
                this.resultText = new ArrayList<>();
                JSONArray textArray = json.getJSONArray("text");
                for (int i = 0; i < textArray.length(); ++i) {
                    this.resultText.add(textArray.getString(i));
                }
            }

            if (json.has("ocr")) {
                this.ocr = new OCRFile(json.getJSONObject("ocr"));
            }

            if (json.has("image")) {
                this.imageFile = new FileLabel(json.getJSONObject("image"));
            }
        }

        public List<String> getResultText() {
            return this.resultText;
        }

        public OCRFile getOCRFile() {
            return this.ocr;
        }

        public FileLabel getImageFile() {
            return this.imageFile;
        }
    }
}
