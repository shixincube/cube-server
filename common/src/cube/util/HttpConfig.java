/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

/**
 * HTTP 配置信息。
 */
public class HttpConfig {

    public int httpPort = 0;
    public int httpsPort = 0;
    public String keystore = null;
    public String storePassword = null;
    public String managerPassword = null;

    public int maxThreads = 32;
    public int minThreads = 8;

}
