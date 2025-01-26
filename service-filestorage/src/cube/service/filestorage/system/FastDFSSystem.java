/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.system;

import java.io.File;
import java.io.InputStream;

/**
 * FastDFS 文件系统。
 */
public class FastDFSSystem implements FileSystem {

    public FastDFSSystem() {
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public FileDescriptor writeFile(String fileCode, File file) {
        return null;
    }

    @Override
    public FileDescriptor writeFile(String fileCode, InputStream inputStream) {
        return null;
    }

    @Override
    public boolean isWriting(String fileCode) {
        return false;
    }

    @Override
    public boolean deleteFile(String fileCode) {
        return false;
    }

    @Override
    public boolean existsFile(String fileCode) {
        return false;
    }

    @Override
    public String loadFileToDisk(String fileCode) {
        return null;
    }
}
