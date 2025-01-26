/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 一般加解密实用函数库。
 */
public class CipherUtils {

    private final static String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    private final static String KEY_ALGORITHM = "AES";

    private final static byte[] sPaddingBytes = new byte[]{
            's', 'h', 'i', 'x', 'i', 'n', 'c', 'u', 'b', 'e', '.', 'c', 'o', 'm', '#', '#',
            'x', 'u', 'j', 'i', 'a', 'n', 'g', 'w', 'e', 'i', '@', 's', 'p', 'a', 'p', '#'
    };

    private CipherUtils() {
    }

    /**
     * 加密。
     *
     * @param plaintext
     * @param key
     * @return
     */
    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        byte[] ciphertext = null;

        try {
            SecretKey secretKey = getSecretKey(key);

            // AES 加密
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            ciphertext = cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return ciphertext;
    }

    /**
     * 解密。
     *
     * @param ciphertext
     * @param key
     * @return
     */
    public static byte[] decrypt(byte[] ciphertext, byte[] key) {
        byte[] plaintext = null;

        try {
            SecretKey secretKey = getSecretKey(key);

            // AES 解密
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            plaintext = cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return plaintext;
    }

    private static SecretKeySpec getSecretKey(byte[] key) {
        KeyGenerator kg = null;

        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(key);
            // AES 要求密钥长度为 128
            kg.init(128, secureRandom);
            // 生成一个密钥
            SecretKey secretKey = kg.generateKey();
            // 转换为AES专用密钥
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        } catch (Exception e) {
            Logger.e(CipherUtils.class, "#getSecretKey");
        }

        return null;
    }

    private static byte[] paddingKey(byte[] key) {
        byte[] result = new byte[16];
        for (int i = 0; i < 16; ++i) {
            if (key.length > i) {
                result[i] = key[i];
            }
            else {
                result[i] = sPaddingBytes[i];
            }
        }
        return result;
    }
}
