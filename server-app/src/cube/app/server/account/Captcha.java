/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.account;

import java.io.File;

/**
 * 验证码。
 */
public class Captcha {

    public final String code;

    public final File file;

    public Captcha(String code, File file) {
        this.code = code;
        this.file = file;
    }
}
