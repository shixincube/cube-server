/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件操作辅助函数。
 */
public final class FileUtils {

    private final static byte[] CHAR_TABLE = new byte[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static byte[] PADDING_TABLE = new byte[] {
            'Q', 'm', 'W', 'n', 'E', 'b', 'R', 'v', 'T', 'c', 'Y', 'x', 'U',
            'z', 'I', 'l', 'O', 'k', 'P', 'j', 'A', 'h', 'S', 'g', 'D', 'f',
            'F', 'd', 'G', 's', 'H', 'a', 'J', 'p', 'K', 'o', 'L', 'i', 'Z',
            'u', 'X', 'y', 'C', 't', 'V', 'r', 'B', 'e', 'N', 'w', 'M', 'q',
            'q', 'M', 'w', 'N', 'e', 'B', 'r', 'V', 't', 'C', 'y', 'X', 'u',
            'Z', 'i', 'L', 'o', 'K', 'p', 'J', 'a', 'H', 's', 'G', 'd', 'F',
            'f', 'D', 'g', 'S', 'h', 'A', 'j', 'P', 'k', 'O', 'l', 'I', 'z',
            'U', 'x', 'Y', 'c', 'T', 'v', 'R', 'b', 'E', 'n', 'W', 'm', 'Q',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private FileUtils() {
    }

    public static String makeFileCode(Long contactId, String fileName) {
        StringBuilder buf = new StringBuilder(contactId.toString());
        buf.append("_").append(fileName);

        // 补空位
        if (buf.length() < 64) {
            buf.append("_").append(contactId.toString());
        }

        String keystr = buf.toString();

        // 将 Key 串切割
        List<byte[]> list = FileUtils.slice(keystr.getBytes(Charset.forName("UTF-8")), 64);

        // Hash
        String code = FileUtils.fashHash(list);
        return code;
    }

    public static String makeStreamName(String token, String filename) {
        StringBuilder buf = new StringBuilder(token);
        return buf.append("_").append(filename).toString();
    }

    public static String[] extractStreamName(String streamName) {
        int index = streamName.indexOf("_");
        if (index > 0) {
            String token = streamName.substring(0, index);
            String filename = streamName.substring(index + 1, streamName.length());
            return new String[] { token, filename };
        }

        return null;
    }

    private static String fashHash(List<byte[]> bytes) {
        int seed = 13;
        int length = bytes.get(0).length;
        int[] hashCode = new int[length];

        for (int i = 0; i < bytes.size(); ++i) {
            // 逐行处理
            byte[] data = bytes.get(i);
            for (int n = 0; n < length; ++n) {
                byte b = data[n];
                hashCode[n] = hashCode[n] * seed + (b);
            }
        }

        // 查表
        StringBuilder buf = new StringBuilder();
        for (int code : hashCode) {
            int index = (code & 0x7FFFFFFF) % CHAR_TABLE.length;
            buf.append((char)CHAR_TABLE[index]);
        }

        return buf.toString();
    }

    private static List<byte[]> slice(byte[] source, int sliceLength) {
        List<byte[]> list = new ArrayList<>();
        if (source.length < sliceLength) {
            byte[] buf = new byte[sliceLength];
            System.arraycopy(PADDING_TABLE, 0, buf, 0, sliceLength);
            System.arraycopy(source, 0, buf, 0, source.length);
            list.add(buf);
        }
        else if (source.length > sliceLength) {
            int cursor = 0;
            int num = (int) Math.floor(source.length / sliceLength);
            for (int i = 0; i < num; ++i) {
                byte[] buf = new byte[sliceLength];
                System.arraycopy(source, cursor, buf, 0, sliceLength);
                list.add(buf);
                cursor += sliceLength;
            }

            int mod = source.length % sliceLength;
            byte[] buf = new byte[sliceLength];
            System.arraycopy(PADDING_TABLE, 0, buf, 0, sliceLength);
            System.arraycopy(source, cursor, buf, 0, mod);
            list.add(buf);
        }
        else {
            list.add(source);
        }
        return list;
    }

//    public static void main(String[] arg) {
//        System.out.println(FileUtils.makeFileCode(50001001L, "三周年纪念.png"));
//        System.out.println(FileUtils.makeFileCode(50001001L, "三周年纪念.jpg"));
//        System.out.println(FileUtils.makeFileCode(50002001L, "三周年纪念.png"));
//        System.out.println();
//        System.out.println(FileUtils.makeFileCode(2005179136L, "这个文件的文件名很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长.txt"));
//    }
}
