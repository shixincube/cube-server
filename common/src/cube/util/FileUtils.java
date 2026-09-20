/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;
import cube.common.entity.FileLabel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private final static char HexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

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
        List<byte[]> list = FileUtils.slice(string.getBytes(StandardCharsets.UTF_8), 32);

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
        // FIXME 2023年6月17日 修改文件码生成规则。
        // 原规则里加入了时间戳，这样相同文件会生成不同的文件码。
        // 修正新规则：不加入时间戳作为原始串进行散列。
        StringBuilder buf = new StringBuilder(contactId.toString());
        buf.append(domain).append(fileName);
        // 删除时间戳 .append(System.currentTimeMillis());

        // 补空位
        if (buf.length() < 64) {
            buf.append("_").append(fileName);
        }
        if (buf.length() < 64) {
            buf.append("_").append(contactId.toString());
        }
        if (buf.length() < 64) {
            buf.append("_").append(domain);
        }

        return FileUtils.hashFileCode(buf.toString());
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
        // FIXME 2023年6月17日 修改文件码生成规则。
        // 原规则里加入了时间戳，这样相同文件会生成不同的文件码。
        // 修正新规则：不加入时间戳作为原始串进行散列。
        StringBuilder buf = new StringBuilder(identification);
        buf.append(domain).append(fileName);

        // 补空位
        if (buf.length() < 64) {
            buf.append("_").append(identification);
        }
        if (buf.length() < 64) {
            buf.append("_").append(fileName);
        }
        if (buf.length() < 64) {
            buf.append("_").append(domain);
        }

        return FileUtils.hashFileCode(buf.toString());
    }

    /**
     * 对文件码原始串做散列。
     *
     * <p>返回一个长度为 64 的 A-Za-z 字符串。该字符串既作为文件区块存储的 Map Key，
     * 也直接作为落盘的磁盘文件名，因此**返回长度与字符集必须保持稳定**。</p>
     *
     * @param keyStr 文件码原始串。
     * @return 返回文件码。
     */
    private static String hashFileCode(String keyStr) {
        // 将 Key 串切割
        List<byte[]> list = FileUtils.slice(keyStr.getBytes(StandardCharsets.UTF_8), 64);

        // Hash
        return FileUtils.fastHash(list);
    }

    /**
     * 制作文件标签。
     *
     * @param domainName 域名称。
     * @param fileCode 文件码。
     * @param contactId 所属联系人 ID 。
     * @param file 文件。
     * @return 返回 FileLabel 实例。
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
        int len = bytes.length;
        char[] result = new char[len * 2];
        int k = 0;
        for (int i = 0; i < len; ++i) {
            byte b = bytes[i];
            result[k++] = HexDigits[b >>> 4 & 0xf];
            result[k++] = HexDigits[b & 0xf];
        }

        return new String(result);
    }

    private static String fastHash(List<byte[]> bytes) {
        int sliceCount = bytes.size();
        int length = bytes.get(0).length;
        int[] hashCode = new int[length];

        // FIXME 2026年9月20日 修正散列碰撞。
        // 原实现里第 n 位输出只由各分片第 n 个字节决定：hashCode[n] = hashCode[n] * seed + b 。
        // 当只有一个分片时 seed == 1 ，第一位即 hashCode[n] = 0 * 1 + b = b ，等于没有混合，
        // 散列退化成 (b % 52) 查 CHAR_TABLE —— 而 CHAR_TABLE 只有 A-Za-z 52 个字母，
        // 于是 ASCII 相差 52 的两个字符必然落到同一个槽：
        //   '0'↔'d' '1'↔'e' … '9'↔'m' 、 'a'↔'-' 'b'↔'.' 's'↔'?' 't'↔'@' 'u'↔'A'
        // 实测 "IMG_1234.jpg" 与 "IMG_efgh.jpg" 生成完全相同的文件码。
        // 这里引入跨位置、跨分片的滚动状态，使每一位输出都携带整个输入的信息，
        // 输出长度与字符表保持不变（仍为 A-Za-z 字母串，长度等于分片长度）。
        int state = 0x811C9DC5;
        for (int i = 0; i < sliceCount; ++i) {
            byte[] data = bytes.get(i);
            for (int n = 0; n < length; ++n) {
                int b = data[n] & 0xFF;
                // FNV-1a 滚动，保证字节的先后顺序也参与散列
                state = (state ^ b) * 0x01000193;
                hashCode[n] = hashCode[n] * 0x9E3779B1 + b + state;
            }
        }

        // 查表
        StringBuilder buf = new StringBuilder(length);
        for (int code : hashCode) {
            int index = (avalanche(code) & 0x7FFFFFFF) % CHAR_TABLE.length;
            buf.append((char)CHAR_TABLE[index]);
        }

        return buf.toString();
    }

    /**
     * 位雪崩：把输入的高低比特充分混合，避免相邻取值在取模后落到相邻槽位。
     *
     * @param hash
     * @return 返回混合后的值。
     */
    private static int avalanche(int hash) {
        int h = hash;
        h ^= (h >>> 16);
        h *= 0x85EBCA6B;
        h ^= (h >>> 13);
        h *= 0xC2B2AE35;
        h ^= (h >>> 16);
        return h;
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
     * 提取文件扩展类型。
     *
     * @param fileName 文件名。
     * @return 返回文件类型。
     */
    public static FileType extractFileExtensionType(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return FileType.matchExtension(fileName);
        }

        String extension = fileName.substring(index + 1);
        return FileType.matchExtension(extension);
    }

    /**
     * 提取文件扩展名。
     *
     * @param fileName 文件名。
     * @return 返回文件名的扩展名。
     */
    public static String extractFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return "";
        }

        return fileName.substring(index + 1);
    }

    /**
     * 提取不包含扩展名的文件名。
     *
     * @param fileName 文件名。
     * @return 返回没有文件扩展名的文件名。
     */
    public static String extractFileName(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return fileName;
        }

        return fileName.substring(0, index);
    }

    /**
     * 提取路径里的完整父路径。
     *
     * @param path 路径。
     * @return 返回路径的父路径。
     */
    public static String extractPath(String path) {
        int index = path.lastIndexOf("/");
        if (index < 0) {
            index = path.lastIndexOf("\\");
        }

        if (index < 0) {
            return "";
        }

        return path.substring(0, index);
    }

    /**
     * 校验文件类型。
     *
     * @param fileName 指定文件名。
     * @return 返回文件类型。
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
     * 按照文本方式读取文件数据。
     *
     * @param filepath
     * @return
     */
    public static String readTextFile(String filepath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filepath));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.e(FileUtils.class, "#readTextFile", e);
            return null;
        }
    }

    /**
     * 缩放文件大小。
     *
     * @param sizeInBytes
     * @return
     */
    public static FileSize scaleFileSize(long sizeInBytes) {
        String value;
        String unit;

        // 不足 1MB 一律按 KB 折算（1KB 以下显示为 0.xx KB）
        if (sizeInBytes < MB) {
            value = String.format("%.2f", (double) sizeInBytes / (double) KB);
            unit = "KB";
        }
        else if (sizeInBytes < GB) {
            value = String.format("%.2f", (double) sizeInBytes / (double) MB);
            unit = "MB";
        }
        else if (sizeInBytes < TB) {
            value = String.format("%.2f", (double) sizeInBytes / (double) GB);
            unit = "GB";
        }
        else {
            value = String.format("%.2f", (double) sizeInBytes / (double) TB);
            unit = "TB";
        }

        return new FileSize(sizeInBytes, value, unit);
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
     * 修正 Windows 系统文件路径保存到 JSON 时转义字符无法解析的问题。
     *
     * @param path
     * @return
     */
    public static String fixWindowsPathForJSON(String path) {
        // 纯字面量替换，不需要正则（原实现用 replaceAll 会把参数当正则解析）
        return path.replace('\\', '/');
    }

    /**
     * 清空指定路径里的内容。
     *
     * <p>该方法只清空 {@code path} 的**内容**，不删除 {@code path} 自身，调用方需自行删除。</p>
     *
     * <p>关于符号链接：{@link File#isDirectory()} 会跟随符号链接，若不加判断，
     * 一个指向本路径之外目录的链接会让该方法递归进去并删除链接目标里的数据。
     * 因此这里显式判断符号链接，遇到链接时只删除链接自身，绝不递归进入其指向的目录。</p>
     *
     * @param path
     */
    public static void emptyPath(File path) {
        if (null == path) {
            return;
        }

        if (Files.isSymbolicLink(path.toPath())) {
            // 链接自身：只删除链接，不跟随
            path.delete();
            return;
        }

        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (null != files && files.length > 0) {
                for (File file : files) {
                    if (Files.isSymbolicLink(file.toPath())) {
                        // 只删除链接自身，不递归进入链接指向的目录
                        file.delete();
                    }
                    else if (file.isDirectory()) {
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
}
