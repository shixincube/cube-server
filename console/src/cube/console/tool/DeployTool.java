/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.console.tool;

import cube.util.FileUtils;

import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部署工具。
 */
public final class DeployTool {

    private DeployTool() {
    }

    public static Path searchDeploySource() {
        String[] pathList = new String[] {
                "deploy",
                "../deploy"
        };

        String[] jarList = new String[] {
                "cube-common",
                "cube-dispatcher"
        };

        File path = null;
        String pathString = null;
        for (String p : pathList) {
            path = new File(p);
            if (path.exists() && path.isDirectory()) {
                pathString = path.getAbsolutePath();
                break;
            }
        }

        if (null == pathString) {
            return null;
        }

        pathString = FileUtils.fixFilePath(pathString);

        Path cellJar = Paths.get(pathString, "bin/cell.jar");
        if (!Files.exists(cellJar)) {
            return null;
        }

        Path libsPath = Paths.get(pathString, "libs");
        File pathFile = new File(libsPath.toString());
        if (!pathFile.isDirectory()) {
            return null;
        }

        boolean hasCommon = false;
        boolean hasDispatcher = false;

        File[] files = pathFile.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("cube-common")) {
                hasCommon = true;
            }
            else if (file.getName().startsWith("cube-dispatcher")) {
                hasDispatcher = true;
            }
        }

        if (!hasCommon || !hasDispatcher) {
            return null;
        }

        return Paths.get(pathString);
    }

    /**
     * 获取本机的所有 MAC 地址。
     * @return
     * @throws Exception
     */
    public static List<String> getMACList() throws Exception {
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
}
