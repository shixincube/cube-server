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

package cube.service.filestorage;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.common.entity.FileLabel;
import cube.core.AbstractModule;
import cube.core.Cache;
import cube.core.CacheKey;
import cube.core.CacheValue;
import cube.service.auth.AuthService;
import cube.service.filestorage.system.DiskSystem;
import cube.service.filestorage.system.FileDescriptor;
import cube.service.filestorage.system.FileSystem;
import cube.storage.StorageType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 文件存储器服务模块。
 */
public class FileStorageService extends AbstractModule {

    private FileSystem fileSystem;

    private ConcurrentHashMap<String, FileDescriptor> fileDescriptors;

    private Cache fileLabelCache;

    private FileLabelStorage fileLabelStorage;

    /**
     * 多线程执行器。
     */
    private ExecutorService executor;

    public FileStorageService(ExecutorService executor) {
        super();
        this.executor = executor;
        this.fileDescriptors = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        // 加载配置
        Properties properties = this.loadConfig();

        if (null != properties) {
            // 创建文件系统
            String filesystem = properties.getProperty("filesystem", "disk");
            if (filesystem.equalsIgnoreCase("disk")) {
                String path = properties.getProperty("disk.dir", "storage/files");
                String host = properties.getProperty("disk.host", "127.0.0.1");
                int port = Integer.parseInt(properties.getProperty("disk.port", "6080"));

                this.fileSystem = new DiskSystem(path, host, port);
            }
            else {
                Logger.w(this.getClass(), "Unsupported file system: " + filesystem);

                this.fileSystem = new DiskSystem("storage/files", "127.0.0.1", 6080);
            }

            // 创建缓存
            String cacheConfigString = properties.getProperty("label.cache.config", "{}");
            JSONObject cacheConfig = null;
            try {
                cacheConfig = new JSONObject(cacheConfigString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // 安装缓存
            this.fileLabelCache = this.getKernel().installCache(properties.getProperty("label.cache.name", "FileLabelCache"),
                    cacheConfig);
        }
        else {
            Logger.e(this.getClass(), "Load config file failed");

            this.fileSystem = new DiskSystem("storage/files", "127.0.0.1", 6080);

            JSONObject cacheConfig = new JSONObject();
            try {
                cacheConfig.put("configFile", "config/filelabel-cache.properties");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            this.fileLabelCache = new SharedMemoryCache("FileLabelCache");
            this.fileLabelCache.configure(cacheConfig);
            this.fileLabelCache.start();
        }

        // 启动文件系统
        this.fileSystem.start();

        // 初始化存储
        this.initStorage();
    }

    @Override
    public void stop() {
        // 停止文件系统
        this.fileSystem.stop();

        this.fileLabelStorage.close();
    }

    /**
     * 写文件描述。
     *
     * @param fileCode
     * @param inputStream
     */
    public void writeFile(String fileCode, InputStream inputStream) {
        // 写入文件系统
        FileDescriptor descriptor = this.fileSystem.writeFile(fileCode, inputStream);

        // 缓存文件标识
        this.fileDescriptors.put(fileCode, descriptor);

        // 存储文件标识
        this.fileLabelStorage.writeFileDescriptor(fileCode, descriptor);
    }

    /**
     * 写文件标签。
     *
     * @param fileLabel
     * @return
     */
    public FileLabel putFile(FileLabel fileLabel) {
        // 设置文件标签的 URL 信息

        // 获取标识
        FileDescriptor descriptor = this.fileDescriptors.get(fileLabel.getFileCode());
        fileLabel.setDirectURL(descriptor.getURL());

        // 获取外部访问的 URL 信息
        String[] urls = this.getFileURLs(fileLabel.getDomain().getName());
        if (null == urls) {
            return null;
        }

        fileLabel.setFileURLs(urls[0] + fileLabel.getFileCode(),
                urls[1] + fileLabel.getFileCode());

        // 写入到存储器进行记录
        this.fileLabelStorage.writeFileLabel(fileLabel);

        // 写入集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        return fileLabel;
    }

    /**
     * 读文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel getFile(String fileCode) {
        CacheValue value = this.fileLabelCache.get(new CacheKey(fileCode));
        if (null == value) {
            FileLabel fileLabel = this.fileLabelStorage.readFileLabel(fileCode);
            return fileLabel;
        }

        return new FileLabel(value.get());
    }

    private String[] getFileURLs(String domain) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        JSONObject primary = authService.getPrimaryContent(domain);
        if (null == primary) {
            return null;
        }

        try {
            JSONObject fileStorage = primary.getJSONObject(FileStorageServiceCellet.NAME);
            String[] result = new String[2];
            result[0] = fileStorage.getString("fileURL");
            result[1] = fileStorage.getString("fileSecureURL");
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Properties loadConfig() {
        Path path = Paths.get("config/file-storage.properties");
        if (!Files.exists(path)) {
            path = Paths.get("file-storage.properties");
            if (!Files.exists(path)) {
                return null;
            }
        }

        Properties properties = new Properties();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path.toFile());
            properties.load(fis);
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

        return properties;
    }

    private void initStorage() {
        JSONObject config = new JSONObject();
        try {
            config.put("file", "storage/FileStorageService.db");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.fileLabelStorage = new FileLabelStorage(this.executor, StorageType.SQLite, config);

        this.fileLabelStorage.open();

        (new Thread() {
            @Override
            public void run() {
                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                fileLabelStorage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }
}
