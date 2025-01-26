/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.file.ImageOperation;

/**
 * 处理图像数据。
 */
public class ProcessImage extends NoticeData {

    public final static String ACTION = FileProcessorAction.Image.name;

    public final static String DOMAIN = "domain";

    public final static String FILE_CODE = "fileCode";

    public final static String PARAMETER = "parameter";

    public ProcessImage(FileLabel fileLabel, ImageOperation operation) {
        super(ACTION);
        this.put(DOMAIN, fileLabel.getDomain().getName());
        this.put(FILE_CODE, fileLabel.getFileCode());
        this.put(PARAMETER, operation.toJSON());
    }

    public ProcessImage(String domain, String fileCode, ImageOperation operation) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(FILE_CODE, fileCode);
        this.put(PARAMETER, operation.toJSON());
    }
}
