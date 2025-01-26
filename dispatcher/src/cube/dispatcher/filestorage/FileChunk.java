/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件数据块。
 */
public class FileChunk implements Comparable<FileChunk> {

    /**
     * 文件块关联的联系人 ID 。
     */
    protected long contactId;

    /**
     * 文件块关联的域。
     */
    protected String domain;

    /**
     * 文件块当前对应的令牌。
     */
    protected String token;

    /**
     * 文件名。
     */
    protected String fileName;

    /**
     * 文件大小。
     */
    protected long fileSize;

    /**
     * 文件最后修改时间。
     */
    protected long lastModified;

    /**
     * 当前数据库对应的文件的游标。
     */
    protected long cursor;

    /**
     * 当前数据库包含的数据长度。
     */
    protected int size;

    /**
     * 当前数据库在整体数据中的结束位置。
     */
    protected long position;

    /**
     * 当前文件块的数据。
     */
    private byte[] data;

    /**
     * 当前文件块数据的临时文件。
     */
    private Path dataFile;

    /**
     * 构造函数。
     *
     * @param contactId
     * @param domain
     * @param token
     * @param fileName
     * @param fileSize
     * @param cursor
     * @param size
     * @param data
     */
    public FileChunk(long contactId, String domain, String token, String fileName,
                     long fileSize, long lastModified, long cursor, int size, byte[] data) {
        this.contactId = contactId;
        this.domain = domain;
        this.token = token;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.cursor = cursor;
        this.size = size;
        this.data = data;
        this.position = cursor + size;
    }

    /**
     * 将数据写到临时文件。
     */
    public void flush(Path path, String filename) {
        this.dataFile = Paths.get(path.toString(), filename);
        try {
            Files.write(this.dataFile, this.data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.data = null;
    }

    public byte[] getData() {
        if (null == this.data && null != this.dataFile) {
            try {
                return Files.readAllBytes(this.dataFile);
            } catch (IOException e) {
                return null;
            }
        }
        else {
            return this.data;
        }
    }

    public void clear() {
        this.data = null;
        if (null != this.dataFile) {
            try {
                Files.delete(this.dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof FileChunk) {
            FileChunk other = (FileChunk) object;
            if (other.cursor == this.cursor) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int compareTo(FileChunk other) {
        return (int) (this.cursor - other.cursor);
    }
}
