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

package cube.util;

import cell.util.log.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 配置管理数据实用函数库。
 */
public final class ConfigUtils {

    private ConfigUtils() {
    }

    public static Properties readConsoleServers() throws IOException {
        return ConfigUtils.readConsoleServers("config/servers.properties");
    }

    public static Properties readConsoleServers(String path) throws IOException {
        return ConfigUtils.readProperties(path);
    }

    public static Properties readConsoleFollower() throws IOException {
        return ConfigUtils.readConsoleFollower("config/console-follower.properties");
    }

    public static Properties readConsoleFollower(String path) throws IOException {
        return ConfigUtils.readProperties(path);
    }

    public static Properties readProperties(String path) throws IOException {
        Properties result = new Properties();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(path));
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
            byte[] data = Files.readAllBytes(Paths.get("config/storage.json"));
            json = new JSONObject(new String(data, Charset.forName("UTF-8")));
        } catch (IOException e) {
            Logger.d(ConfigUtils.class, "#readStorageConfig - " + e.getMessage());
        }
        return json;
    }
}
