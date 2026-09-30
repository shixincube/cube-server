/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.test;

import cell.core.talk.LiteralBase;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.util.CipherUtils;
import cube.util.SQLUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 第一批安全加固的验证用例。
 *
 * <p>本类不依赖 JUnit（工程未引入测试框架），以 {@code main} 方式运行，
 * 全部通过时退出码为 0，否则为 1。</p>
 *
 * <p>位于 {@code cube/test} 包下，{@code build.xml} 已将其排除在产物 jar 之外。</p>
 *
 * <p>运行方式：</p>
 * <pre>
 * javac -encoding UTF-8 -source 1.8 -target 1.8 -sourcepath common/src \
 *       -cp deploy/bin/cell.jar -d &lt;out&gt; common/src/cube/test/TestSqlAndCipher.java
 * java -cp &lt;out&gt;:deploy/bin/cell.jar cube.test.TestSqlAndCipher
 * </pre>
 */
public class TestSqlAndCipher {

    private static int passed = 0;

    private static int failed = 0;

    public static void main(String[] args) {
        testCorrectString();
        testIdentifier();
        testSpellSelect();
        testSpellInsert();
        testWriteBoundaries();
        testCreateTable();
        testConditionalValueRendering();
        testCreateLike();
        testCreateIN();
        testCipherRoundTrip();
        testCipherRandomized();
        testCipherLegacyCompatibility();
        testCipherTamperAndFailures();
        testStorageFieldImmutability();

        System.out.println();
        System.out.println("========================================");
        System.out.println("passed: " + passed + ", failed: " + failed);
        System.out.println("========================================");

        System.exit(failed > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // SQLUtils
    // ------------------------------------------------------------------

    private static void testCorrectString() {
        section("SQLUtils.correctString");

        assertEquals("escape single quote", "o''brien", SQLUtils.correctString("o'brien"));
        // 顺序关键：必须先转义反斜杠，否则 "\'" 中的反斜杠会吃掉后面的引号
        assertEquals("escape backslash before quote", "a\\\\b''c", SQLUtils.correctString("a\\b'c"));
        assertEquals("escape injection payload", "\\\\'' OR 1=1 -- ",
                SQLUtils.correctString("\\' OR 1=1 -- "));
        assertEquals("plain text untouched", "plain", SQLUtils.correctString("plain"));
        assertEquals("null safe", null, SQLUtils.correctString(null));
    }

    private static void testIdentifier() {
        section("SQLUtils identifier whitelist");

        assertEquals("dot and dash normalized", "contact_shixincube_com",
                SQLUtils.correctTableName("contact_shixincube.com"));

        check("valid identifier accepted", SQLUtils.isValidIdentifier("contact_domain1"));
        check("dash rejected", !SQLUtils.isValidIdentifier("a-b"));
        check("backtick rejected", !SQLUtils.isValidIdentifier("a`b"));
        check("space rejected", !SQLUtils.isValidIdentifier("a b"));
        check("null rejected", !SQLUtils.isValidIdentifier(null));

        // 域名称等外部输入可能被直接拼进表名，这里必须被收敛到安全字符集
        String hostile = SQLUtils.correctTableName("t`; DROP TABLE x --");
        check("hostile name sanitized to a valid identifier: " + hostile,
                SQLUtils.isValidIdentifier(hostile));
        check("hostile name keeps no backtick", hostile.indexOf('`') < 0);
        check("hostile name keeps no semicolon", hostile.indexOf(';') < 0);
        check("hostile name keeps no space", hostile.indexOf(' ') < 0);
        check("hostile name keeps no dash", hostile.indexOf('-') < 0);
    }

    private static void testSpellSelect() {
        section("SQLUtils.spellSelect");

        assertEquals("no conditionals", "SELECT * FROM `t`", SQLUtils.spellSelect("t", null, null));
        // 历史实现在空数组上会因 conditionals[0] 抛 ArrayIndexOutOfBoundsException
        assertEquals("empty conditional array", "SELECT * FROM `t`",
                SQLUtils.spellSelect("t", null, new Conditional[0]));
        assertEquals("all-null conditional array", "SELECT * FROM `t`",
                SQLUtils.spellSelect("t", null, new Conditional[] { null }));

        String sql = SQLUtils.spellSelect("t", new StorageField[] {
                        new StorageField("id", LiteralBase.LONG),
                        new StorageField("name", LiteralBase.STRING) },
                new Conditional[] { Conditional.createEqualTo("id", 5L) });
        assertEquals("with fields and condition", "SELECT `id`,`name` FROM `t` WHERE `id`=5 ", sql);

        // 表名注入必须被中和
        assertEquals("hostile table name neutralized", "SELECT * FROM `t__DROP_TABLE_x__`",
                SQLUtils.spellSelect("t`;DROP TABLE x--", null, null));
    }

    private static void testSpellInsert() {
        section("SQLUtils.spellInsert");

        String sql = SQLUtils.spellInsert("t", new StorageField[] {
                new StorageField("id", 1L),
                new StorageField("name", "a"),
                new StorageField("flag", LiteralBase.BOOL, Boolean.TRUE)
        });
        assertEquals("normal insert", "INSERT INTO t (`id`,`name`,`flag`) VALUES (1,'a',1)", sql);

        String quoted = SQLUtils.spellInsert("t", new StorageField[] {
                new StorageField("name", "o'brien")
        });
        assertEquals("value escaped", "INSERT INTO t (`name`) VALUES ('o''brien')", quoted);

        String blank = SQLUtils.spellInsert("t", new StorageField[] {
                new StorageField("name", LiteralBase.STRING)
        });
        assertEquals("all values null returns null", null, blank);

        String emptyFields = SQLUtils.spellInsert("t", new StorageField[0]);
        assertEquals("empty fields returns null", null, emptyFields);

        String mixed = SQLUtils.spellInsert("t", new StorageField[] {
                new StorageField("id", 1L),
                new StorageField("name", LiteralBase.STRING)
        });
        assertEquals("null value columns skipped", "INSERT INTO t (`id`) VALUES (1)", mixed);
    }

    private static void testWriteBoundaries() {
        section("SQLUtils write boundaries");

        StorageField[] fields = new StorageField[] { new StorageField("name", "a") };
        Conditional[] conditions = new Conditional[] { Conditional.createEqualTo("id", 1L) };

        assertThrows("delete with null conditionals", () -> SQLUtils.spellDelete("t", null));
        assertThrows("delete with empty conditionals", () -> SQLUtils.spellDelete("t", new Conditional[0]));
        assertThrows("delete with all-null conditionals", () -> SQLUtils.spellDelete("t", new Conditional[] { null }));
        assertEquals("delete ok", "DELETE FROM t WHERE `id`=1 ", SQLUtils.spellDelete("t", conditions));

        assertThrows("update with null conditionals", () -> SQLUtils.spellUpdate("t", fields, null));
        assertThrows("update with empty conditionals", () -> SQLUtils.spellUpdate("t", fields, new Conditional[0]));
        assertEquals("update ok", "UPDATE t SET `name`='a' WHERE `id`=1 ",
                SQLUtils.spellUpdate("t", fields, conditions));

        assertEquals("update with no assignable field returns null", null,
                SQLUtils.spellUpdate("t", new StorageField[] { new StorageField("name", LiteralBase.STRING) },
                        conditions));
    }

    private static void testCreateTable() {
        section("SQLUtils.spellCreateTable");

        assertThrows("create with null fields", () -> SQLUtils.spellCreateTable("t", null));
        assertThrows("create with empty array", () -> SQLUtils.spellCreateTable("t", new StorageField[0]));

        String sql = SQLUtils.spellCreateTable("t", new StorageField[] {
                new StorageField("id", LiteralBase.LONG) });
        assertEquals("create ok", "CREATE TABLE IF NOT EXISTS t (`id` BIGINT )", sql);
    }

    // ------------------------------------------------------------------
    // Conditional
    // ------------------------------------------------------------------

    private static void testConditionalValueRendering() {
        section("Conditional value rendering");

        assertEquals("long value unquoted", "`ts`>100",
                Conditional.createGreaterThan(new StorageField("ts", 100L)).toString());
        assertEquals("int value unquoted", "`ts`<5",
                Conditional.createLessThan(new StorageField("ts", 5)).toString());
        assertEquals("bool value as 1", "`flag`>=1",
                Conditional.createGreaterThanEqual(
                        new StorageField("flag", LiteralBase.BOOL, Boolean.TRUE)).toString());
        // 历史实现对 STRING 类型既不加引号也不转义
        assertEquals("string value quoted", "`ts`>'2026-01-01'",
                Conditional.createGreaterThan(new StorageField("ts", "2026-01-01")).toString());
        assertEquals("string value escaped", "`ts`<'a''b'",
                Conditional.createLessThan(new StorageField("ts", "a'b")).toString());
        assertEquals("equal-to escaped", "`n`='o''brien'",
                Conditional.createEqualTo(new StorageField("n", "o'brien")).toString());
        assertEquals("unequal-to escaped", "`n`<>'o''brien'",
                Conditional.createUnequalTo(new StorageField("n", "o'brien")).toString());

        assertThrows("equal-to with null value",
                () -> Conditional.createEqualTo(new StorageField("n", LiteralBase.STRING)));
        assertThrows("greater-than with null value",
                () -> Conditional.createGreaterThan(new StorageField("n", LiteralBase.STRING)));
    }

    private static void testCreateLike() {
        section("Conditional.createLike");

        assertEquals("wildcards escaped", "`name` LIKE '%50\\%\\_off%' ESCAPE '\\\\'",
                Conditional.createLike("name", "50%_off").toString());
        assertEquals("quote escaped", "`name` LIKE '%o''brien%' ESCAPE '\\\\'",
                Conditional.createLike("name", "o'brien").toString());
        assertEquals("backslash escaped", "`name` LIKE '%a\\\\b%' ESCAPE '\\\\'",
                Conditional.createLike("name", "a\\b").toString());
        assertEquals("null keyword", "`name` LIKE '%%' ESCAPE '\\\\'",
                Conditional.createLike("name", null).toString());
    }

    private static void testCreateIN() {
        section("Conditional.createIN");

        assertEquals("long in", "`id` IN (1,2)",
                Conditional.createIN(new StorageField("id", LiteralBase.LONG),
                        new Long[] { 1L, 2L }).toString());
        assertEquals("string in escaped", "`n` IN ('a','b''c')",
                Conditional.createIN(new StorageField("n", LiteralBase.STRING),
                        new String[] { "a", "b'c" }).toString());

        assertThrows("in with empty values",
                () -> Conditional.createIN(new StorageField("id", LiteralBase.LONG), new Long[0]));
        assertThrows("in with null element",
                () -> Conditional.createIN(new StorageField("id", LiteralBase.LONG), new Long[] { 1L, null }));
    }

    // ------------------------------------------------------------------
    // CipherUtils
    // ------------------------------------------------------------------

    private static void testCipherRoundTrip() {
        section("CipherUtils round trip");

        byte[] key = "shixincube.com".getBytes(StandardCharsets.UTF_8);

        byte[] plaintext = pattern(200);
        byte[] ciphertext = CipherUtils.encrypt(plaintext, key);
        check("ciphertext produced", null != ciphertext);
        check("ciphertext longer than plaintext (header + tag)", ciphertext.length > plaintext.length);

        byte[] decrypted = CipherUtils.decrypt(ciphertext, key);
        check("round trip restores plaintext", Arrays.equals(plaintext, decrypted));

        byte[] shortText = "{\"domain\":\"first-prototype-box\"}".getBytes(StandardCharsets.UTF_8);
        check("short round trip", Arrays.equals(shortText,
                CipherUtils.decrypt(CipherUtils.encrypt(shortText, key), key)));

        check("empty plaintext round trip",
                Arrays.equals(new byte[0], CipherUtils.decrypt(CipherUtils.encrypt(new byte[0], key), key)));
    }

    private static void testCipherRandomized() {
        section("CipherUtils randomized IV and salt");

        byte[] key = "shixincube.com".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = pattern(64);

        byte[] first = CipherUtils.encrypt(plaintext, key);
        byte[] second = CipherUtils.encrypt(plaintext, key);

        check("same plaintext yields different ciphertext (ECB removed)", !Arrays.equals(first, second));
        check("first decrypts", Arrays.equals(plaintext, CipherUtils.decrypt(first, key)));
        check("second decrypts", Arrays.equals(plaintext, CipherUtils.decrypt(second, key)));
    }

    private static void testCipherLegacyCompatibility() {
        section("CipherUtils legacy compatibility");

        byte[] key = "shixincube.com".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = pattern(120);

        // 模拟存量数据：由历史算法产生的密文，必须能被 decrypt 自动识别并解出
        byte[] legacy = CipherUtils.encryptLegacy(plaintext, key);
        check("legacy ciphertext produced", null != legacy);

        byte[] decrypted = CipherUtils.decrypt(legacy, key);
        check("decrypt() auto-falls back to legacy path", Arrays.equals(plaintext, decrypted));

        check("decryptLegacy() works directly", Arrays.equals(plaintext,
                CipherUtils.decryptLegacy(CipherUtils.encryptLegacy(plaintext, key), key)));

        byte[] modern = CipherUtils.encrypt(plaintext, key);
        check("modern ciphertext differs from legacy", !Arrays.equals(modern, legacy));
        check("modern decrypts via new path", Arrays.equals(plaintext, CipherUtils.decrypt(modern, key)));
    }

    private static void testCipherTamperAndFailures() {
        section("CipherUtils tamper detection and failure semantics");

        byte[] key = "shixincube.com".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = pattern(64);
        byte[] ciphertext = CipherUtils.encrypt(plaintext, key);

        // 篡改密文：GCM 认证标签必须检出
        byte[] tampered = ciphertext.clone();
        tampered[tampered.length - 1] ^= 0x01;
        check("tampered ciphertext rejected", null == CipherUtils.decrypt(tampered, key));

        byte[] wrongKey = "wrong-password".getBytes(StandardCharsets.UTF_8);
        check("wrong key rejected", null == CipherUtils.decrypt(ciphertext, wrongKey));

        check("null ciphertext", null == CipherUtils.decrypt(null, key));
        check("empty ciphertext", null == CipherUtils.decrypt(new byte[0], key));
        check("null key", null == CipherUtils.decrypt(ciphertext, null));
        check("empty key", null == CipherUtils.decrypt(ciphertext, new byte[0]));
        check("garbage ciphertext", null == CipherUtils.decrypt(new byte[] { 1, 2, 3 }, key));
        check("null plaintext", null == CipherUtils.encrypt(null, key));
        check("empty key on encrypt", null == CipherUtils.encrypt(plaintext, new byte[0]));
    }

    // ------------------------------------------------------------------
    // StorageField 不可变性
    // ------------------------------------------------------------------

    private static void testStorageFieldImmutability() {
        section("StorageField immutability");

        // 1) 就地修改方法应已全部移除
        check("no setValue(Object)", !hasDeclaredMethod("setValue", Object.class));
        check("no setConstraints(Constraint[])", !hasDeclaredMethod("setConstraints", Constraint[].class));
        check("no resetLiteralBase(LiteralBase)", !hasDeclaredMethod("resetLiteralBase", LiteralBase.class));

        // 2) 所有实例字段均为 final
        boolean allFinal = true;
        int fieldCount = 0;
        for (Field f : StorageField.class.getDeclaredFields()) {
            if (f.isSynthetic()) {
                continue;
            }
            ++fieldCount;
            if (!Modifier.isFinal(f.getModifiers())) {
                allFinal = false;
            }
        }
        check("all instance fields are final (count=" + fieldCount + ")", allFinal && 5 == fieldCount);

        // 3) 构造器拷贝入参数组，getConstraints() 返回内部数组的副本
        Constraint[] source = new Constraint[] { Constraint.NOT_NULL, Constraint.UNIQUE };
        StorageField field = new StorageField("id", LiteralBase.INT, source);
        source[0] = Constraint.PRIMARY_KEY;
        Constraint[] first = field.getConstraints();
        first[1] = Constraint.CHECK;
        Constraint[] second = field.getConstraints();
        check("constructor copies the constraint array", Constraint.NOT_NULL == second[0]);
        check("getConstraints() returns a copy", Constraint.UNIQUE == second[1]);
        check("getConstraints() returns a distinct array", first != second);

        // 4) 未设置约束时返回 null
        check("null constraints when not set",
                null == new StorageField("a", LiteralBase.STRING).getConstraints());

        // 5) 全参构造器保留全部属性
        StorageField full = new StorageField("t", "n", LiteralBase.LONG, 1L,
                new Constraint[] { Constraint.DEFAULT_0 });
        assertEquals("full constructor: name", "n", full.getName());
        assertEquals("full constructor: tableName", "t", full.getTableName());
        check("full constructor: literalBase", LiteralBase.LONG == full.getLiteralBase());
        assertEquals("full constructor: constraint", Constraint.DEFAULT_0, full.getConstraints()[0]);

        // 6) long 不再被降级为 int（历史上 SQLite 建表会就地改写 literalBase）
        StorageField big = new StorageField("n", 4000000000L);
        assertEquals("long value survives", 4000000000L, big.getLong());
        check("LONG literal kept", LiteralBase.LONG == big.getLiteralBase());

        // 7) 拼装建表语句不会改写入参
        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, new Constraint[] { Constraint.AUTO_INCREMENT }),
                new StorageField("name", LiteralBase.STRING)
        };
        SQLUtils.spellCreateTable("demo", fields);
        check("spellCreateTable keeps literalBase", LiteralBase.LONG == fields[0].getLiteralBase());
        check("spellCreateTable keeps constraints",
                Constraint.AUTO_INCREMENT == fields[0].getConstraints()[0]);
    }

    private static boolean hasDeclaredMethod(String name, Class<?>... parameterTypes) {
        try {
            StorageField.class.getDeclaredMethod(name, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 基础设施
    // ------------------------------------------------------------------

    /**
     * 生成指定长度的确定性测试数据。
     *
     * @param length 长度。
     * @return 返回测试数据。
     */
    private static byte[] pattern(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; ++i) {
            data[i] = (byte) (i % 251);
        }
        return data;
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("-- " + name);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            ++passed;
            System.out.println("  [PASS] " + name);
        }
        else {
            ++failed;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        boolean same = (null == expected) ? (null == actual) : expected.equals(actual);
        if (same) {
            ++passed;
            System.out.println("  [PASS] " + name);
        }
        else {
            ++failed;
            System.out.println("  [FAIL] " + name);
            System.out.println("         expected: " + expected);
            System.out.println("         actual  : " + actual);
        }
    }

    private static void assertThrows(String name, Runnable action) {
        try {
            action.run();
            ++failed;
            System.out.println("  [FAIL] " + name + " (expected IllegalArgumentException, but nothing was thrown)");
        } catch (IllegalArgumentException e) {
            ++passed;
            System.out.println("  [PASS] " + name);
        } catch (Throwable t) {
            ++failed;
            System.out.println("  [FAIL] " + name + " (expected IllegalArgumentException, got "
                    + t.getClass().getName() + ")");
        }
    }
}
