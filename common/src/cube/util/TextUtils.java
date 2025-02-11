/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

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

    private final static Pattern sChinese =
            Pattern.compile("[\\u4E00-\\u9FA5|\\\\！|\\\\，|\\\\。|\\\\（|\\\\）|\\\\《|\\\\》|\\\\“|\\\\”|\\\\？|\\\\：|\\\\；|\\\\【|\\\\】]");
    private final static Pattern sChineseWord =
            Pattern.compile("[\\u4E00-\\u9FA5]");

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
     * 指定字符串是否是中文内容。
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
        if (unit.getCapability().getName().equalsIgnoreCase(ModelConfig.CHAT_UNIT)) {
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
     * 提取文本里的第一个年份数据，如果没有找到返回 <code>null</code> 值。
     *
     * @param text
     * @return
     */
    public static String extractYear(String text) {
        if (!TextUtils.isDateString(text)) {
            return null;
        }

        int index = text.indexOf("年");

        return null;
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

//        String[] data = {
//                "http://www.news.cn/politics/leaders/2023-06/09/c_1129683180.htm",
//                "https://github.com/shixincube/cube-server",
//                "https://v26-web.douyinvod.com/4e1fc24a1b0137951fc477d4742c2603/64841a3f/video/tos/cn/tos-cn-ve-15c001-alinc2/oQTgo4C9VA8B2pnDAwg8VKrfQbQekFDB1huzQA/?a=6383&ch=5&cr=3&dr=0&lr=all&cd=0%7C0%7C0%7C3&cv=1&br=1858&bt=1858&cs=0&ds=6&ft=GN7rKGVVywIiRZm8Zmo~xj7ScoAp7cE06vrKEdFGcto0g3&mime_type=video_mp4&qs=1&rc=ODQ2OTdoOzg4ODc4NWdoZEBpM2R2dWY6ZjQ0ajMzNGkzM0AvMC4yYDQxNi4xNWMuMS80YSNyMF9ycjRfa2hgLS1kLS9zcw%3D%3D&l=20230610133343084F39F2113AF95FAE3C&btag=e00030000",
//                "http://192.168.9.173/?t=9876",
//                "http://baidu/?t=9876"
//        };
//        for (String url : data) {
//            System.out.println("URL: " + TextUtils.isURL(url));
//        }
//        for (String url : data) {
//            System.out.println("URL: " + TextUtils.ellipsisURL(url, 24));
//        }
//        System.out.println("----------------------------------------");
//        for (String url : data) {
//            String domain = TextUtils.extractDomain(url);
//            System.out.println(domain + " - IP: " + TextUtils.isIPv4(domain));
//        }

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

        String[] dateValue = new String[] {
                "2023年",
                "6月",
                "25日",
                "30号",
                "8月份",
                "九月",
                "6月3日",
                "过年",
                "2023初",
                "年份2023",
        };
        for (String value : dateValue) {
            System.out.println(value + " : " + TextUtils.isDateString(value));
        }

//        String[] sentenceList = new String[] {
//                "很抱歉，作为一个人工智能助手，我没有实时获取汤臣倍健昨天的负面舆情数据的能力。同时，作为一个中立的信息来源，我也无法对任何特定的舆情数据进行评估或证实。舆情数据的真实性和准确性可能会受到多种因素的影响，包括数据采集的及时性、样本的选择、数据来源的可靠性等等。如果对汤臣倍健昨天的舆情数据有任何疑问或关注，建议关注相关的新闻报道、社交媒体评论等公开信息渠道，以了解实际情况。",
//                "目前，A股半年报披露已经拉开帷幕，数据显示，截至7月4日20点，A股共有96家上市公司披露2023年半年度业绩预告，其中78家预喜，占比近八成。其中，国内VDS领军企业汤臣倍健率先于7月3日发布2023年半年度业绩预告，成为A股首批披露2023年半年度业绩预告的公司。  预告称，2023年上半年归属于上市公司股东的净利润约为13.63亿元~15.72亿元，同比增长预计将达到30%~50%，盈利水平有望超过2021年中报峰值的13.71亿元。由此可见，在受行业景气度影响的当下，汤臣倍健整体仍旧保持了较好的业绩增长。据悉，在今年618期间，汤臣倍健旗下全品牌全网销售额就创下新高，总销售额突破8亿元，同比增长超40%。在京东、天猫发布的好评榜、热卖榜等众多榜单中，汤臣倍健蛋白粉、专业益生菌品牌“Life-Space”、专业婴童品牌天然博士、新锐女性美态管理品牌汤臣倍健yep、骨关节营养品牌健力多等多个品牌实现了销量及口碑双丰收。其中汤臣倍健蛋白粉继续稳居天猫免疫力蛋白粉品类TOP1、京东“蛋白粉金榜”TOP1；专业益生菌品牌“Life-Space”实现了整体销售额近1.6亿元，同比增长超45%，稳占天猫进口益生菌好评榜TOP1、京东热卖榜TOP1。此外，“有颜值、更有技术含量”的Yep -GAGs胶原蛋白肽新品出道即大热，也夺得了天猫“胶原蛋白V榜”第一名。  聚焦“新原料、新功能、新技术”，近几年汤臣倍健深入营养健康的科学研究，也取得了不少研究成果。比如获得了双项国家发明专利的含真皮重要成分GAGs的胶原蛋白肽、更容易定植于肠道的专利益生菌等，这些科研成果落地产品后，也随即成为了深受消费者喜爱的口碑爆品。欧睿数据显示，2022年中国维生素与膳食补充剂行业零售总规模为2001亿人民币，增速约为4.9%。汤臣倍健份额为10.3%，稳居第一，整体数据非常可观。而汤臣倍健业绩的公示也进一步向消费者证明了其综合实力，让大家吃了一颗定心丸。未来，汤臣倍健将持续坚持科学营养，拉开强科技的领先优势，稳坐龙头地位，为国人输送更多优质的膳食营养补充剂产品。"
//        };
//        List<String> sentences = TextUtils.splitSentence(sentenceList[1]);
//        for (String str : sentences) {
//            System.out.println(str);
//        }

//        String[] list = new String[] {
//                "1. 是数字符号",
//                " 2. 不是数字符号",
//                "3 不是数字符号"
//        };
//        for (String str : list) {
//            System.out.println(TextUtils.startsWithNumberSign(str));
//            if (TextUtils.startsWithNumberSign(str)) {
//                System.out.println(str.substring(2).trim());
//            }
//        }

//        String table = "| 序号 | 学号 | 姓名 | 出生日期 |\n|--------|--------|--------|--------|\n| 1 | 1781001 | 刘备 | 40485 |\n| 2 | 1781002 | 曹操 | 40402 |\n| 3 | 1781003 | 孙权 | 40473 |\n";
//        String data = "这是表格数据：\n\n" + table + "\n以上是表格信息";
//        String result = extractMarkdownTable(data);
//        System.out.println(result);
    }
}
