/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileProcessorAction;

/**
 * 生成图片缩略图。
 */
public class MakeThumb extends NoticeData {

    public final static String ACTION = FileProcessorAction.Thumb.name;

    public final static String DOMAIN = "domain";

    public final static String FILE_CODE = "fileCode";

    public final static String QUALITY = "quality";

    public MakeThumb(String domain, String fileCode) {
        this(domain, fileCode, 80);
    }

    public MakeThumb(String domain, String fileCode, int quality) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(FILE_CODE, fileCode);
        this.put(QUALITY, quality);
    }
}
