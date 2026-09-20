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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
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
     * 生成随机序列号。
     * 该序列号最大值是 0x0FFFFFFE 。
     *
     * @return
     */
    public static long generateSerialNumber() {
        long min = 10000000L;
        long max = 999999999L;
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * 生成基于 MAC 地址信息的识别标识。
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
     * 生成稳定的节点标识（16 位十六进制字符串）。
     *
     * 与 {@link #makeUniqueStringWithMAC()} 的关键差异：本方法只使用**唯一的主网卡**，
     * 因此网卡集合发生变化（启停 VPN、切换 Wi-Fi、开热点、出现 utun/bridge 等虚拟接口）时结果不会漂移。
     * 节点侧与控制台侧使用同一实现，只要部署在同一台机器上就能得到完全一致的标识。
     *
     * @return 稳定标识，任何路径都失败时退化为 {@link #makeUniqueStringWithMAC()} 的结果。
     */
    public static String makeStableNodeId() {
        try {
            // 首选：主网卡的 MAC
            String mac = getPrimaryMAC();
            if (null != mac) {
                String id = hashAsHex(mac);
                if (null != id) {
                    return id;
                }
            }

            // 其次：主机名 + 首选 IPv4
            StringBuilder seed = new StringBuilder();
            String host = getHostName();
            if (null != host) {
                seed.append(host);
            }
            String ip = getPrimaryIPv4();
            if (null != ip) {
                if (seed.length() > 0) {
                    seed.append('@');
                }
                seed.append(ip);
            }
            if (seed.length() > 0) {
                String id = hashAsHex(seed.toString());
                if (null != id) {
                    return id;
                }
            }
        } catch (Exception e) {
            Logger.w(ConfigUtils.class, "#makeStableNodeId", e);
        }

        // 兜底：保持与历史版本一致的行为
        return makeUniqueStringWithMAC();
    }

    /**
     * 获取唯一的主网卡 MAC 地址。
     *
     * 过滤掉回环、未启用、点对点与各类虚拟接口（VPN、容器网络、Airdrop 等），
     * 优先选择物理以太网/Wi-Fi 接口（名称以 en/eth 开头），否则按接口名排序取第一个，保证结果确定。
     *
     * @return MAC 地址字符串，无法确定时返回 null。
     */
    private static String getPrimaryMAC() throws Exception {
        Map<String, String> candidates = new TreeMap<>();

        java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface iface = en.nextElement();

            if (!iface.isUp() || iface.isLoopback() || iface.isPointToPoint() || iface.isVirtual()) {
                continue;
            }

            String name = iface.getName();
            if (null == name || isVirtualInterfaceName(name)) {
                continue;
            }

            byte[] mac = iface.getHardwareAddress();
            if (null == mac || mac.length == 0) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; ++i) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            candidates.put(name, sb.toString());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String> entry : candidates.entrySet()) {
            String name = entry.getKey().toLowerCase();
            if (name.startsWith("en") || name.startsWith("eth")) {
                return entry.getValue();
            }
        }

        return candidates.values().iterator().next();
    }

    /**
     * 判断是否为虚拟接口。
     *
     * @param name 接口名。
     * @return 虚拟接口返回 true。
     */
    private static boolean isVirtualInterfaceName(String name) {
        String lower = name.toLowerCase();
        String[] prefixes = {"utun", "awdl", "llw", "bridge", "vmnet", "vboxnet", "docker",
                "veth", "br-", "tun", "tap", "gif", "stf", "anpi", "ap", "ham", "lo"};
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取主机名。
     *
     * @return 主机名，失败返回 null。
     */
    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取首选的非回环 IPv4 地址。
     *
     * @return IP 地址字符串，无法确定时返回 null。
     */
    private static String getPrimaryIPv4() {
        try {
            java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            List<String> addresses = new ArrayList<>();
            while (en.hasMoreElements()) {
                NetworkInterface iface = en.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    InetAddress ip = addr.getAddress();
                    if (null == ip || ip.isLoopbackAddress()) {
                        continue;
                    }
                    if (ip.getAddress().length == 4) {
                        addresses.add(ip.getHostAddress());
                    }
                }
            }
            if (addresses.isEmpty()) {
                return null;
            }
            Collections.sort(addresses);
            return addresses.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 对指定字符串做 MD5 并截取前 8 字节的十六进制串（16 个字符）。
     *
     * @param value 输入字符串。
     * @return 16 位十六进制字符串，失败返回 null。
     */
    private static String hashAsHex(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(value.getBytes(StandardCharsets.UTF_8));
            byte[] head = new byte[8];
            System.arraycopy(digest, 0, head, 0, head.length);
            return FileUtils.bytesToHexString(head);
        } catch (Exception e) {
            return null;
        }
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
