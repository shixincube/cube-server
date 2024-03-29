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

package cube.auth;

public final class AuthConsts {

    /**
     * 默认域。
     */
    public static String DEFAULT_DOMAIN = "default_domain";

    /**
     * 默认 App ID 。
     */
    public static String DEFAULT_APP_ID = "CubeApp";

    /**
     * 默认 App Key 。
     */
    public static String DEFAULT_APP_KEY = "default-opensource-appkey";


    public static String formatString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Default domain : ").append(AuthConsts.DEFAULT_DOMAIN).append("\n");
        buf.append("Default app id : ").append(AuthConsts.DEFAULT_APP_ID).append("\n");
        buf.append("Default app key: ").append(AuthConsts.DEFAULT_APP_KEY);
        return buf.toString();
    }
}
