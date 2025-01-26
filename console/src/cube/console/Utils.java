/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console;

import java.util.HashMap;
import java.util.Map;

/**
 * 实用函数库。
 */
public final class Utils {

    private Utils() {
    }

    public static Map<String, String> parseQueryStringParams(String queryString) {
        Map<String, String> result = new HashMap<>();
        String[] array = queryString.split("&");

        for (String pair : array) {
            String[] param = pair.split("=");
            result.put(param[0], param[1]);
        }

        return result;
    }
}
