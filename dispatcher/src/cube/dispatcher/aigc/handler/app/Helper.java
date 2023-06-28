/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.dispatcher.aigc.handler.app;

import cell.util.Utils;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Helper {

    private final static Pattern sChinesePattern = Pattern.compile("[\\u4E00-\\u9FA5|\\\\！|\\\\，|\\\\。|\\\\（|\\\\）|\\\\《|\\\\》|\\\\“|\\\\”|\\\\？|\\\\：|\\\\；|\\\\【|\\\\】]");

    private Helper() {
    }

    public static String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (null == authorization) {
            return null;
        }

        return authorization.replace("Baize ", "").trim();
    }

    public static void respondOk(AIGCHandler handler, HttpServletResponse response, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.put("status", "Success");
        payload.put("message", "");
        payload.put("data", data);
        handler.respondOk(response, payload);
        handler.complete();
    }

    public static void respondFailure(AIGCHandler handler, HttpServletResponse response, int status) {
        JSONObject payload = new JSONObject();
        payload.put("status", "Fail");
        payload.put("message", "");
        handler.respond(response, status);
        handler.complete();
    }

    public static List<String> splitContent(String content) {
        // 先拆为字符数组
        List<String> list = new LinkedList<>();
        char[] buf = content.toCharArray();
        for (int i = 0; i < buf.length; ++i) {
            String s = String.valueOf(buf[i]);
            Matcher m = sChinesePattern.matcher(s);
            int len = Utils.randomInt(5, 30);
            if (m.matches()) {
                // 中文字符
                StringBuilder word = new StringBuilder(s);
                while (++i < buf.length) {
                    s = String.valueOf(buf[i]);
                    m = sChinesePattern.matcher(s);
                    if (m.matches()) {
                        word.append(s);
                        if (word.length() >= len) {
                            break;
                        }
                    }
                    else {
                        // 非中文，结束
                        --i;
                        break;
                    }
                }
                list.add(word.toString());
            }
            else {
                StringBuilder word = new StringBuilder(s);
                while (++i < buf.length) {
                    s = String.valueOf(buf[i]);
                    m = sChinesePattern.matcher(s);
                    if (m.matches()) {
                        // 中文，结束
                        --i;
                        break;
                    }
                    else {
                        if (s.equals(" ") || s.equals(",") || s.equals(".") || s.equals(":") || s.equals("!") || s.equals("?")) {
                            // 符号，结束
                            word.append(s);
                            if (word.length() > len) {
                                break;
                            }
                        }
                        else {
                            word.append(s);
                        }
                    }
                }
                list.add(word.toString());
            }
        }

        return list;
    }


    public static void main(String[] args) {
        String content = "来自 App 测试：三国志刘备传 : AMny - 这是一串字符串，The sun is setting and the heartbroken are in the sky";
//        String content = "来自人工智能的测试：三国志刘备传 : AMny - 这是一串字符串";

        List<String> list = Helper.splitContent(content);
        for (String text : list) {
            System.out.println(text);
        }
    }
}
