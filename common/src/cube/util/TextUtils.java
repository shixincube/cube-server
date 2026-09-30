/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.Utils;
import cube.aigc.ModelConfig;
import cube.common.entity.AIGCUnit;
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

    private static final Pattern sDate = Pattern.compile("^\\d+年|月|日|号$");

    private static final Pattern sBrowserNameSafari = Pattern.compile("Version\\/([\\d.]+).*Safari");

    private static final Pattern sWholeURL =
            Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)[a-z0-9-]+(\\.[a-z0-9-]+)|(:[0-9]{1,5})+([/?].*)?$");

    private static final Pattern sURL =
            Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");


    private static final Pattern sIPv4 = Pattern.compile("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private final static Pattern sChinese =
            Pattern.compile("[\\u4E00-\\u9FA5|\\\\！|\\\\，|\\\\。|\\\\（|\\\\）|\\\\《|\\\\》|\\\\“|\\\\”|\\\\？|\\\\：|\\\\；|\\\\【|\\\\】]");
    private final static Pattern sChineseWord =
            Pattern.compile("[\\u4E00-\\u9FA5]");

    private final static Pattern sChinesePunctuation = Pattern.compile("[\\\\！|\\\\，|\\\\。|\\\\（|\\\\）|\\\\《|\\\\》|\\\\“|\\\\”|\\\\？|\\\\：|\\\\；|\\\\【|\\\\】]");

    private final static Pattern sEnglishPunctuation = Pattern.compile("[\\\\!|\\\\,|\\\\.|\\\\(|\\\\)|\\\\<|\\\\>|\\\\?|\\\\:|\\\\;]");

    private final static Pattern sEnglish = Pattern.compile("^[a-zA-Z]*$");

    /**
     * 中文冒号。
     */
    public final static String gColonInChinese = "：";

    /**
     * 英文冒号。
     */
    public final static String gColonInEnglish = ":";

    /**
     * 中文句号。
     */
    public final static String gPeriodInChinese = "。";

    /**
     * 英文句号。
     */
    public final static String gPeriodInEnglish = ".";

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
     * 判断字符串是否全是数字。
     * @param character
     * @return
     */
    public static boolean isNumeric(char character) {
        return isNumeric(String.valueOf(character));
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

    /**
     * 文本省略。
     *
     * @param text
     * @param limit
     * @return
     */
    public static String ellipsis(String text, int limit) {
        if (text.length() > limit) {
            return text.substring(0, limit - 1) + "...";
        }
        else {
            return text;
        }
    }

    public static String ellipsisURL(String url, int limit) {
        String result = url.toLowerCase();
        if (result.startsWith("https")) {
            result = result.substring(8);
        }
        else {
            result = result.substring(7);
        }

        if (result.length() > limit) {
            result = result.substring(0, limit - 6) + "..." + result.substring(result.length() - 6);
        }

        return result;
    }

    /**
     * 判断短字符串是否是日期信息字符串。
     *
     * @param value
     * @return
     */
    public static boolean isDateString(String value) {
        Matcher matcher = sDate.matcher(value);
        boolean result = matcher.find();
        if (result) {
            return true;
        }

        String v2 = TextUtils.convChineseToArabicNumerals(value);
        matcher = sDate.matcher(v2);
        return matcher.find();
    }

    /**
     * 将中文的数字字符转为阿拉伯数字字符。例如："二"转为"2"。
     *
     * @param value
     * @return
     */
    public static String convChineseToArabicNumerals(String value) {
        String result = value.replaceAll("十一", "11")
                .replaceAll("十二", "12")
                .replaceAll("一", "1")
                .replaceAll("二", "2")
                .replaceAll("三", "3")
                .replaceAll("四", "4")
                .replaceAll("五", "5")
                .replaceAll("六", "6")
                .replaceAll("七", "7")
                .replaceAll("八", "8")
                .replaceAll("九", "9")
                .replaceAll("零", "0")
                .replaceAll("十", "10");
        return result;
    }

    /**
     * 判断指定文本中是否包含中文标点符号。
     *
     * @param text
     * @return
     */
    public static boolean containsChinesePunctuation(String text) {
        boolean result = false;
        for (int i = 0; i < text.length(); ++i) {
            String s = text.substring(i, i + 1);
            Matcher m = sChinesePunctuation.matcher(s);
            if (m.matches()) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 判断指定文本中是否包含英文标点符号。
     *
     * @param text
     * @return
     */
    public static boolean containsEnglishPunctuation(String text) {
        boolean result = false;
        for (int i = 0; i < text.length(); ++i) {
            String s = text.substring(i, i + 1);
            Matcher m = sEnglishPunctuation.matcher(s);
            if (m.matches()) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 过滤所有标点符号。
     *
     * @param text
     * @return
     */
    public static String filterPunctuation(String text) {
        String result = text.replaceAll(sEnglishPunctuation.pattern(), "");
        return result.replaceAll(sChinesePunctuation.pattern(), "");
    }

    /**
     * 指定字符串是否是中文内容，包括中文标点符号。
     *
     * @param value
     * @return
     */
    public static boolean containsChinese(String value) {
        boolean chinese = false;

        for (int i = 0; i < value.length(); ++i) {
            String s = value.substring(i, i + 1);
            Matcher m = sChinese.matcher(s);
            if (m.matches()) {
                chinese = true;
                break;
            }
        }

        return chinese;
    }

    /**
     * 是否是中文文字。
     *
     * @param word
     * @return
     */
    public static boolean isChineseWord(String word) {
        boolean result = false;
        for (int i = 0; i < word.length(); ++i) {
            String w = word.substring(i, i + 1);
            Matcher m = sChineseWord.matcher(w);
            if (m.matches()) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 文本内容是否以英文为主。
     *
     * @param text
     * @return
     */
    public static boolean isTextMainlyInEnglish(String text) {
        // 按照空格进行分割
        String[] words = text.split(" ");
        int englishCounts = 0;
        int chineseCounts = 0;
        for (String word : words) {
            if (word.length() > 1) {
                for (int i = 0; i < word.length(); ++i) {
                    String w = word.substring(i, i + 1);
                    Matcher cm = sChineseWord.matcher(w);
                    Matcher em = sEnglish.matcher(w);
                    if (cm.matches()) {
                        ++chineseCounts;
                    }
                    else if (em.matches()) {
                        ++englishCounts;
                    }
                }
            }
            else {
                Matcher cm = sChineseWord.matcher(word);
                Matcher em = sEnglish.matcher(word);
                if (cm.matches()) {
                    ++chineseCounts;
                }
                else if (em.matches()) {
                    ++englishCounts;
                }
            }
        }
        return englishCounts > (chineseCounts + chineseCounts);
    }

    /**
     * 修正中文字符之间的空格。
     *
     * @param input
     * @return
     */
    public static String fixChineseBlank(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, len = input.length(); i < len; ++i) {
            String w = input.substring(i, i + 1);
            result.append(w);

            Matcher m = sChineseWord.matcher(w);
            boolean matches = m.matches();

            if (matches && (i + 1) < len) {
                String n = input.substring(i + 1, i + 2);
                if (n.equals(" ")) {
                    i = i + 1;
                }
            }
        }
        return result.toString();
    }

    /**
     * 是否是日文内容。
     *
     * @param input
     * @return
     */
    public static boolean isJapanese(String input) {
        try {
            if (containsChinese(input)) {
                return false;
            }

            return input.getBytes("shift-jis").length >= (2 * input.length());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否是词。
     *
     * @param word
     * @return
     */
    public static boolean isWord(String word) {
        boolean result = false;
        for (int i = 0; i < word.length(); ++i) {
            char c = word.charAt(0);
            if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90)
                || (c >= 97 && c <= 122)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 删除文本里的所有标点符号。
     *
     * @param text
     * @return
     */
    public static String removePunctuations(String text) {
        return text.replaceAll("\\p{P}", "");
    }

    /**
     * 判断文本的最后一个字符是否是标点符号。
     *
     * @param text
     * @return
     */
    public static boolean isLastPunctuationMark(String text) {
        String last = text.substring(text.length() - 1);
        if (removePunctuations(last).length() == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 文本是否以数字符号开头。
     * 例如："1."开头的字符串。
     *
     * @param text
     * @return
     */
    public static boolean startsWithNumberSign(String text) {
        if (text.startsWith("1.") || text.startsWith("2.") || text.startsWith("3.") ||
            text.startsWith("4.") || text.startsWith("5.") || text.startsWith("6.") ||
            text.startsWith("7.") || text.startsWith("8.") || text.startsWith("9.") ||
            text.startsWith("10.") || text.startsWith("11.")|| text.startsWith("12.") ||
            text.startsWith("13.") || text.startsWith("14.")|| text.startsWith("15.")) {
            return true;
        }

        return false;
    }

    /**
     * 将长句切割为短句。
     *
     * @param sentence
     * @return
     */
    public static List<String> splitSentence(String sentence) {
        List<String> result = new ArrayList<>();
        String[] array = sentence.split("\n");
        for (String text : array) {
            String content = text.replaceAll("\n", "").trim();
            if (content.length() == 0) {
                continue;
            }

            String[] contentArray = content.split("。");
            for (String str : contentArray) {
                if (str.length() == 0) {
                    continue;
                }
                result.add(str.trim() + "。");
            }
        }
        return result;
    }

    /**
     * 按照指定长度限制分拆列表。
     * 每个列表里的字符串总长度不会超过指定限制。
     *
     * @param list
     * @param limit
     * @return
     */
    public static List<List<String>> splitList(List<String> list, int limit) {
        List<List<String>> result = new ArrayList<>();
        int length = 0;
        List<String> tmpList = new ArrayList<>();
        for (String text : list) {
            if (length > limit) {
                result.add(tmpList);
                tmpList = new ArrayList<>();
                length = 0;
            }

            tmpList.add(text);
            length += text.length();
        }
        result.add(tmpList);
        return result;
    }

    public static String filterChinese(AIGCUnit unit, String text) {
        if (unit.getCapability().getName().equalsIgnoreCase(ModelConfig.BAIZE_UNIT)) {
            if (TextUtils.containsChinese(text)) {
                return text.replaceAll(",", "，");
            }
            else {
                return text;
            }
        }
        else {
            return text;
        }
    }

    public static boolean isBlank(String source) {
        if (null == source || source.trim().length() == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 提取 Markdown 格式的文本内容。
     *
     * @param markdown
     * @return
     */
    public static String extractMarkdownText(String markdown) {
        StringBuilder buf = new StringBuilder();

        List<String> list = extractMarkdownTextAsList(markdown);
        for (String text : list) {
            buf.append(text).append("\n");
        }

        return buf.toString();
    }

    /**
     * 按行提取 Markdown 格式的文本内容。
     *
     * @param markdown
     * @return
     */
    public static List<String> extractMarkdownTextAsList(String markdown) {
        List<String> result = new ArrayList<>();

        String[] list = markdown.split("\n");
        for (String text : list) {
            String content = text.trim();
            int index = 0;

            content = content.replaceAll("\\*\\*", "").trim();

//            if (content.startsWith("**") || content.endsWith("**")) {
//                content = content.replaceAll("\\*\\*", "").trim();
//            }
            if (content.startsWith("#") || content.startsWith("*") || content.startsWith("-")) {
                index = content.indexOf(" ") + 1;
            }
            else if (startsWithNumberSign(content)) {
                index = content.indexOf(" ") + 1;
            }

            if (index > 0) {
                content = content.substring(index);
            }

            if (content.length() > 1) {
                result.add(content);
            }
        }

        return result;
    }

    /**
     * 提取 Markdown 格式的表格。
     *
     * @param text
     * @return
     */
    public static String extractMarkdownTable(String text) {
        String[] lines = text.split("\n");

        int index = -1;
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (line.length() < 2) {
                continue;
            }
            String tl = line.trim();
            if ((tl.startsWith("|----") && tl.endsWith("----|")) ||
                    (tl.startsWith("| ----") && tl.endsWith("---- |"))) {
                index = i;
                break;
            }
        }

        if (index <= 0) {
            return null;
        }

        index -= 1;
        StringBuilder buf = new StringBuilder();
        for (int i = index; i < lines.length; ++i) {
            String line = lines[i];
            if (line.length() < 2) {
                continue;
            }
            String tl = line.trim();
            if (tl.startsWith("|") && tl.endsWith("|")) {
                if (tl.contains("----")) {
                    continue;
                }

                String[] items = tl.split("\\|");
                for (String item : items) {
                    if (item.length() == 0) {
                        continue;
                    }
                    buf.append(item.trim()).append(",");
                }
                buf.delete(buf.length() - 1, buf.length());
                buf.append("\n");
            }
            else {
                break;
            }
        }

        return buf.toString();
    }

    /**
     * 提取文本里的第一个年份数据，如果没有找到返回 <code>0</code> 值。
     *
     * @param text
     * @return 如果提取失败返回 <code>0</code> 值。
     */
    public static int extractYear(String text) {
        int index = text.indexOf("年");
        if (index <= 0) {
            return 0;
        }
        // 倒序输出年份字符串
        StringBuffer buf = new StringBuffer();
        int count = 0;
        for (int i = index - 1; i >= 0; --i) {
            if (text.charAt(i) == ' ') {
                continue;
            }

            if (TextUtils.isNumeric(text.charAt(i))) {
                buf.append(text.charAt(i));
            }
            ++count;
            if (buf.length() > 4 || count > 4) {
                // 数字串超长
                return 0;
            }
        }
        if (buf.length() == 0) {
            return 0;
        }
        if (buf.length() == 2) {
            buf.append("02");
        }
        try {
            return Integer.parseInt(buf.reverse().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 提取文本里的第一个月份数据，如果没有找到返回 <code>0</code> 值。
     *
     * @param text
     * @return 如果提取失败返回 <code>0</code> 值。
     */
    public static int extractMonth(String text) {
        int index = text.indexOf("月");
        if (index <= 0) {
            return 0;
        }
        // 倒序输出年份字符串
        StringBuffer buf = new StringBuffer();
        int count = 0;
        for (int i = index - 1; i >= 0; --i) {
            if (text.charAt(i) == ' ') {
                continue;
            }

            if (TextUtils.isNumeric(text.charAt(i))) {
                buf.append(text.charAt(i));
            }
            ++count;
            if (buf.length() > 2 || count > 2) {
                // 数字串超长
                return 0;
            }
        }
        try {
            if (buf.length() == 0) {
                return 0;
            }
            int result = Integer.parseInt(buf.reverse().toString());
            if (result <= 12) {
                return result;
            }
            else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 提取文本里的第一个日数据，如果没有找到返回 <code>0</code> 值。
     *
     * @param text
     * @return 如果提取失败返回 <code>0</code> 值。
     */
    public static int extractDay(String text) {
        int index = text.indexOf("日");
        if (index <= 0) {
            index = text.indexOf("号");
        }
        StringBuffer buf = new StringBuffer();
        int count = 0;
        for (int i = index - 1; i >= 0; --i) {
            if (text.charAt(i) == ' ') {
                continue;
            }

            if (TextUtils.isNumeric(text.charAt(i))) {
                buf.append(text.charAt(i));
            }
            ++count;
            if (buf.length() > 2 || count > 2) {
                // 数字串超长
                return 0;
            }
        }

        // 翻转
        buf = buf.reverse();

        if (buf.length() == 0) {
            index = text.indexOf("月");
            count = 0;
            for (int i = index + 1; i < text.length(); ++i) {
                if (text.charAt(i) == ' ') {
                    continue;
                }

                if (TextUtils.isNumeric(text.charAt(i))) {
                    buf.append(text.charAt(i));
                }
                ++count;
                if (buf.length() > 2 || count > 2) {
                    // 数字串超长
                    return 0;
                }
            }
        }

        if (buf.length() == 0) {
            return 0;
        }

        try {
            int result = Integer.parseInt(buf.toString());
            if (result <= 31) {
                return result;
            }
            else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 提取文本内的可能的 SN 或 ID 数字。如果没有找到返回 <code>0</code> 值。
     *
     * @param sentences
     * @return
     */
    public static long extractSnOrId(List<String> sentences) {
        for (String word : sentences) {
            if (isNumeric(word) && word.length() > 4) {
                return Long.parseLong(word);
            }
        }
        return 0;
    }

    /**
     * 提取文本里描述的位置信息。如果没有找到返回 <code>0</code> 值。
     *
     * @param sentences
     * @return
     */
    public static int extractLocation(List<String> sentences) {
        String num = "0";
        List<String> tmp = new ArrayList<>();
        for (int i = 0; i < sentences.size(); ++i) {
            String sentence = sentences.get(i);
            if (sentence.equals("第")) {
                if (i + 1 < sentences.size()) {
                    num = sentences.get(i + 1);
                    break;
                }
            }
            else if (sentence.contains("最近")) {
                return 1;
            }
            tmp.add(TextUtils.convChineseToArabicNumerals(sentence));
        }

        if (num.equals("0")) {
            int index = -1;
            StringBuilder buf = new StringBuilder();
            for (String text : tmp) {
                index = text.indexOf("第");
                if (index >= 0) {
                    for (int i = index + 1; i < text.length(); ++i) {
                        if (TextUtils.isNumeric(text.charAt(i))) {
                            buf.append(text.charAt(i));
                        }
                    }
                }
                if (buf.length() > 0) {
                    break;
                }
            }
            num = buf.toString();
        }

        try {
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String extractEmailAccountName(String email) {
        int index = email.indexOf("@");
        if (index > 0) {
            return email.substring(0, index);
        }
        else {
            return email;
        }
    }

    /**
     * 提取提示词模板里的标签。
     *
     * @param text
     * @return
     */
    public static List<String> extractPromptTemplateTag(String text) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        int start = 0;
        int end = 0;
        while (pos >= 0) {
            start = text.indexOf("{{", pos);
            end = text.indexOf("}}", pos);
            if (start < 0 || end < 0) {
                break;
            }
            result.add(text.substring(start, end + 2));
            pos = end + 2;
        }
        return result;
    }

    public static List<String> randomSplitString(String text) {
        List<String> textList = new ArrayList<>();
        int length = text.length();
        int seed = 26 - Utils.randomInt(3, 10);
        int index = 0;
        while (length > 0) {
            int floor = Math.max(5, seed - 5);
            int len = Utils.randomInt(floor, floor + 5);
            len = Math.min(len, length);
            textList.add(text.substring(index, index + len));
            index += len;
            length -= len;
        }
        return textList;
    }

    public static String clip(String content) {
        String value = "";
        if (null != content && content.length() > 1) {
            int end = Math.min((int)Math.ceil((double) content.length() * 0.2), 20);
            value = content.substring(0, end);
        }
        StringBuilder buf = new StringBuilder(value);
        buf.append("...");
        return buf.toString();
    }

    public static boolean containsString(String string, String[] candidates) {
        String lowerCaseString = string.toLowerCase();
        for (String word : candidates) {
            if (lowerCaseString.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {



//        String[] dateValue = new String[] {
//                "2023年",
//                "6月",
//                "25日",
//                "30号",
//                "8月份",
//                "九月",
//                "6月3日",
//                "过年",
//                "2023初",
//                "年份2023",
//        };
//        for (String value : dateValue) {
//            System.out.println(value + " : " + TextUtils.isDateString(value));
//        }

//        String[] dateValue = new String[] {
//                "23年",
//                "2025年",
//                "这是来自24年的祝福",
//                "查看2025年4月的",
//                "年份2023",
//                "过年",
//                "今夕是何年",
//                "这是一句无关日期的文本"
//        };
//        for (String value : dateValue) {
//            System.out.println(TextUtils.extractYear(value));
//        }

//        String[] monthDateValue = new String[] {
//                "2月",
//                "12月",
//                "这是来自9月的祝福",
//                "查看2025年4月的",
//                "月份10",
//                "月子",
//                "今夕是何月",
//                "这是一句无关日期的文本"
//        };
//        for (String value : monthDateValue) {
//            System.out.println(TextUtils.extractMonth(value));
//        }

//        String[] dayDateValue = new String[] {
//                "2日",
//                "12号",
//                "这是来自10月17日的祝福",
//                "查看2025年4月3号的",
//                "日期是8月10",
//                "日子",
//                "今夕是何日",
//                "这是一句无关日期的文本"
//        };
//        for (String value : dayDateValue) {
//            System.out.println(TextUtils.extractDay(value));
//        }


//        System.out.println(TextUtils.extractEmailAccountName("xjw@163.com"));






        String text = "\"\"\"\n测试文本：{{文本内容}}\n\"\"\"\n\n{{我是谁}} # 标题\n";
        List<String> result = extractPromptTemplateTag(text);
        for (String tag : result) {
            System.out.println(tag);
        }
    }
}
