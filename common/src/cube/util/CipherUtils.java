/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 一般加解密实用函数库。
 *
 * <h2>密文格式</h2>
 * <p>当前使用的格式（版本 1）：</p>
 * <pre>
 * | MAGIC(4) | VERSION(1) | SALT(16) | IV(12) | AES-GCM 密文 + 认证标签(16) |
 * </pre>
 * <p>盐与初始化向量每次都随机生成并随密文一同输出，因此相同的明文与口令
 * 也会产生完全不同的密文，且具备篡改检测能力（认证标签）。</p>
 *
 * <h2>向后兼容</h2>
 * <p>历史版本使用 {@code AES/ECB/PKCS5Padding}，并以
 * {@code SHA1PRNG + setSeed(key)} 派生密钥。该格式没有版本头，
 * {@link #decrypt(byte[], byte[])} 会通过魔数自动识别：命中魔数走 AES-GCM，
 * 否则回退到 {@link #decryptLegacy(byte[], byte[])} 兼容路径，因此<b>存量密文无需迁移即可继续解密</b>。</p>
 *
 * <h2>失败语义</h2>
 * <p>为保持与历史实现一致的调用契约，加解密失败时记录错误日志并返回 {@code null}，
 * <b>调用方必须判空</b>。</p>
 */
public class CipherUtils {

    /**
     * 密钥算法。
     */
    private final static String KEY_ALGORITHM = "AES";

    /**
     * 当前使用的加密变换。
     */
    private final static String GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * 历史版本使用的加密变换，仅用于解密存量密文与生成兼容密文。
     */
    private final static String LEGACY_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * 密钥派生函数。
     */
    private final static String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 派生密钥长度（比特）。
     */
    private final static int KEY_BITS = 128;

    /**
     * 密钥派生迭代次数。
     */
    private final static int KDF_ITERATIONS = 10000;

    /**
     * 随机盐长度（字节）。
     */
    private final static int SALT_LENGTH = 16;

    /**
     * 初始化向量长度（字节）。GCM 推荐 12 字节。
     */
    private final static int IV_LENGTH = 12;

    /**
     * 认证标签长度（比特）。
     */
    private final static int TAG_LENGTH_BITS = 128;

    /**
     * 认证标签长度（字节）。
     */
    private final static int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;

    /**
     * 密文魔数，ASCII "CUBE"。
     */
    private final static byte[] MAGIC = new byte[] { 0x43, 0x55, 0x42, 0x45 };

    /**
     * 当前密文格式版本号。
     */
    private final static byte FORMAT_VERSION = 0x01;

    /**
     * 头部长度：魔数 + 版本号 + 盐 + 初始化向量。
     */
    private final static int HEADER_LENGTH = MAGIC.length + 1 + SALT_LENGTH + IV_LENGTH;

    /**
     * 随机数发生器，{@link SecureRandom} 是线程安全的。
     */
    private final static SecureRandom RANDOM = new SecureRandom();

    private CipherUtils() {
    }

    /**
     * 加密。
     *
     * <p>输出格式见类注释。与历史实现不同，本方法不再使用 ECB 模式，
     * 每次调用都会生成新的随机盐与初始化向量。</p>
     *
     * @param plaintext 明文。
     * @param key 口令，任意长度的字节序列。
     * @return 返回密文，失败时返回 {@code null} 。
     */
    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        if (null == plaintext) {
            Logger.e(CipherUtils.class, "#encrypt - The plaintext is null");
            return null;
        }

        if (null == key || 0 == key.length) {
            Logger.e(CipherUtils.class, "#encrypt - The key is empty");
            return null;
        }

        try {
            byte[] salt = new byte[SALT_LENGTH];
            RANDOM.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            byte[] sealed = doFinal(Cipher.ENCRYPT_MODE, plaintext, key, salt, iv);

            byte[] result = new byte[HEADER_LENGTH + sealed.length];
            int offset = 0;
            System.arraycopy(MAGIC, 0, result, offset, MAGIC.length);
            offset += MAGIC.length;
            result[offset++] = FORMAT_VERSION;
            System.arraycopy(salt, 0, result, offset, salt.length);
            offset += salt.length;
            System.arraycopy(iv, 0, result, offset, iv.length);
            offset += iv.length;
            System.arraycopy(sealed, 0, result, offset, sealed.length);

            return result;
        } catch (GeneralSecurityException e) {
            Logger.e(CipherUtils.class, "#encrypt - Failed to encrypt with AES/GCM", e);
            return null;
        }
    }

    /**
     * 解密。
     *
     * <p>自动识别密文格式：带版本头（魔数）的密文使用 AES/GCM 解密，
     * 其余按历史 {@code AES/ECB} 格式解密，因此存量数据无需迁移。</p>
     *
     * @param ciphertext 密文。
     * @param key 口令。
     * @return 返回明文，失败时返回 {@code null} 。
     */
    public static byte[] decrypt(byte[] ciphertext, byte[] key) {
        if (null == ciphertext || 0 == ciphertext.length) {
            Logger.e(CipherUtils.class, "#decrypt - The ciphertext is empty");
            return null;
        }

        if (null == key || 0 == key.length) {
            Logger.e(CipherUtils.class, "#decrypt - The key is empty");
            return null;
        }

        if (!isVersioned(ciphertext)) {
            Logger.w(CipherUtils.class, "#decrypt - Legacy ciphertext detected, "
                    + "falling back to AES/ECB compatibility path. Please re-issue the data.");
            return decryptLegacy(ciphertext, key);
        }

        try {
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(ciphertext, MAGIC.length + 1, salt, 0, SALT_LENGTH);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(ciphertext, MAGIC.length + 1 + SALT_LENGTH, iv, 0, IV_LENGTH);

            byte[] sealed = new byte[ciphertext.length - HEADER_LENGTH];
            System.arraycopy(ciphertext, HEADER_LENGTH, sealed, 0, sealed.length);

            return doFinal(Cipher.DECRYPT_MODE, sealed, key, salt, iv);
        } catch (GeneralSecurityException e) {
            Logger.e(CipherUtils.class, "#decrypt - Failed to decrypt with AES/GCM", e);
            return null;
        }
    }

    /**
     * 按历史格式加密，仅用于生成兼容密文（迁移与回归验证），<b>不得用于新数据</b>。
     *
     * @param plaintext 明文。
     * @param key 口令。
     * @return 返回历史格式密文，失败时返回 {@code null} 。
     */
    public static byte[] encryptLegacy(byte[] plaintext, byte[] key) {
        if (null == plaintext || null == key || 0 == key.length) {
            Logger.e(CipherUtils.class, "#encryptLegacy - Invalid argument");
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(LEGACY_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getLegacySecretKey(key));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            Logger.e(CipherUtils.class, "#encryptLegacy - Failed to encrypt", e);
            return null;
        }
    }

    /**
     * 按历史格式解密，仅用于解密存量密文。
     *
     * @param ciphertext 历史格式密文。
     * @param key 口令。
     * @return 返回明文，失败时返回 {@code null} 。
     */
    public static byte[] decryptLegacy(byte[] ciphertext, byte[] key) {
        if (null == ciphertext || null == key || 0 == key.length) {
            Logger.e(CipherUtils.class, "#decryptLegacy - Invalid argument");
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(LEGACY_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getLegacySecretKey(key));
            return cipher.doFinal(ciphertext);
        } catch (BadPaddingException e) {
            // 口令错误或密文损坏
            Logger.e(CipherUtils.class, "#decryptLegacy - Bad padding, the key may be incorrect");
            return null;
        } catch (GeneralSecurityException e) {
            Logger.e(CipherUtils.class, "#decryptLegacy - Failed to decrypt", e);
            return null;
        }
    }

    /**
     * 判断密文是否带当前版本头。
     *
     * @param data 密文。
     * @return 带版本头返回 {@code true} 。
     */
    private static boolean isVersioned(byte[] data) {
        if (data.length < HEADER_LENGTH + TAG_LENGTH_BYTES) {
            return false;
        }

        for (int i = 0; i < MAGIC.length; ++i) {
            if (data[i] != MAGIC[i]) {
                return false;
            }
        }

        return data[MAGIC.length] == FORMAT_VERSION;
    }

    /**
     * 执行 AES/GCM 加解密。
     *
     * @param mode {@link Cipher#ENCRYPT_MODE} 或 {@link Cipher#DECRYPT_MODE} 。
     * @param data 输入数据。
     * @param key 口令。
     * @param salt 盐。
     * @param iv 初始化向量。
     * @return 返回处理结果。
     * @throws GeneralSecurityException 加解密失败时抛出。
     */
    private static byte[] doFinal(int mode, byte[] data, byte[] key, byte[] salt, byte[] iv)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
        cipher.init(mode, deriveKey(key, salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        return cipher.doFinal(data);
    }

    /**
     * 使用 PBKDF2 派生密钥。
     *
     * <p>历史实现使用 {@code SecureRandom("SHA1PRNG").setSeed(key)} 派生密钥，
     * 该做法等价于把随机源替换为确定性函数，密钥空间远小于口令空间。
     * 这里改用标准的 PBKDF2 密钥派生函数。</p>
     *
     * <p>口令以 {@code ISO-8859-1} 解码为字符序列，保证任意字节序列都能
     * 无损且确定地映射为字符（{@code UTF-8} 对非法字节序列会替换为占位字符而丢失信息）。</p>
     *
     * @param key 口令。
     * @param salt 盐。
     * @return 返回 AES 专用密钥。
     * @throws GeneralSecurityException 派生失败时抛出。
     */
    private static SecretKeySpec deriveKey(byte[] key, byte[] salt) throws GeneralSecurityException {
        String password = new String(key, StandardCharsets.ISO_8859_1);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(encoded, KEY_ALGORITHM);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 按历史方式派生密钥，仅用于兼容存量密文。
     *
     * @param key 口令。
     * @return 返回 AES 专用密钥，失败时返回 {@code null} 。
     */
    private static SecretKey getLegacySecretKey(byte[] key) {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(key);
            // AES 要求密钥长度为 128
            kg.init(128, secureRandom);
            // 生成一个密钥
            SecretKey secretKey = kg.generateKey();
            // 转换为 AES 专用密钥
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(CipherUtils.class, "#getLegacySecretKey", e);
        }

        return null;
    }
}
