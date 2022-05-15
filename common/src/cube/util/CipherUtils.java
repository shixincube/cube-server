/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 一般加解密实用函数库。
 */
public class CipherUtils {

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
            SecretKey secretKey = new SecretKeySpec(paddingKey(key), "AES");

            // AES 加密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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
            SecretKey secretKey = new SecretKeySpec(paddingKey(key), "AES");

            // AES 解密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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
