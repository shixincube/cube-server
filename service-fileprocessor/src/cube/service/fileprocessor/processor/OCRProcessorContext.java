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

package cube.service.fileprocessor.processor;

import cell.util.log.Logger;
import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.util.FileUtils;
import cube.file.OCRFile;
import cube.file.TesseractHocrFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 处理结果上下文。
 */
public class OCRProcessorContext extends ProcessorContext {

    private FileLabel imageFile;

    private List<String> resultText;

    private OCRFile ocrFile;

    public OCRProcessorContext() {
        super();
        this.resultText = new ArrayList<>();
    }

    public OCRProcessorContext(JSONObject json) {
        super(json);

        this.resultText = new ArrayList<>();

        JSONArray textArray = json.getJSONArray("text");
        for (int i = 0; i < textArray.length(); ++i) {
            this.resultText.add(textArray.getString(i));
        }

        if (json.has("image")) {
            this.imageFile = new FileLabel(json.getJSONObject("image"));
        }
    }

    public void setImageFile(FileLabel imageFile) {
        this.imageFile = imageFile;
    }

    public FileLabel getImageFile() {
        return this.imageFile;
    }

    public void readResult(File file) {
        String filename = file.getName();
        if (!file.exists()) {
            file = new File(file.getParent(), filename + ".txt");
            if (!file.exists()) {
                file = new File(file.getParent(), filename + ".hocr");
            }
        }

        String ext = FileUtils.extractFileExtension(file.getName());
        if (ext.length() == 0 || ext.equalsIgnoreCase("txt")) {
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    this.resultText.add(line);
                }
            } catch (FileNotFoundException e) {
                Logger.e(this.getClass(), "#readResult", e);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#readResult", e);
            } finally {
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }

            this.ocrFile = new OCRFile(this.resultText);
        }
        else if (ext.equalsIgnoreCase("hocr")) {
            TesseractHocrFile tesseractHocrFile = new TesseractHocrFile(file);
            this.ocrFile = tesseractHocrFile;
            this.resultText.addAll(tesseractHocrFile.toText());
        }
    }

    public List<String> getResultText() {
        return this.resultText;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        if (null != this.imageFile) {
            json.put("image", this.imageFile.toCompactJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.OCR.name);

        JSONArray text = new JSONArray();
        for (String line : this.resultText) {
            text.put(line);
        }
        json.put("text", text);

        json.put("ocr", this.ocrFile.toJSON());

        return json;
    }
}
