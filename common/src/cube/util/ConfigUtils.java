/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 配置管理数据实用函数库。
 */
public final class ConfigUtils {

    /**
     * 升序。
     */
    public final static String ORDER_ASC = "asc";

    /**
     * 降序。
     */
    public final static String ORDER_DESC = "desc";

    private ConfigUtils() {
    }

    /**
     * 生成服务器基于 MAC 地址信息的识别标识。
     *
     * @return
     */
    public static String makeUniqueStringWithMAC() {
        String uniqueString = null;

        try {
            List<byte[]> md5Values = new ArrayList<>();

            // 计算所有 MAC 的 MD5 码
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            List<String> macList = getMACList();
            for (String mac : macList) {
                md5.update(mac.getBytes());
                md5Values.add(md5.digest());
                md5.reset();
            }

            // 将 MD5 码对位相加
            byte[] md5data = new byte[md5Values.get(0).length];
            for (byte[] value : md5Values) {
                for (int i = 0; i < md5data.length; ++i) {
                    md5data[i] += value[i];
                }
            }

            // 压缩编码
            byte[] compressed = new byte[md5data.length / 2];
            int index = 0;
            for (int i = 0; i < md5data.length; i += 2) {
                int temp = (md5data[i] >= 0 ? md5data[i] : 256 + md5data[i])
                        + (md5data[i + 1] >= 0 ? md5data[i + 1] : 256 + md5data[i + 1]);
                compressed[index] = (byte) temp;
                ++index;
            }

            uniqueString = FileUtils.bytesToHexString(compressed);
        } catch (Exception e) {
            Logger.e(ConfigUtils.class, "#makeUniqueStringWithMAC", e);
        }

        return uniqueString;
    }

    /**
     * 获取本机的有效 MAC 地址。
     * @return
     * @throws Exception
     */
    private static List<String> getMACList() throws Exception {
        java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> tmpMacList = new ArrayList<>();
        while (en.hasMoreElements()) {
            NetworkInterface iface = en.nextElement();
            List<InterfaceAddress> addrs = iface.getInterfaceAddresses();
            for (InterfaceAddress addr : addrs) {
                InetAddress ip = addr.getAddress();
                if (ip.toString().indexOf("awdl") > 0 || ip.toString().indexOf("llw") > 0) {
                    // 跳过 Apple macOS 的 Airdrop 设备
                    continue;
                }

                NetworkInterface network = NetworkInterface.getByInetAddress(ip);
                if (network == null) {
                    continue;
                }
                byte[] mac = network.getHardwareAddress();
                if (mac == null) {
                    continue;
                }
                sb.delete(0, sb.length());
                for (int i = 0; i < mac.length; ++i) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                tmpMacList.add(sb.toString());
            }
        }

        if (tmpMacList.isEmpty()) {
            return tmpMacList;
        }

        // 去重
        List<String> unique = tmpMacList.stream().distinct().collect(Collectors.toList());
        return unique;
    }

    /**
     * 读取 Properties 文件数据。
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Properties readProperties(String path) throws IOException {
        Properties result = null;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(path));
            result = new Properties();
            result.load(fis);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        return result;
    }

    /**
     * 读取存储配置文件。
     *
     * @return
     */
    public static JSONObject readStorageConfig() {
        JSONObject json = new JSONObject();
        try {
            Path file = Paths.get("config/storage_dev.json");
            if (!Files.exists(file)) {
                file = Paths.get("config/storage.json");
            }

            byte[] data = Files.readAllBytes(file);
            json = new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.d(ConfigUtils.class, "#readStorageConfig - " + e.getMessage());
        }
        return json;
    }

    /**
     * 读取指定 JSON 格式的文件。
     *
     * @param filePath
     * @return
     */
    public static JSONObject readJsonFile(String filePath) {
        JSONObject json = null;
        try {
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                file = Paths.get("config/" + filePath);
            }

            byte[] data = Files.readAllBytes(file);
            json = new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.w(ConfigUtils.class, "#readJsonFile - Read file error", e);
        }
        return json;
    }

    /**
     * 写入 JSON 格式数据到文件。
     *
     * @param filePath
     * @param json
     * @return
     */
    public static boolean writeJsonFile(String filePath, JSONObject json) {
        try {
            Path path = Paths.get(filePath);
            Files.write(path, json.toString(4).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            Logger.w(ConfigUtils.class, "#writeJsonFile - Write file error", e);
            return false;
        }
    }
}
