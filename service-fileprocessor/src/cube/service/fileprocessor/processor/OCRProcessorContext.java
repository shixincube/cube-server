/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cell.util.log.Logger;
import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.file.OCRFile;
import cube.file.TesseractHocrFile;
import cube.file.operation.OCROperation;
import cube.processor.ProcessorContext;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 处理结果上下文。
 */
public class OCRProcessorContext extends ProcessorContext {

    private String language = "chi_sim+eng";

    private boolean singleLine = false;

    private FileLabel imageFileLabel;

    private List<String> resultText;

    private OCRFile ocrFile;

    public OCRProcessorContext(OCROperation ocrOperation) {
        super();
        this.resultText = new ArrayList<>();
        this.singleLine = ocrOperation.isSingleTextLine();
        if (null != ocrOperation.getLanguage()) {
            this.language = ocrOperation.getLanguage();
        }
    }

    public OCRProcessorContext(JSONObject json) {
        super(json);

        this.resultText = new ArrayList<>();

        JSONArray textArray = json.getJSONArray("text");
        for (int i = 0; i < textArray.length(); ++i) {
            this.resultText.add(textArray.getString(i));
        }

        if (json.has("image")) {
            this.imageFileLabel = new FileLabel(json.getJSONObject("image"));
        }
    }

    public String getLanguage() {
        return this.language;
    }

    public boolean isSingleLine() {
        return this.singleLine;
    }

    public void setImageFileLabel(FileLabel imageFileLabel) {
        this.imageFileLabel = imageFileLabel;
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

    public OCRFile getOcrFile() {
        return this.ocrFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        if (null != this.imageFileLabel) {
            json.put("image", this.imageFileLabel.toCompactJSON());
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
