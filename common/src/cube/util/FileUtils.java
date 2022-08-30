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

import cube.common.entity.FileLabel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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

    private final static byte[] PADDING_TABLE = new byte[] {
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

    public final static long KB = 1024;
    public final static long MB = (long)1024 * KB;
    public final static long GB = (long)1024 * MB;
    public final static long TB = (long)1024 * GB;
    public final static long PB = (long)1024 * TB;

    private FileUtils() {
    }

    /**
     * 快速生成字符串 Hash 值。
     *
     * @param string
     * @return
     */
    public static String fastHash(String string) {
        // 将 string 串切割
        List<byte[]> list = FileUtils.slice(string.getBytes(Charset.forName("UTF-8")), 32);

        // Hash
        String code = FileUtils.fastHash(list);
        return code;
    }

    /**
     * 生成文件码。
     *
     * @param contactId 联系人 ID 。
     * @param domain 工作的域。
     * @param fileName 文件名。
     * @return 返回文件码。
     */
    public static String makeFileCode(Long contactId, String domain, String fileName) {
        StringBuilder buf = new StringBuilder(contactId.toString());
        buf.append(domain).append(fileName).append(System.currentTimeMillis());

        // 补空位
        if (buf.length() < 64) {
            buf.append("_").append(contactId.toString());
        }
        if (buf.length() < 64) {
            buf.append("_").append(domain);
        }

        String keyStr = buf.toString();

        // 将 Key 串切割
        List<byte[]> list = FileUtils.slice(keyStr.getBytes(Charset.forName("UTF-8")), 64);

        // Hash
        String code = FileUtils.fastHash(list);
        return code;
    }

    /**
     * 生成文件码。
     *
     * @param identification 识别码。
     * @param domain 工作的域。
     * @param fileName 文件名。
     * @return 返回文件码。
     */
    public static String makeFileCode(String identification, String domain, String fileName) {
        StringBuilder buf = new StringBuilder(identification);
        buf.append(domain).append(fileName).append(System.currentTimeMillis());

        // 补空位
        if (buf.length() < 64) {
            buf.append("_").append(identification);
        }
        if (buf.length() < 64) {
            buf.append("_").append(domain);
        }

        String keyStr = buf.toString();

        // 将 Key 串切割
        List<byte[]> list = FileUtils.slice(keyStr.getBytes(Charset.forName("UTF-8")), 64);

        // Hash
        String code = FileUtils.fastHash(list);
        return code;
    }

    /**
     * 制作文件标签。
     *
     * @param domainName 域名称。
     * @param fileCode 文件码。
     * @param contactId 所属联系人 ID 。
     * @param file 文件。
     * @return
     */
    public static FileLabel makeFileLabel(String domainName, String fileCode, Long contactId, File file) {
        // 计算文件散列码
        MessageDigest md5 = null;
        MessageDigest sha1 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[4096];
            int length = 0;
            while ((length = fis.read(bytes)) > 0) {
                md5.update(bytes, 0, length);
                sha1.update(bytes, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        byte[] hashMD5 = md5.digest();
        byte[] hashSHA1 = sha1.digest();
        String md5Code = FileUtils.bytesToHexString(hashMD5);
        String sha1Code = FileUtils.bytesToHexString(hashSHA1);

        // 判断文件类型
        FileType fileType = FileType.matchExtension(extractFileExtension(file.getName()));

        FileLabel fileLabel = new FileLabel(domainName, fileCode, contactId, file);
        fileLabel.setFileType(fileType);
        fileLabel.setMD5Code(md5Code);
        fileLabel.setSHA1Code(sha1Code);

        return fileLabel;
    }

    /**
     * 字节数组转16进制字符串。
     *
     * @param bytes
     * @return
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            byte b = bytes[i];
            int n = b & 0xFF;
            if (n < 16) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(n));
        }
        return buf.toString();
    }

    private static String fastHash(List<byte[]> bytes) {
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

    /**
     * 提取文件扩展名。
     *
     * @param fileName
     * @return
     */
    public static FileType extractFileExtensionType(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return FileType.UNKNOWN;
        }

        String extension = fileName.substring(index + 1);
        return FileType.matchExtension(extension);
    }

    /**
     * 提取文件扩展名。
     *
     * @param fileName 文件名。
     * @return
     */
    public static String extractFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return "";
        }

        return fileName.substring(index + 1);
    }

    /**
     * 提取文件名。
     *
     * @param fileName
     * @return
     */
    public static String extractFileName(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return fileName;
        }

        return fileName.substring(0, index);
    }

    /**
     * 校验文件类型。
     *
     * @param fileName 指定文件名。
     * @return
     */
    public static FileType verifyFileType(String fileName) {
        return FileUtils.verifyFileType(fileName, null);
    }

    /**
     * 校验文件类型。
     *
     * @param fileName 指定文件名。
     * @param data 指定文件数据。
     * @return
     */
    public static FileType verifyFileType(String fileName, byte[] data) {
        if (null != data) {
            // 通过数据进行判断
            FileType dataType = FileType.extractFileType(data);
            if (dataType != FileType.UNKNOWN && dataType != FileType.ZIP && dataType != FileType.GZIP) {
                return dataType;
            }
        }

        // 判断扩展名类型
        return extractFileExtensionType(fileName);
    }

    /**
     * 缩放文件大小。
     *
     * @param sizeInBytes
     * @return
     */
    public static FileSize scaleFileSize(long sizeInBytes) {
        String value = null;
        String unit = null;

        if (sizeInBytes < KB) {
            double d = ((double) sizeInBytes / (double) KB);
            value = String.format("%.2f", d);
            unit = "KB";
        }
        else if (sizeInBytes >= KB && sizeInBytes < MB) {
            double d = ((double) sizeInBytes / (double) KB);
            value = String.format("%.2f", d);
            unit = "KB";
        }
        else if (sizeInBytes >= MB && sizeInBytes < GB) {
            double d = ((double) sizeInBytes / (double) MB);
            value = String.format("%.2f", d);
            unit = "MB";
        }
        else if (sizeInBytes >= GB && sizeInBytes < TB) {
            double d = ((double) sizeInBytes / (double) GB);
            value = String.format("%.2f", d);
            unit = "GB";
        }
        else {
            double d = ((double) sizeInBytes / (double) TB);
            value = String.format("%.2f", d);
            unit = "TB";
        }

        return new FileSize(sizeInBytes, value, unit);
    }

    /**
     * 在文件名里插入后缀。
     *
     * @param fileName
     * @param postfix
     * @return
     */
    public static String insertPostfix(String fileName, String postfix) {
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            String name = fileName.substring(0, index);
            String extension = fileName.substring(index + 1);
            StringBuilder buf = new StringBuilder(name);
            buf.append(postfix).append(".").append(extension);
            return buf.toString();
        }
        else {
            return fileName + postfix;
        }
    }

    /**
     * 是否是常用图片类型。
     *
     * @param fileType 文件类型。
     * @return 如果是图片类型返回 {@code true} 。
     */
    public static boolean isImageType(FileType fileType) {
        switch (fileType) {
            case JPEG:
            case PNG:
            case GIF:
            case BMP:
            case WEBP:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否是常用文档类型。
     *
     * @param fileType 文件类型。
     * @return 如果是文档类型返回 {@code true} 。
     */
    public static boolean isDocumentType(FileType fileType) {
        switch (fileType) {
            case DOC:
            case DOCX:
            case PPT:
            case PPTX:
            case XLS:
            case XLSX:
            case PDF:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否是常用的视频类型。
     *
     * @param fileType 文件类型。
     * @return 如果是视频类型返回 {@code true} 。
     */
    public static boolean isVideoType(FileType fileType) {
        switch (fileType) {
            case MP4:
            case MPG4:
            case MPG:
            case MPEG:
            case AVI:
            case MKV:
            case TS:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否是常用的音频类型。
     *
     * @param fileType 文件类型。
     * @return 如果是音频类型返回 {@code true} 。
     */
    public static boolean isAudioType(FileType fileType) {
        switch (fileType) {
            case MP3:
            case OGG:
            case WAV:
            case WMA:
            case AAC:
                return true;
            default:
                return false;
        }
    }

    /**
     * 优化文件路径显示。
     *
     * @param absolutePath
     * @return
     */
    public static String fixFilePath(String absolutePath) {
        int index = absolutePath.indexOf(File.separator);
        if (index < 0) {
            return absolutePath;
        }

        String[] array = absolutePath.split(File.separator);
        LinkedList<String> list = new LinkedList<>();
        for (int i = 0; i < array.length; ++i) {
            list.add(array[i]);
        }

        Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            String path = iter.next();
            if (path.equals(".")) {
                iter.remove();
            }
        }

        for (int i = 0; i < list.size(); ++i) {
            String path = list.get(i);
            if (path.equals("..")) {
                list.remove(i);
                list.remove(i - 1);
                i -= 2;
            }
        }

        StringBuilder result = new StringBuilder();
        for (String path : list) {
            result.append(path);
            result.append(File.separator);
        }
        result.delete(result.length() - 1, result.length());
        return result.toString();
    }

    /**
     * 修正 Windows 系统文件路径保存到 JSON 时转义字符无法解析的问题。
     *
     * @param path
     * @return
     */
    public static String fixWindowsPathForJSON(String path) {
        return path.replaceAll("\\\\", "/");
    }

    /**
     * 清空路径。
     *
     * @param path
     */
    public static void emptyPath(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (null != files && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        emptyPath(file);
                        // 删除空目录
                        file.delete();
                    }
                    else {
                        file.delete();
                    }
                }
            }
        }
    }

//    public static void main(String[] args) {
//        System.out.println(FileUtils.fixFilePath("/Users/ambrose/Documents/Repositories/Cube3/cube-server/console/../deploy"));
//        System.out.println(FileUtils.fixFilePath("D:/ambrose/Documents/Repositories/Cube3/cube-server/console/../deploy"));
//        System.out.println(FileUtils.fixFilePath("D:\\ambrose\\Documents\\Repositories\\Cube3\\cube-server\\console\\..\\deploy"));
//        System.out.println(FileUtils.makeFileCode(50001001L, "三周年纪念.png"));
//        System.out.println(FileUtils.makeFileCode(50001001L, "三周年纪念.jpg"));
//        System.out.println(FileUtils.makeFileCode(50002001L, "三周年纪念.png"));
//        System.out.println();
//        System.out.println(FileUtils.makeFileCode(2005179136L, "这个文件的文件名很长很长很长很长很长很长很长"
//          + "很长很长很长很长很长很长很长很长很长很长很长.txt"));
//    }
}
