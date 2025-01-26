/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

/**
 * 区块标签。
 */
public class FileChunkTag implements Comparable<FileChunkTag> {

    protected long contactId;

    protected String domain;

    protected String fileName;

    protected String fileCode;

    private int hashCode = 0;

    public FileChunkTag(FileChunk chunk, String fileCode) {
        this.contactId = chunk.contactId;
        this.domain = chunk.domain;
        this.fileName = chunk.fileName;
        this.fileCode = fileCode;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof FileChunkTag) {
            FileChunkTag other = (FileChunkTag) object;
            return other.contactId == this.contactId &&
                    other.domain.equals(this.domain) &&
                    other.fileName.equals(this.fileName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (0 == this.hashCode) {
            Long id = this.contactId;
            this.hashCode = id.hashCode() + this.domain.hashCode() + this.fileName.hashCode();
        }

        return this.hashCode;
    }

    @Override
    public int compareTo(FileChunkTag other) {
        if (other.contactId == this.contactId &&
                other.domain.equals(this.domain) &&
                other.fileName.equals(this.fileName)) {
            return 0;
        }
        else {
            return this.hashCode() - other.hashCode();
        }
    }
}
