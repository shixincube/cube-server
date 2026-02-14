/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.file.ImageOperation;
import cube.processor.ProcessorContext;
import org.json.JSONObject;

/**
 * 图像处理上下文。
 */
public class ImageProcessorContext extends ProcessorContext {

    private ImageOperation imageOperation;

    private FileLabel inputFileLabel;

    public ImageProcessorContext(ImageOperation imageOperation) {
        this.imageOperation = imageOperation;
    }

    public ImageOperation getImageOperation() {
        return this.imageOperation;
    }

    public void setInputFileLabel(FileLabel fileLabel) {
        this.inputFileLabel = fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.Image.name);

        if (null != this.inputFileLabel) {
            json.put("inputFileLabel", this.inputFileLabel.toCompactJSON());
        }

        if (null != this.imageOperation) {
            json.put("operation", this.imageOperation.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
