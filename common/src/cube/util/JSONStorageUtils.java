/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * JSON 文本与 MySQL 字段之间的读写适配工具。
 *
 * <p>MySQL 的字段对 JSON 的支持较弱，工程里统一将 JSON 以字符串形式写入 TEXT 字段。
 * 而存储层（参见 {@code cube.util.SQLUtils#spellInsert}）是拼接 SQL 语句执行的，
 * 字符串值只做了 {@code '} -> {@code ''} 的替换，<b>没有处理反斜杠</b>。
 * MySQL 的字符串字面量里 {@code \} 是转义符，因此 JSON 里的 {@code \"}、{@code \n}、
 * {@code \\} 会被 MySQL 吃掉一层，存进库里的文本就不再是合法 JSON。</p>
 *
 * <p>本工具统一收口“写库编码 / 读库解码”两件事：</p>
 * <ul>
 *     <li>写库 {@link #encode(String)}：把反斜杠加倍，抵消 MySQL 消费掉的那一层转义。</li>
 *     <li>读库 {@link #decodeObject(String)} / {@link #decodeArray(String)}：
 *     优先按标准 JSON 解析；解析失败时调用 {@link #repair(String)} 修复历史脏数据
 *     （未做编码就写入的旧记录）后再解析，修复不了才返回 {@code null}。</li>
 * </ul>
 *
 * <p>注意：本工具只做字符级替换，不使用 {@code String.replaceAll()}。
 * {@code replaceAll()} 的替换串里 {@code \} 和 {@code $} 有特殊含义，用于纯字符串替换是误用。</p>
 */
public final class JSONStorageUtils {

    private JSONStorageUtils() {
    }

    /**
     * 将 JSON 文本编码为可安全写入 MySQL 字符串字面量的文本。
     *
     * @param jsonText 指定 JSON 文本。
     * @return 返回编码后的文本。入参为 {@code null} 时返回 {@code null}。
     */
    public static String encode(String jsonText) {
        if (null == jsonText) {
            return null;
        }

        // 反斜杠加倍，抵消 MySQL 字符串字面量的转义
        return jsonText.replace("\\", "\\\\");
    }

    /**
     * 将 JSON 对象编码为可安全写入 MySQL 字符串字面量的文本。
     *
     * @param json 指定 JSON 对象。
     * @return 返回编码后的文本。入参为 {@code null} 时返回 {@code null}。
     */
    public static String encode(JSONObject json) {
        return (null != json) ? encode(json.toString()) : null;
    }

    /**
     * 将 JSON 数组编码为可安全写入 MySQL 字符串字面量的文本。
     *
     * @param json 指定 JSON 数组。
     * @return 返回编码后的文本。入参为 {@code null} 时返回 {@code null}。
     */
    public static String encode(JSONArray json) {
        return (null != json) ? encode(json.toString()) : null;
    }

    /**
     * 将数据库里读出的文本解码为 JSON 对象。
     *
     * @param text 指定数据库字段文本。
     * @return 返回 JSON 对象。文本为空或无法解析时返回 {@code null}。
     */
    public static JSONObject decodeObject(String text) {
        String jsonText = normalize(text);
        if (null == jsonText) {
            return null;
        }

        try {
            return new JSONObject(jsonText);
        } catch (JSONException e) {
            // 历史数据可能未做编码就写入，尝试修复
            String repaired = repair(jsonText);
            try {
                JSONObject result = new JSONObject(repaired);
                Logger.w(JSONStorageUtils.class, "#decodeObject - Repaired dirty JSON data");
                return result;
            } catch (JSONException re) {
                Logger.w(JSONStorageUtils.class, "#decodeObject - Can NOT parse JSON data: " + jsonText);
                return null;
            }
        }
    }

    /**
     * 将数据库里读出的文本解码为 JSON 数组。
     *
     * @param text 指定数据库字段文本。
     * @return 返回 JSON 数组。文本为空或无法解析时返回 {@code null}。
     */
    public static JSONArray decodeArray(String text) {
        String jsonText = normalize(text);
        if (null == jsonText) {
            return null;
        }

        try {
            return new JSONArray(jsonText);
        } catch (JSONException e) {
            // 历史数据可能未做编码就写入，尝试修复
            String repaired = repair(jsonText);
            try {
                JSONArray result = new JSONArray(repaired);
                Logger.w(JSONStorageUtils.class, "#decodeArray - Repaired dirty JSON data");
                return result;
            } catch (JSONException re) {
                Logger.w(JSONStorageUtils.class, "#decodeArray - Can NOT parse JSON data: " + jsonText);
                return null;
            }
        }
    }

    /**
     * 修复被 MySQL 反转义破坏的 JSON 文本。
     *
     * <p>仅针对“写入时未做编码”的历史数据，尽可能还原为合法 JSON：</p>
     * <ul>
     *     <li>把裸的控制字符（换行、回车、制表符等）还原成 JSON 转义序列；</li>
     *     <li>把不构成合法 JSON 转义序列的反斜杠加倍。</li>
     * </ul>
     *
     * <p>说明：如果旧数据的引号转义（{@code \"}）已经被 MySQL 吃掉，
     * 丢失的信息无法还原，此时修复后的文本依然不是合法 JSON。</p>
     *
     * @param text 指定待修复的文本。
     * @return 返回修复后的文本。
     */
    public static String repair(String text) {
        StringBuilder buf = new StringBuilder(text.length() + 16);

        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if ('\\' != c) {
                appendChar(buf, c);
                continue;
            }

            if (i + 1 >= text.length()) {
                // 末尾孤立的反斜杠，补一个
                buf.append("\\\\");
                break;
            }

            char next = text.charAt(i + 1);
            if (isValidEscapeChar(next)) {
                // 合法的 JSON 转义序列，保持原样
                buf.append(c).append(next);
                ++i;
            }
            else {
                // 不是合法的 JSON 转义序列，说明这是一个真实的反斜杠字符，需要加倍
                buf.append("\\\\");
            }
        }

        return buf.toString();
    }

    /**
     * 判断指定字符是否可以作为 JSON 转义序列的第二个字符。
     *
     * @param c 指定字符。
     * @return 是合法转义字符返回 {@code true}。
     */
    private static boolean isValidEscapeChar(char c) {
        switch (c) {
            case '"':
            case '\\':
            case '/':
            case 'b':
            case 'f':
            case 'n':
            case 'r':
            case 't':
            case 'u':
                return true;
            default:
                return false;
        }
    }

    /**
     * 将字符按 JSON 规范写入缓存，裸控制字符转为转义序列。
     *
     * @param buf 指定缓存。
     * @param c 指定字符。
     */
    private static void appendChar(StringBuilder buf, char c) {
        switch (c) {
            case '\b':
                buf.append("\\b");
                break;
            case '\t':
                buf.append("\\t");
                break;
            case '\n':
                buf.append("\\n");
                break;
            case '\f':
                buf.append("\\f");
                break;
            case '\r':
                buf.append("\\r");
                break;
            default:
                if (c < 0x20) {
                    buf.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                }
                else {
                    buf.append(c);
                }
                break;
        }
    }

    /**
     * 规整数据库读出的文本。
     *
     * @param text 指定文本。
     * @return 返回去除首尾空白后的文本，空文本返回 {@code null}。
     */
    private static String normalize(String text) {
        if (null == text) {
            return null;
        }

        String result = text.trim();
        return result.isEmpty() ? null : result;
    }
}
