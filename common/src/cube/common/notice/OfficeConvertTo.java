/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileProcessorAction;
import org.json.JSONObject;

/**
 * 文档转换。
 */
public class OfficeConvertTo extends NoticeData {

    public final static String ACTION = FileProcessorAction.OfficeConvertTo.name;

    public final static String DOMAIN = "domain";

    public final static String FILE_CODE = "fileCode";

    public final static String PARAMETER = "parameter";

    public final static String FORMAT = "format";

    public OfficeConvertTo(String domain, String fileCode, String format) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(FILE_CODE, fileCode);

        JSONObject parameter = new JSONObject();
        parameter.put(FORMAT, format);

        this.put(PARAMETER, parameter);
    }
}
