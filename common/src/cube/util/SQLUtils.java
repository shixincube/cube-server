/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;

import java.util.regex.Pattern;

/**
 * SQL 辅助函数。
 *
 * <p><b>安全约定</b>：本类通过字符串拼接构造 SQL，因此承担两道职责——</p>
 * <ol>
 *   <li><b>表名</b>一律经 {@link #correctTableName(String)} 收敛到
 *       {@link #FILTER_PATTERN} 白名单字符集内，阻断来自域名称等外部输入的标识符注入；</li>
 *   <li><b>值</b>一律经 {@link #correctString(String)} 转义（反斜杠与单引号双重转义），
 *       再按类型包裹引号。</li>
 * </ol>
 *
 * <p>本类不做字段名的白名单校验：字段名全部来自代码内字面量，且会被反引号包裹。
 * 若后续字段名引入外部输入，必须补充同等强度的校验。</p>
 */
public final class SQLUtils {

    public final static String Quote = "`";

    /**
     * 标识符（表名）安全字符白名单。
     *
     * <p>仅允许 ASCII 字母、数字、下划线、美元符号与中日韩统一表意文字。
     * 白名单之外的字符（如反引号、空格、分号、注释符）一律不允许出现在标识符中。</p>
     */
    public final static String FILTER_PATTERN = "^[A-Za-z0-9_$\\u4e00-\\u9fa5]+$";

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(FILTER_PATTERN);

    private SQLUtils() {
    }

    /**
     * 判断指定字符串是否是安全的 SQL 标识符。
     *
     * @param name 待校验的标识符。
     * @return 符合白名单返回 {@code true} 。
     */
    public static boolean isValidIdentifier(String name) {
        return (null != name) && IDENTIFIER_PATTERN.matcher(name).matches();
    }

    /**
     * 判断指定字符是否属于标识符白名单字符集。
     *
     * @param c 待判断的字符。
     * @return 允许返回 {@code true} 。
     */
    private static boolean isAllowedIdentifierChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        if ('_' == c || '$' == c) {
            return true;
        }
        // 中日韩统一表意文字
        return (c >= '\u4e00' && c <= '\u9fa5');
    }

    /**
     * 将任意字符串净化为安全标识符：白名单之外的字符一律替换为下划线。
     *
     * <p>与"发现非法字符即抛异常"相比，本方法保证<b>输出必然安全</b>，
     * 不会因为某个部署环境中出现了特殊的域名称而导致服务不可用。</p>
     *
     * @param name 原始字符串。
     * @return 净化后的标识符，入参为 {@code null} 时返回 {@code null} 。
     */
    public static String sanitizeIdentifier(String name) {
        if (null == name) {
            return null;
        }

        StringBuilder buf = new StringBuilder(name.length());
        int replaced = 0;

        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (isAllowedIdentifierChar(c)) {
                buf.append(c);
            }
            else {
                buf.append('_');
                ++replaced;
            }
        }

        if (replaced > 0) {
            Logger.w(SQLUtils.class, "#sanitizeIdentifier - Illegal identifier characters replaced: "
                    + name + " -> " + buf.toString());
        }

        return buf.toString();
    }

    /**
     * 矫正表名。
     *
     * <p>先把 {@code .} 与 {@code -} 归一为下划线（保持历史行为），
     * 再收敛到标识符白名单字符集，确保返回值可以直接拼进 SQL。</p>
     *
     * @param name 原始表名。
     * @return 矫正后的表名，入参为 {@code null} 时返回 {@code null} 。
     */
    public static String correctTableName(String name) {
        if (null == name) {
            return null;
        }

        String result = name.replace('.', '_').replace('-', '_');
        // 收敛到白名单，阻断域名称等外部输入污染 SQL 标识符
        return sanitizeIdentifier(result);
    }

    /**
     * 矫正 SQL 字符串值。
     *
     * <p>转义顺序不可颠倒：必须<b>先转义反斜杠，再转义单引号</b>。
     * 否则输入 {@code \'} 会被处理成 {@code \''} ，其中的反斜杠仍会吃掉后面的引号，
     * 形成注入点。</p>
     *
     * @param string 原始字符串。
     * @return 转义后的字符串，入参为 {@code null} 时返回 {@code null} 。
     */
    public static String correctString(String string) {
        if (null == string) {
            return null;
        }

        String result = string.replace("\\", "\\\\");
        result = result.replace("'", "''");
        return result;
    }

    /**
     * 输出 WHERE 句式。
     *
     * @param buf 输出缓冲区。
     * @param conditionals 条件数组，允许为 {@code null} 或空数组（表示无 WHERE 句式）。
     */
    private static void appendWhere(StringBuilder buf, Conditional[] conditionals) {
        if (null == conditionals || conditionals.length == 0) {
            return;
        }

        // 取第一个有效条件决定是否需要 WHERE 关键字
        Conditional first = null;
        for (Conditional cond : conditionals) {
            if (null != cond) {
                first = cond;
                break;
            }
        }

        if (null == first) {
            return;
        }

        if (first.needWhereSentence()) {
            buf.append(" WHERE ");
        }
        else {
            buf.append(" ");
        }

        for (Conditional cond : conditionals) {
            if (null == cond) {
                continue;
            }

            buf.append(cond.toString()).append(" ");
        }
    }

    /**
     * 校验条件数组，缺失条件时拒绝构造 SQL。
     *
     * <p>UPDATE / DELETE 缺少条件意味着全表改写，属于编程错误，必须显式失败
     * 而不是生成 {@code ... WHERE} 这样的非法语句（历史上这类语句会被数据库拒绝，
     * 但错误信息难以定位）。</p>
     *
     * @param table 表名，仅用于错误提示。
     * @param method 方法名，仅用于错误提示。
     * @param conditionals 条件数组。
     * @throws IllegalArgumentException 条件数组为 {@code null} 、空数组或全部元素为 {@code null} 时抛出。
     */
    private static void assertConditionals(String table, String method, Conditional[] conditionals) {
        if (null == conditionals || conditionals.length == 0) {
            throw new IllegalArgumentException(method + " - Missing conditions, refuse to build SQL for table: " + table);
        }

        for (Conditional conditional : conditionals) {
            if (null != conditional) {
                return;
            }
        }

        throw new IllegalArgumentException(method + " - All conditions are null, refuse to build SQL for table: " + table);
    }

    /**
     * 按字段类型追加 SQL 字面量。
     *
     * @param buf 输出缓冲区。
     * @param field 字段描述。
     */
    private static void appendLiteral(StringBuilder buf, StorageField field) {
        LiteralBase literal = field.getLiteralBase();

        if (LiteralBase.INT == literal) {
            buf.append(field.getInt());
        }
        else if (LiteralBase.LONG == literal) {
            buf.append(field.getLong());
        }
        else if (LiteralBase.BOOL == literal) {
            buf.append(field.getBoolean() ? 1 : 0);
        }
        else {
            // 其余类型（含 STRING 以及历史实现未覆盖的 FLOAT / DOUBLE / JSON 等）一律按字符串转义后加引号，
            // 避免出现"什么都不输出"导致 SQL 语法错误
            buf.append("'").append(correctString(field.getString())).append("'");
        }
    }

    /**
     * 拼装 SELECT 语句。
     *
     * @param table 表名。
     * @param fields 字段列表，{@code null} 或空数组表示查询全部字段。
     * @param conditionals 条件列表，允许为 {@code null} 或空数组。
     * @return 返回 SELECT 语句。
     */
    public static String spellSelect(String table, StorageField[] fields, Conditional[] conditionals) {
        StringBuilder buf = new StringBuilder("SELECT ");

        if (null != fields && fields.length > 0) {
            for (StorageField field : fields) {
                buf.append(Quote).append(field.getName()).append(Quote);
                buf.append(",");
            }
            buf.delete(buf.length() - 1, buf.length());
        }
        else {
            buf.append("*");
        }

        buf.append(" FROM ");
        buf.append(Quote);
        buf.append(correctTableName(table));
        buf.append(Quote);

        appendWhere(buf, conditionals);

        return buf.toString();
    }

    /**
     * 拼装多表 SELECT 语句。
     *
     * @param tables 表名数组。
     * @param fields 字段列表，{@code null} 或空数组表示查询全部字段。
     * @param conditionals 条件列表，允许为 {@code null} 或空数组。
     * @return 返回 SELECT 语句。
     * @throws IllegalArgumentException 表名数组为 {@code null} 或空数组时抛出。
     */
    public static String spellSelect(String[] tables, StorageField[] fields, Conditional[] conditionals) {
        if (null == tables || tables.length == 0) {
            throw new IllegalArgumentException("#spellSelect - Empty tables, refuse to build SQL");
        }

        StringBuilder buf = new StringBuilder("SELECT ");

        if (null != fields && fields.length > 0) {
            for (StorageField field : fields) {
                buf.append(Quote).append(field.getTableName()).append(Quote)
                        .append(".").append(Quote).append(field.getName()).append(Quote);
                buf.append(",");
            }
            buf.delete(buf.length() - 1, buf.length());
        }
        else {
            buf.append("*");
        }

        buf.append(" FROM ");
        for (int i = 0; i < tables.length; ++i) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(correctTableName(tables[i]));
        }

        appendWhere(buf, conditionals);

        return buf.toString();
    }

    /**
     * 拼装 CREATE TABLE 语句。
     *
     * @param table 表名。
     * @param fields 字段列表。
     * @return 返回 CREATE TABLE 语句。
     * @throws IllegalArgumentException 字段列表为 {@code null} 或空数组时抛出。
     */
    public static String spellCreateTable(String table, StorageField[] fields) {
        if (null == fields || fields.length == 0) {
            throw new IllegalArgumentException("#spellCreateTable - Empty fields, refuse to build SQL for table: " + table);
        }

        StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        buf.append(correctTableName(table));
        buf.append(" (");
        for (StorageField field : fields) {
            // 字段名
            buf.append(Quote).append(field.getName()).append(Quote);

            switch (field.getLiteralBase()) {
                case STRING:
                    buf.append(" TEXT ");
                    break;
                case INT:
                    buf.append(" INTEGER ");
                    break;
                case LONG:
                    buf.append(" BIGINT ");
                    break;
                case BOOL:
                    buf.append(" BOOLEAN ");
                    break;
                default:
                    break;
            }

            Constraint[] constraints = field.getConstraints();
            if (null != constraints) {
                for (Constraint constraint : constraints) {
                    buf.append(constraint.getStatement()).append(" ");
                }
            }

            buf.append(",");
        }

        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(")");

        return buf.toString();
    }

    /**
     * 拼装 INSERT 语句。
     *
     * <p>值为 {@code null} 的字段会被跳过。若字段列表为空或<b>所有字段值均为 {@code null}</b>，
     * 则返回 {@code null} —— 此时不存在任何合法的 INSERT 语句，
     * 调用方必须判空并按其失败语义处理（历史实现会拼出 {@code INSERT INTO t () VALUES ()} 这类非法语句）。</p>
     *
     * @param table 表名。
     * @param fields 字段列表。
     * @return 返回 INSERT 语句，无法构造时返回 {@code null} 。
     */
    public static String spellInsert(String table, StorageField[] fields) {
        if (null == fields || fields.length == 0) {
            Logger.w(SQLUtils.class, "#spellInsert - Empty fields, skip building SQL for table: " + table);
            return null;
        }

        StringBuilder names = new StringBuilder();
        StringBuilder values = new StringBuilder();
        int count = 0;

        for (StorageField field : fields) {
            if (null == field || null == field.getValue()) {
                // 跳过空值
                continue;
            }

            if (count > 0) {
                names.append(",");
                values.append(",");
            }

            names.append(Quote).append(field.getName()).append(Quote);
            appendLiteral(values, field);
            ++count;
        }

        if (0 == count) {
            Logger.w(SQLUtils.class, "#spellInsert - All field values are null, skip building SQL for table: " + table);
            return null;
        }

        return "INSERT INTO " + correctTableName(table)
                + " (" + names.toString() + ") VALUES (" + values.toString() + ")";
    }

    /**
     * 拼装 UPDATE 语句。
     *
     * @param table 表名。
     * @param fields 字段列表，值为 {@code null} 的字段会被跳过。
     * @param conditionals 条件列表，不允许为 {@code null} 或空数组。
     * @return 返回 UPDATE 语句，无有效赋值字段时返回 {@code null} 。
     * @throws IllegalArgumentException 条件列表缺失时抛出。
     */
    public static String spellUpdate(String table, StorageField[] fields, Conditional[] conditionals) {
        assertConditionals(table, "#spellUpdate", conditionals);

        if (null == fields || fields.length == 0) {
            Logger.w(SQLUtils.class, "#spellUpdate - Empty fields, skip building SQL for table: " + table);
            return null;
        }

        StringBuilder buf = new StringBuilder("UPDATE ");
        buf.append(correctTableName(table));
        buf.append(" SET ");

        int count = 0;
        for (StorageField field : fields) {
            if (null == field || null == field.getValue()) {
                // 跳过空值
                continue;
            }

            if (count > 0) {
                buf.append(",");
            }

            buf.append(Quote).append(field.getName()).append(Quote).append("=");
            appendLiteral(buf, field);
            ++count;
        }

        if (0 == count) {
            Logger.w(SQLUtils.class, "#spellUpdate - All field values are null, skip building SQL for table: " + table);
            return null;
        }

        buf.append(" WHERE ");
        for (Conditional conditional : conditionals) {
            if (null == conditional) {
                // 跳过 null 值
                continue;
            }

            buf.append(conditional.toString());
            buf.append(" ");
        }

        return buf.toString();
    }

    /**
     * 拼装 DELETE 语句。
     *
     * @param table 表名。
     * @param conditionals 条件列表，不允许为 {@code null} 或空数组。
     * @return 返回 DELETE 语句。
     * @throws IllegalArgumentException 条件列表缺失时抛出。
     */
    public static String spellDelete(String table, Conditional[] conditionals) {
        assertConditionals(table, "#spellDelete", conditionals);

        StringBuilder buf = new StringBuilder("DELETE FROM ");
        buf.append(correctTableName(table));
        buf.append(" WHERE ");
        for (Conditional conditional : conditionals) {
            if (null == conditional) {
                continue;
            }

            buf.append(conditional.toString());
            buf.append(" ");
        }

        return buf.toString();
    }
}
