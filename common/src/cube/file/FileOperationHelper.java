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

package cube.file;

import cube.common.action.FileProcessorAction;
import cube.file.operation.*;
import org.json.JSONObject;

/**
 * 文件操作辅助类。
 */
public final class FileOperationHelper {

    private FileOperationHelper() {
    }

    public static FileOperation parseFileOperation(JSONObject json) {
        String process = json.getString("process");

        if (FileProcessorAction.Image.name.equals(process)) {
            String operation = json.getString("operation");
            if (CropOperation.Operation.equals(operation)) {
                return new CropOperation(json);
            }
            else if (GrayscaleOperation.Operation.equals(operation)) {
                return new GrayscaleOperation(json);
            }
            else if (BrightnessOperation.Operation.equals(operation)) {
                return new BrightnessOperation(json);
            }
            else if (SharpeningOperation.Operation.equals(operation)) {
                return new SharpeningOperation(json);
            }
            else if (ReplaceColorOperation.Operation.equals(operation)) {
                return new ReplaceColorOperation(json);
            }
            else if (EliminateColorOperation.Operation.equals(operation)) {
                return new EliminateColorOperation(json);
            }
            else if (ReverseColorOperation.Operation.equals(operation)) {
                return new ReverseColorOperation(json);
            }
            else if (SteganographyOperation.Operation.equals(operation)) {
                return new SteganographyOperation(json);
            }
            else if (WatermarkOperation.Operation.equals(operation)) {
                return new WatermarkOperation(json);
            }
            else if (DetectObjectsOperation.Operation.equals(operation)) {
                return new DetectObjectsOperation(json);
            }
        }
        else if (FileProcessorAction.OfficeConvertTo.name.equals(process)) {
            return new OfficeConvertToOperation(json);
        }
        else if (FileProcessorAction.Video.name.equals(process)) {
            String operation = json.getString("operation");
            if (SnapshotOperation.Operation.equals(operation)) {
                return new SnapshotOperation(json);
            }
        }
        else if (FileProcessorAction.OCR.name.equals(process)) {
            return new OCROperation(json);
        }

        return null;
    }
}
