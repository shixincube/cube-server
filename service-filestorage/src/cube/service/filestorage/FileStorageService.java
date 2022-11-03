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

package cube.service.filestorage;

import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.cache.SharedMemoryCache;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.common.state.FileStorageStateCode;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.hierarchy.FileHierarchyManager;
import cube.service.filestorage.plugin.CreateDomainAppPlugin;
import cube.service.filestorage.plugin.SignInPlugin;
import cube.service.filestorage.plugin.SignOutPlugin;
import cube.service.filestorage.recycle.RecycleBin;
import cube.service.filestorage.system.DiskSystem;
import cube.service.filestorage.system.FileDescriptor;
import cube.service.filestorage.system.FileSystem;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    private long defaultFileDuration = 30L * 24 * 60 * 60 * 1000;

    /**
     * 文件大小门限。
     */
    private long fileSizeThreshold = 500 * 1024 * 1024;

    /**
     * 每个联系人的最大存储空间。
     */
    private long defaultMaxSpaceSizeForContact = (long) 1024 * 1024 * 1024;

    /**
     * 默认客户端上传速率阀值。
     */
    private long defaultUploadThreshold = 1024 * 1024;

    /**
     * 默认客户端下载速率阀值。
     */
    private long defaultDownloadThreshold = 1024 * 1024;

    private FileStorageServiceCellet cellet;

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
    protected ServiceStorage serviceStorage;

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
     * 通知器。
     */
    private Notifier notifier;

    /**
     * 插件系统。
     */
    private FileStoragePluginSystem pluginSystem;

    /**
     * 文件链接管理器。
     */
    private FileSharingManager sharingManager;

    /**
     * 构造函数。
     *
     * @param executor 多线程执行器。
     */
    public FileStorageService(FileStorageServiceCellet cellet, ExecutorService executor) {
        super();
        this.cellet = cellet;
        this.executor = executor;
        this.fileDescriptors = new ConcurrentHashMap<>();
        this.daemonTask = new DaemonTask(this);
        this.sharingManager = new FileSharingManager(this);
        this.notifier = new Notifier(this);
    }

    @Override
    public void start() {
        // 加载配置
        Properties properties = this.loadConfig();

        // 实例化 PluginSystem
        this.pluginSystem = new FileStoragePluginSystem();

        if (null != properties) {
            // 创建文件系统
            String filesystem = properties.getProperty("filesystem", "disk");
            if (filesystem.equalsIgnoreCase("disk")) {
                String path = properties.getProperty("disk.path", "storage/files");
                String host = properties.getProperty("disk.host", "127.0.0.1");
                int port = Integer.parseInt(properties.getProperty("disk.port", "6080"));

                String masterHost = properties.getProperty("disk.master.host", "127.0.0.1");
                int masterPort = Integer.parseInt(properties.getProperty("disk.master.port", "0"));

                this.fileSystem = new DiskSystem(path, host, port, masterHost, masterPort);
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

            // 最大存储空间
            this.defaultMaxSpaceSizeForContact = Long.parseLong(properties.getProperty("max.space.size",
                    Long.toString(this.defaultMaxSpaceSizeForContact)));

            // 阀值参数
            this.defaultUploadThreshold = Long.parseLong(properties.getProperty("threshold.upload",
                    Long.toString(this.defaultUploadThreshold)));
            this.defaultDownloadThreshold = Long.parseLong(properties.getProperty("threshold.download",
                    Long.toString(this.defaultDownloadThreshold)));
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

        // 初始化插件
        this.initPlugin();

        // 创建文件层级管理器
        this.fileHierarchyManager = new FileHierarchyManager(this.serviceStorage, this);

        // 回收站
        this.recycleBin = new RecycleBin(this.serviceStorage);

        // 启动分享管理器
        this.sharingManager.start();

        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;

        // 停止分享管理器
        this.sharingManager.stop();

        if (null != this.fileSystem) {
            // 停止文件系统
            this.fileSystem.stop();
        }

        if (null != this.serviceStorage) {
            // 关闭存储
            this.serviceStorage.close();
        }

        this.pluginSystem = null;
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return this.pluginSystem;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        this.daemonTask.run();
        this.fileHierarchyManager.onTick();
    }

    /**
     * 获取存储器。
     *
     * @return
     */
    protected ServiceStorage getServiceStorage() {
        return this.serviceStorage;
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
     * 获取分享管理器。
     *
     * @return 返回分享管理器实例。
     */
    public FileSharingManager getSharingManager() {
        return this.sharingManager;
    }

    public DaemonTask getDaemonTask() {
        return this.daemonTask;
    }

    protected FileHierarchyManager getFileHierarchyManager() {
        return this.fileHierarchyManager;
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected void notifyPerformance(Contact contact, Device device, long fileSpaceSize) {
        FileStoragePerformance performance = this.serviceStorage.readPerformance(contact.getDomain().getName(),
                contact.getId());
        if (null == performance) {
            performance = new FileStoragePerformance(contact.getId(), this.defaultMaxSpaceSizeForContact,
                    this.defaultUploadThreshold, this.defaultDownloadThreshold,
                    0, true, true);
            this.serviceStorage.writePerformance(contact.getDomain().getName(), performance);
        }

        // 设置当前空间大小
        performance.setSpaceSize(fileSpaceSize);

        JSONObject payload = new JSONObject();
        payload.put("code", FileStorageStateCode.Ok.code);
        payload.put("data", performance.toJSON());

        Packet packet = new Packet(FileStorageAction.Performance.name, payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                contact.getId(), contact.getDomain().getName());
        this.cellet.speak(device.getTalkContext(), dialect);
    }

    protected FileStoragePerformance getPerformance(Contact contact) {
        FileStoragePerformance performance = this.serviceStorage.readPerformance(contact.getDomain().getName(),
                contact.getId());
        if (null == performance) {
            // 没有配置性能，进行配置
            performance = new FileStoragePerformance(contact.getId(), this.defaultMaxSpaceSizeForContact,
                    this.defaultUploadThreshold, this.defaultDownloadThreshold,
                    0, true, true);
            this.serviceStorage.writePerformance(contact.getDomain().getName(), performance);
        }

        long size = this.fileHierarchyManager.countFileTotalSize(contact.getDomain().getName(),
                contact.getId());
        performance.setSpaceSize(size);

        return performance;
    }

    protected FileStoragePerformance updatePerformance(Contact contact, FileStoragePerformance performance) {
        if (contact.getId().longValue() != performance.getContactId()) {
            return null;
        }

        this.serviceStorage.writePerformance(contact.getDomain().getName(), performance);
        return performance;
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
        if (null != descriptor) {
            // 缓存文件标识
            this.fileDescriptors.put(fileCode, descriptor);
        }
    }

    /**
     * 向文件系统写入文件数据。
     *
     * @param fileCode
     * @param file
     * @return 返回文件描述。
     */
    public FileDescriptor writeFile(String fileCode, File file) {
        // 删除旧文件
        this.fileSystem.deleteFile(fileCode);

        // 写入文件系统
        FileDescriptor descriptor = this.fileSystem.writeFile(fileCode, file);
        if (null != descriptor) {
            // 缓存文件标识
            this.fileDescriptors.put(fileCode, descriptor);
        }

        return descriptor;
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

        int count = 1000;
        while (null == descriptor) {
            --count;
            if (count <= 0) {
                break;
            }

            try {
                Thread.sleep(10);
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

        String appKey = null;
        AuthToken authToken = ContactManager.getInstance().getAuthToken(fileLabel.getDomain().getName(), fileLabel.getOwnerId());
        if (null != authToken) {
            appKey = authToken.getAppKey();
        }

        // 获取外部访问的 URL 信息
        String[] urls = this.getFileURLs(fileLabel.getDomain().getName(), appKey);
        if (null == urls) {
            return null;
        }

        StringBuilder queryString = new StringBuilder("?fc=");
        queryString.append(fileLabel.getFileCode());
        queryString.append("&type=");
        queryString.append(fileLabel.getFileType().getPreferredExtension());
        fileLabel.setFileURLs(urls[0] + queryString.toString(), urls[1] + queryString.toString());

        // 设置有效期
        if (0 == fileLabel.getExpiryTime()) {
            fileLabel.setExpiryTime(fileLabel.getCompletedTime() + this.defaultFileDuration);
        }

        // 写入到存储器进行记录
        this.serviceStorage.writeFileLabel(fileLabel, descriptor);

        // 写入集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 触发 Hook
                FileStorageHook hook = pluginSystem.getSaveFileHook();
                hook.apply(new FileStoragePluginContext(fileLabel));
            }
        });

        return fileLabel;
    }

    /**
     * 保存本地文件到存储系统。
     *
     * @param fileLabel
     * @param file
     * @return
     */
    public FileLabel saveFile(FileLabel fileLabel, File file) {
        FileDescriptor descriptor = this.writeFile(fileLabel.getFileCode(), file);
        if (null == descriptor) {
            Logger.w(this.getClass(), "#saveFile - write file failed");
            return null;
        }

        // 设置直连 URL
        fileLabel.setDirectURL(descriptor.getURL());

        // 获取外部访问的 URL 信息
        String[] urls = this.getFileURLs(fileLabel.getDomain().getName(), null);
        if (null == urls) {
            return null;
        }

        String queryString = "?fc=" + fileLabel.getFileCode();
        fileLabel.setFileURLs(urls[0] + queryString, urls[1] + queryString);

        // 设置有效期
        if (0 == fileLabel.getExpiryTime()) {
            fileLabel.setExpiryTime(fileLabel.getCompletedTime() + this.defaultFileDuration);
        }

        // 写入到存储器进行记录
        this.serviceStorage.writeFileLabel(fileLabel, descriptor);

        // 写入集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        // 触发 Hook
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                FileStorageHook hook = pluginSystem.getSaveFileHook();
                hook.apply(new FileStoragePluginContext(fileLabel));
            }
        });

        return fileLabel;
    }

    /**
     * 判断指定文件是否已经构建描述符。
     *
     * @param fileCode
     * @return
     */
    public boolean hasFileDescriptor(String fileCode) {
        // 判断是否正在放置文件
        return this.fileDescriptors.containsKey(fileCode);
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
        this.serviceStorage.updateFileLabel(fileLabel);

        // 更新集群缓存
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        return true;
    }

    /**
     * 更新文件名。
     *
     * @param fileLabel
     * @param newFileName
     * @return
     */
    public FileLabel updateFileName(FileLabel fileLabel, String newFileName) {
        // 获取原文件的扩展名
        String extName = FileUtils.extractFileExtension(fileLabel.getFileName());
        // 设置新文件名，包含扩展名
        fileLabel.setFileName(newFileName + "." + extName);

        fileLabel.setLastModified(System.currentTimeMillis());

        // 更新存储
        this.serviceStorage.updateFileLabel(fileLabel);

        // 更新集群存储
        this.fileLabelCache.put(new CacheKey(fileLabel.getFileCode()), new CacheValue(fileLabel.toJSON()));

        return fileLabel;
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

        return this.serviceStorage.existsFileLabel(domainName, fileCode);
    }

    /**
     * 精确查找指定属性的文件。
     * @param domainName
     * @param fileName
     * @param lastModified
     * @param fileSize
     * @return
     */
    public FileLabel findFile(String domainName, Long contactId, String fileName, long lastModified, long fileSize) {
        String fileCode = this.serviceStorage.findFile(domainName, contactId, fileName, lastModified, fileSize);
        if (null == fileCode) {
            return null;
        }

        return this.getFile(domainName, fileCode);
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
            FileLabel fileLabel = this.serviceStorage.readFileLabel(domainName, fileCode);
            if (null == fileLabel) {
                return null;
            }

            this.fileLabelCache.put(new CacheKey(fileCode), new CacheValue(fileLabel.toJSON()));
            return fileLabel;
        }

        return new FileLabel(value.get());
    }

    /**
     * 删除文件。
     *
     * @param domainName
     * @param fileLabel
     * @return
     */
    public boolean deleteFile(String domainName, FileLabel fileLabel) {
        // 从文件系统删除
        if (!this.fileSystem.deleteFile(fileLabel.getFileCode())) {
            Logger.w(this.getClass(), "#deleteFile - Delete file failed: " +
                    fileLabel.getFileCode() + " - " + fileLabel.getFileName());
        }

        // 从缓存移除
        this.fileLabelCache.remove(new CacheKey(fileLabel.getFileCode()));

        // 从数据库里删除
        this.serviceStorage.deleteFile(domainName, fileLabel.getFileCode());

        return true;
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
     * @return 返回文件在本地的绝对路径。
     */
    public String loadFileToDisk(String domainName, String fileCode) {
        FileLabel fileLabel = this.getFile(domainName, fileCode);
        if (null == fileLabel) {
            return null;
        }

        if (fileLabel.getFileSize() > this.fileSizeThreshold) {
            Logger.w(this.getClass(), "#loadFileToDisk - File size is greater than threshold : " + fileLabel.getFileSize());
            return null;
        }

        return this.fileSystem.loadFileToDisk(fileCode);
    }

    /**
     * 检查新增大小是否超出空间上限。
     *
     * @param contact
     * @param increment
     * @return
     */
    public boolean checkFileSpaceSize(Contact contact, long increment) {
        long spaceSize = 0;
        DaemonTask.ManagedContact mc = this.daemonTask.getManagedContact(contact);
        if (null != mc) {
            if (System.currentTimeMillis() - mc.timestamp > 5 * 60 * 1000) {
                mc.spaceSize = this.fileHierarchyManager.countFileTotalSize(contact.getDomain().getName(),
                        contact.getId());
                mc.notified = false;
            }

            spaceSize = mc.spaceSize;
        }
        else {
            spaceSize = this.fileHierarchyManager.countFileTotalSize(contact.getDomain().getName(), contact.getId());
        }

        FileStoragePerformance performance = this.getPerformance(contact);
        if (spaceSize + increment > performance.getMaxSpaceSize()) {
            // 大于上限
            return false;
        }
        else {
            // 小于上限
            return true;
        }
    }

    /**
     * 刷新新的访问域到数据库。
     * @param authDomain
     */
    public void refreshAuthDomain(AuthDomain authDomain) {
        List<String> list = new ArrayList<>();
        list.add(authDomain.domainName);
        this.serviceStorage.execSelfChecking(list);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object notify(Object event) {
        if (event instanceof PrimitiveInputStream) {
            PrimitiveInputStream inputStream = (PrimitiveInputStream) event;
            // 写入文件数据
            this.writeFile(inputStream.getName(), inputStream);

            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return event;
        }
        else if (event instanceof JSONObject) {
            return this.notifier.deliver((JSONObject) event);
        }
        else if (event instanceof File) {
            String filename = ((File) event).getName();
            return this.writeFile(filename, (File) event);
        }

        return null;
    }

    /**
     * 从授权模块获取指定域对应的文件访问地址。
     *
     * @param domain
     * @param appKey
     * @return
     */
    private String[] getFileURLs(String domain, String appKey) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        PrimaryDescription description = authService.getPrimaryDescription(domain, appKey);
        if (null == description) {
            return null;
        }
        JSONObject primary = description.getPrimaryContent();
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
                this.serviceStorage = new ServiceStorage(this.executor, StorageType.SQLite, config);
            }
            else {
                this.serviceStorage = new ServiceStorage(this.executor, StorageType.MySQL, config);
            }
        }
        else {
            config.put("file", "storage/FileStorageService.db");
            this.serviceStorage = new ServiceStorage(this.executor, StorageType.SQLite, config);
        }

        (new Thread() {
            @Override
            public void run() {
                // 打开存储器
                serviceStorage.open();

                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                serviceStorage.execSelfChecking(authService.getDomainList());

                // 激活磁盘集群
                if (fileSystem instanceof DiskSystem) {
                    ((DiskSystem) fileSystem).activateCluster(serviceStorage.getType(), serviceStorage.getConfig());
                }
            }
        }).start();
    }

    private void initPlugin() {
        (new Thread() {
            @Override
            public void run() {
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                PluginSystem<?> pluginSystem = authService.getPluginSystem();
                while (null == pluginSystem) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    pluginSystem = authService.getPluginSystem();
                }

                pluginSystem.register(AuthServiceHook.CreateDomainApp,
                        new CreateDomainAppPlugin(FileStorageService.this));

                // 监听联系人登录
                while (!ContactManager.getInstance().isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignIn,
                        new SignInPlugin(FileStorageService.this));
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignOut,
                        new SignOutPlugin(FileStorageService.this));
            }
        }).start();
    }
}
