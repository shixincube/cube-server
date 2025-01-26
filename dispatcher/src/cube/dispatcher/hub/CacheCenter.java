/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub;

import cube.common.entity.FileLabel;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存中心。
 */
public class CacheCenter {

    private final static CacheCenter instance = new CacheCenter();

    private Path workPath = Paths.get("cube-hub-files");

    private long fileCacheDuration = 30L * 24 * 60 * 60 * 1000;

    private ConcurrentHashMap<String, FileLabel> fileLabelMap;

    private CacheCenter() {
        this.fileLabelMap = new ConcurrentHashMap<>();

        if (!Files.exists(this.workPath)) {
            try {
                Files.createDirectories(this.workPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public final static CacheCenter getInstance() {
        return CacheCenter.instance;
    }

    public Path getWorkPath() {
        return this.workPath;
    }

    /**
     * 写入文件标签数据。
     *
     * @param fileLabel
     * @return
     */
    public File putFileLabel(FileLabel fileLabel) {
        this.fileLabelMap.put(fileLabel.getFileCode(), fileLabel);

        // 写入到缓存目录
        File file = new File(this.workPath.toFile(), fileLabel.getFileCode() + ".cfl");
        if (file.exists()) {
            file.delete();
        }

        try {
            Files.write(Paths.get(file.getAbsolutePath()), fileLabel.toJSON().toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new File(this.workPath.toFile(),
                fileLabel.getFileCode() + "." + fileLabel.getFileType().getPreferredExtension());
    }

    /**
     * 尝试读取已缓存的文件数据。
     *
     * @param fileCode
     * @return
     */
    public CachedFile tryGetFile(String fileCode) {
        FileLabel fileLabel = this.fileLabelMap.get(fileCode);

        if (null == fileLabel) {
            File labelFile = new File(this.workPath.toFile(), fileCode + ".cfl");
            if (labelFile.exists()) {
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(labelFile.getAbsolutePath()));
                    String jsonString = new String(bytes, StandardCharsets.UTF_8);
                    fileLabel = new FileLabel(new JSONObject(jsonString));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (null != fileLabel) {
                this.fileLabelMap.put(fileCode, fileLabel);
            }
        }

        if (null == fileLabel) {
            return null;
        }

        File file = new File(this.workPath.toFile(),
                fileCode + "." + fileLabel.getFileType().getPreferredExtension());
        return file.exists() ? new CachedFile(file, fileLabel) : null;
    }

    public void lock(String channelCode) {
        synchronized (this) {

        }
    }

    public void unlock(String channelCode) {
        synchronized (this) {

        }
    }

    public void selfChecking() {
        // 遍历目录，删除超期文件
        File[] files = this.workPath.toFile().listFiles();
        if (null != files && files.length > 0) {
            long now = System.currentTimeMillis();
            for (File file : files) {
                if (now - file.lastModified() > this.fileCacheDuration) {
                    // 文件失效，删除
                    file.delete();
                }
            }
        }
    }

    public class CachedFile {

        public final File file;

        public final FileLabel fileLabel;

        public CachedFile(File file, FileLabel fileLabel) {
            this.file = file;
            this.fileLabel = fileLabel;
        }
    }
}
