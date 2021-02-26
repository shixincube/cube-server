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

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.console.storage.ServiceStorage;
import cube.console.tool.DeployTool;
import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * 服务单元管理器。
 */
public class ServiceManager {

    private String tag;

    private ServiceStorage storage;

    private Path deploySourcePath;

    public ServiceManager(String tag) {
        this.tag = tag;
    }

    public void start() {
        String filepath = "console.properties";
        File file = new File(filepath);
        if (!file.exists()) {
            filepath = "config/console.properties";
            file = new File(filepath);
            if (!file.exists()) {
                return;
            }
        }

        try {
            Properties properties = ConfigUtils.readProperties(filepath);
            this.storage = new ServiceStorage(properties);
            this.storage.open();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#start", e);
        }

        this.deploySourcePath = DeployTool.searchDeploySource();
        if (null != this.deploySourcePath) {
            Logger.i(this.getClass(), "Deploy source path: " + this.deploySourcePath.toString());

            // 检查当前库里是否有默认部署信息
            ServiceServer server = this.storage.readServer(this.tag, this.deploySourcePath.toString());
            if (null == server) {
                // 插入本地部署
                this.storage.writeServer(this.tag, this.getDefaultDeployPath(),
                        this.getDefaultConfigPath(), this.getDefaultCelletsPath());
            }
        }
        else {
            Logger.e(this.getClass(), "Can NOT find deploy source path");
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }
    }

    public String getDefaultDeployPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        return this.deploySourcePath.toString();
    }

    public String getDefaultConfigPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path path = Paths.get(this.deploySourcePath.toString(), "config");
        return path.toString();
    }

    public String getDefaultCelletsPath() {
        if (null == this.deploySourcePath) {
            return null;
        }

        Path path = Paths.get(this.deploySourcePath.toString(), "cellets");
        return path.toString();
    }

    public List<ServiceServer> listServiceServers() {
        if (null == this.deploySourcePath) {
            return null;
        }

        List<ServiceServer> list = this.storage.listServers();
        if (null == list) {
            return null;
        }

        for (ServiceServer server : list) {
            if (this.tag.equals(server.tag)) {
                server.refresh();
            }
            else {
                // TODO
            }
        }

        return list;
    }
}
