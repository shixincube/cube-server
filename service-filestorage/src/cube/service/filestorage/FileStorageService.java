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

package cube.service.filestorage;

import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.common.entity.FileLabel;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.hierarchy.FileHierarchyManager;
import cube.service.filestorage.recycle.RecycleBin;
import cube.service.filestorage.system.DiskSystem;
import cube.service.filestorage.system.FileDescriptor;
import cube.service.filestorage.system.FileSystem;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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

    public final static String NAME = "FileStorage";

    /**
     * 默认的文件有效时长。
     */
    private long defaultFileDuration = 30L * 24L * 60L * 60L * 1000L;

    /**
     * 文件大小门限。
     */
    private long fileSizeThreshold = 20L * 1024L * 1024L;

    /**
     * 文件系统。
     */
    private FileSystem fileSystem;

    /**
     * 文件描述符缓存。
     */
    protected ConcurrentHashMap<String, FileDescriptor> fileDescriptors;

    /**
     * 文件标签的集群缓存。
     */
    protected Cache fileLabelCache;

    /**
     * 文件标签存储器。
     */
    private FileStructStorage fileStructStorage;

    /**
     * 文件层级管理器。
     */
    private FileHierarchyManager fileHierarchyManager;

    /**
     * 文件回收站。
     */
    private RecycleBin recycleBin;

    /**
     * 多线程执行器。
     */
    private ExecutorService executor;

    /**
     * 守护任务。
     */
    private DaemonTask daemonTask;

    /**
     * 构造函数。
     *
     * @param executor 多线程执行器。
     */
    public FileStorageService(ExecutorService executor) {
        super();
        this.executor = executor;
        this.fileDescriptors = new ConcurrentHashMap<>();
        this.daemonTask = new DaemonTask(this);
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
            this.fileLabelCache = this.getKernel().installCache(
                    properties.getProperty("label.cache.name", "FileLabelCache"),
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

        // 创建文件层级管理器
        this.fileHierarchyManager = new FileHierarchyManager(this.fileStructStorage, this);

        // 回收站
        this.recycleBin = new RecycleBin(this.fileStructStorage);
    }

    @Override
    public void stop() {
        // 停止文件系统
        this.fileSystem.stop();

        // 关闭存储
        this.fileStructStorage.close();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        this.daemonTask.run();

        this.fileHierarchyManager.onTick();
    }

    /**
     * 获取回收站实例。
     *
     * @return 返回回收站实例。
     */
    public RecycleBin getRecycleBin() {
        return this.recycleBin;
    }

    /**
     * 向文件系统写入文件数据。
     *
     * @param fileCode
     * @param inputStream
     */
    public void writeFile(String fileCode, InputStream inputStream) {
        // 删除旧文件
        this.fileSystem.deleteFile(fileCode);

        // 写入文件系统
        FileDescriptor descriptor = this.fileSystem.writeFile(fileCode, inputStream);

        // 缓存文件标识
        this.fileDescriptors.put(fileCode, descriptor);
    }

    /**
     * 向文件系统写入文件数据。
     *
     * @param fileCode
     * @param file
     */
    public void writeFile(String fileCode, File file) {
        // 删除旧文件
        this.fileSystem.deleteFile(fileCode);

        // 写入文件系统
        FileDescriptor descriptor = this.fileSystem.writeFile(fileCode, file);

        // 缓存文件标识
        this.fileDescriptors.put(fileCode, descriptor);
    }

    /**
     * 放置文件标签。
     *
     * @param fileLabel
     * @return
     */
    public FileLabel putFile(FileLabel fileLabel) {
        // 设置文件标签的 URL 信息

        // 获取标识
        FileDescriptor descriptor = this.fileDescriptors.get(fileLabel.getFileCode());

        int count = 100;
        while (null == descriptor) {
            --count;
            if (count <= 0) {
                break;
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            descriptor = this.fileDescriptors.get(fileLabel.getFileCode());
        }

        if (null == descriptor) {
            // 没有获取正确的文件描述符
            Logger.e(this.getClass(), "#putFile - Can NOT find file descriptor: " + fileLabel.getFileCode());
            return null;
        }

        fileLabel.setDirectURL(descriptor.getURL());

        // 获取外部访问的 URL 信息
        String[] urls = this.getFileURLs(fileLabel.getDomain().getName());
        if (null == urls) {
            return null;
        }

        String queryString = "?fc=" + fileLabel.getFileCode();
        fileLabel.setFileURLs(urls[0] + queryString, urls[1] + queryString);

        // 设置有效期
        fileLabel.setExpiryTime(fileLabel.getCompletedTime() + this.defaultFileDuration);

        // 写入到存储器进行记录
        this.fileStructStorage.writeFileLabel(fileLabel, descriptor);

        // 写入集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        return fileLabel;
    }

    /**
     * 更新指定文件的有效期。
     *
     * @param fileLabel
     * @param expiryTime
     * @return
     */
    public boolean updateFileExpiryTime(FileLabel fileLabel, long expiryTime) {
        if (expiryTime > 0 && expiryTime < fileLabel.getCompletedTime()) {
            return false;
        }

        // 修改超期时间
        fileLabel.setExpiryTime(expiryTime);

        // 更新存储器
        this.fileStructStorage.updateFileLabel(fileLabel);

        // 更新集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        return true;
    }

    /**
     * 文件是否存在文件系统里。
     *
     * @param fileCode
     * @return
     */
    public boolean existsFileData(String fileCode) {
        if (this.fileSystem.isWriting(fileCode)) {
            return false;
        }

        return this.fileSystem.existsFile(fileCode);
    }

    /**
     * 文件是否存在。
     *
     * @param domainName
     * @param fileCode
     * @return
     */
    public boolean existsFile(String domainName, String fileCode) {
        CacheValue value = this.fileLabelCache.get(new CacheKey(fileCode));
        if (null != value) {
            return true;
        }

        return this.fileStructStorage.existsFileLabel(domainName, fileCode);
    }

    /**
     * 读文件标签。
     *
     * @param domainName
     * @param fileCode
     * @return
     */
    public FileLabel getFile(String domainName, String fileCode) {
        CacheValue value = this.fileLabelCache.get(new CacheKey(fileCode));
        if (null == value) {
            FileLabel fileLabel = this.fileStructStorage.readFileLabel(domainName, fileCode);
            if (null == fileLabel) {
                return null;
            }

            this.fileLabelCache.put(new CacheKey(fileCode), new CacheValue(fileLabel.toJSON()));
            return fileLabel;
        }

        return new FileLabel(value.get());
    }

    /**
     * 获取指定域下，联系人或群组的文件目录。
     *
     * @param domainName
     * @param contactOrGroupId
     * @return
     */
    public FileHierarchy getFileHierarchy(String domainName, Long contactOrGroupId) {
        return this.fileHierarchyManager.getFileHierarchy(contactOrGroupId, domainName);
    }

    /**
     * 加载文件到本地。
     *
     * @param domainName
     * @param fileCode
     * @return
     */
    public String loadFileToDisk(String domainName, String fileCode) {
        FileLabel fileLabel = this.getFile(domainName, fileCode);
        if (null == fileLabel || fileLabel.getFileSize() > this.fileSizeThreshold) {
            return null;
        }

        return this.fileSystem.loadFileToDisk(fileCode);
    }

    /**
     * 从授权模块获取指定域对应的文件访问地址。
     *
     * @param domain
     * @return
     */
    private String[] getFileURLs(String domain) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        JSONObject primary = authService.getPrimaryContent(domain);
        if (null == primary) {
            return null;
        }

        try {
            JSONObject fileStorage = primary.getJSONObject(FileStorageService.NAME);
            String[] result = new String[2];
            result[0] = fileStorage.getString("fileURL");
            result[1] = fileStorage.getString("fileSecureURL");
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 从配置文件加载配置。
     *
     * @return
     */
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

    /**
     * 初始化存储。
     */
    private void initStorage() {
        // 读取存储配置
        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(FileStorageService.NAME)) {
            config = config.getJSONObject(FileStorageService.NAME);
            if (config.getString("type").equalsIgnoreCase("SQLite")) {
                this.fileStructStorage = new FileStructStorage(this.executor, StorageType.SQLite, config);
            }
            else {
                this.fileStructStorage = new FileStructStorage(this.executor, StorageType.MySQL, config);
            }
        }
        else {
            config.put("file", "storage/FileStorageService.db");
            this.fileStructStorage = new FileStructStorage(this.executor, StorageType.SQLite, config);
        }

        (new Thread() {
            @Override
            public void run() {
                // 打开存储器
                fileStructStorage.open();

                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                fileStructStorage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }
}
