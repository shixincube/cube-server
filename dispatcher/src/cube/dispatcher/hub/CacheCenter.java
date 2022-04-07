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

package cube.dispatcher.hub;

import cube.common.entity.FileLabel;
import org.json.JSONObject;

import java.io.File;
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
