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

package cube.util;

import cube.common.entity.TextConstraint;
import cube.vision.Size;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本实用函数库。
 */
public final class TextUtils {

    // Pattern.compile("^-?[0-9]+");
    private static final Pattern sPatternNumeric = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private static final Pattern sBrowserNameSafari = Pattern.compile("Version\\/([\\d.]+).*Safari");

    private static final Pattern sWholeURL =
            Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)[a-z0-9-]+(\\.[a-z0-9-]+)|(:[0-9]{1,5})+([/?].*)?$");

    private static final Pattern sURL =
            Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

    private static final Pattern sFileURL = Pattern.compile(
            "^((https|http|ftp|rtsp|mms)?://)"  //https、http、ftp、rtsp、mms
            + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
            + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184
            + "|" // 允许IP和DOMAIN（域名）
            + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.
            + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
            + "[a-z]{2,6})" // first level domain- .com or .museum
            + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
            + "((/?)|" // a slash isn't required if there is no file name
            + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$");

    private static final Pattern sIPv4 = Pattern.compile("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private TextUtils() {
    }

    /**
     * 判断字符串是否全是数字。
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        Matcher isNum = sPatternNumeric.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 解析浏览器的 User Agent 串。
     * @param userAgent
     * @return
     */
    public static JSONObject parseUserAgent(String userAgent) {
        String browserName = "Unknown";
        String osName = "Unknown";

        if (userAgent.contains("Firefox")) {
            browserName = "Firefox";
        }
        else if (userAgent.contains("WeChat")) {
            browserName = "WeChat";
        }
        else if (userAgent.contains("MicroMessenger")) {
            browserName = "WeChat";
        }
        else if (userAgent.contains("MetaSr")) {
            browserName = "Sougou";
        }
        else if (userAgent.contains("QQBrowser")) {
            browserName = "QQBrowser";
        }
        else if (userAgent.contains("MSIE")) {
            browserName = "MSIE";
        }
        else if (userAgent.contains("Edge")) {
            browserName = "Edge";
        }
        else if (userAgent.contains("Presto")) {
            browserName = "Opera";
        }
        else if (userAgent.contains("Chrome")) {
            browserName = "Chrome";
        }
        else if (sBrowserNameSafari.matcher(userAgent).find()) {
            browserName = "Safari";
        }

        if (userAgent.contains("iPhone")) {
            osName = "iPhone";
        }
        else if (userAgent.contains("iPad")) {
            osName = "iPad";
        }
        else if (userAgent.contains("Android")) {
            osName = "Android";
        }
        else if (userAgent.contains("Windows")) {
            osName = "Windows";
        }
        else if (userAgent.contains("Macintosh")) {
            osName = "Mac";
        }

        JSONObject json = new JSONObject();
        json.put("browserName", browserName);
        json.put("osName", osName);
        return json;
    }

    /**
     * 度量文本区域面积。
     * @param text
     * @param constraint
     * @param wordSpacing
     * @param lineSpacing
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Size measureTextAreas(String text, TextConstraint constraint,
                                         int wordSpacing, int lineSpacing,
                                         int maxWidth, int maxHeight) {
        Size areas = new Size(maxWidth, maxHeight);

        int row = 1;
        int col = 0;
        // 解析文本行列数量
        int numChar = 0;
        for (int i = 0, len = text.length(); i < len; ++i) {
            char c = text.charAt(i);
            if (c == '\n') {
                ++row;
                if (numChar > col) {
                    col = numChar;
                }
                numChar = 0;
            }
            else {
                ++numChar;
            }
        }
        if (col == 0 || col < numChar) {
            col = numChar;
        }

        int ps = constraint.pointSize;

        // 字符总宽度 + 总字符间距
        areas.width = ps * col + wordSpacing * (col - 1);

        // 字符总高度 + 总行间距
        areas.height = ps * row + lineSpacing * (row - 1);

        // 上下和左右都各留一个字符的 Padding
        areas.width += ps + ps;
        areas.height += ps + ps;

        if (areas.width > maxWidth) {
            areas.width = maxWidth;
        }

        if (areas.height > maxHeight) {
            areas.height = maxHeight;
        }

        return areas;
    }

    /**
     * 是否是 URL 格式。
     *
     * @param string
     * @return
     */
    public static boolean isURL(String string) {
        Matcher matcher = sWholeURL.matcher(string);
        return matcher.find();
    }

    /**
     * 是否是 IPv4 格式。
     *
     * @param string
     * @return
     */
    public static boolean isIPv4(String string) {
        Matcher matcher = sIPv4.matcher(string);
        return matcher.find();
    }

    /**
     * 提取 URL 的域名信息。
     *
     * @param url
     * @return
     */
    public static String extractDomain(String url) {
        Pattern pattern = Pattern.compile("[^//]*?\\.(com|cn|net|org|biz|info|cc|tv|ai|io|tw|hk|mo|edu|gov|int|mil|pro|name|museum|coop|aero)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        else {
            pattern = Pattern.compile("[^(http|ftp|https)://](([a-zA-Z0-9._-]+)|([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}))(([a-zA-Z]{2,6})|(:[0-9]{1,4})?)");
            matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group();
            }
        }

        return url;
    }

    /**
     * 提取文本里的所有 URL 链接。
     *
     * @param text
     * @return
     */
    public synchronized static List<String> extractAllURLs(String text) {
        List<String> list = new ArrayList<>();
        Matcher matcher = sURL.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            if (isURL(url)) {
                list.add(url);
            }
        }
        return list;
    }

    public static void main(String[] args) {
//        String[] data = {
//                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1",
//                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Safari/605.1.15"
//        };
//
//        for (String ua : data) {
//            JSONObject result = TextUtils.parseUserAgent(ua);
//            System.out.println("----------------------------------------");
//            System.out.println(result.toString(4));
//        }

        String[] data = {
                "http://www.news.cn/politics/leaders/2023-06/09/c_1129683180.htm",
                "https://github.com/shixincube/cube-server",
                "https://v26-web.douyinvod.com/4e1fc24a1b0137951fc477d4742c2603/64841a3f/video/tos/cn/tos-cn-ve-15c001-alinc2/oQTgo4C9VA8B2pnDAwg8VKrfQbQekFDB1huzQA/?a=6383&ch=5&cr=3&dr=0&lr=all&cd=0%7C0%7C0%7C3&cv=1&br=1858&bt=1858&cs=0&ds=6&ft=GN7rKGVVywIiRZm8Zmo~xj7ScoAp7cE06vrKEdFGcto0g3&mime_type=video_mp4&qs=1&rc=ODQ2OTdoOzg4ODc4NWdoZEBpM2R2dWY6ZjQ0ajMzNGkzM0AvMC4yYDQxNi4xNWMuMS80YSNyMF9ycjRfa2hgLS1kLS9zcw%3D%3D&l=20230610133343084F39F2113AF95FAE3C&btag=e00030000",
                "http://192.168.9.173/?t=9876",
                "http://baidu/?t=9876"
        };
        for (String url : data) {
            System.out.println("URL: " + TextUtils.isURL(url));
        }
        System.out.println("----------------------------------------");
        for (String url : data) {
            String domain = TextUtils.extractDomain(url);
            System.out.println(domain + " - IP: " + TextUtils.isIPv4(domain));
        }

//        List<String> simData = new ArrayList<>();
//        for (String url : data) {
//            StringBuilder buf = new StringBuilder("这个链接是什么内容");
//            buf.append(Utils.randomString(Utils.randomInt(3, 10)));
//            buf.append(url);
//            buf.append("\n疑似链接：");
//            buf.append(url);
//            simData.add(buf.toString());
//        }
//
//        for (String text : simData) {
//            System.out.println("----------------------------------------");
//            List<String> result = TextUtils.extractAllURLs(text);
//            for (String url : result) {
//                System.out.println(url);
//            }
//        }
    }
}
