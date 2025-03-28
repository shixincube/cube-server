/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索过滤器。
 */
public class SearchFilter {

    protected List<FileType> fileTypes = new ArrayList<>();

    protected List<String> extensions = new ArrayList<>();

    /**
     * 名称关键字。
     */
    protected List<String> nameKeywords = new ArrayList<>();

    private List<String> lowerCaseNameKeywords = new ArrayList<>();

    protected boolean inverseOrder = true;

    protected int beginIndex = 0;

    protected int endIndex = 14;

    private int hash = 0;

    /**
     * 构造函数。
     *
     * @param json
     */
    public SearchFilter(JSONObject json) {
        if (json.has("begin")) {
            this.beginIndex = json.getInt("begin");
        }
        if (json.has("end")) {
            this.endIndex = json.getInt("end");
        }

        if (json.has("nameKeywords")) {
            JSONArray array = json.getJSONArray("nameKeywords");
            for (int i = 0; i < array.length(); ++i) {
                String keyword = array.getString(i);
                this.nameKeywords.add(keyword);
                this.lowerCaseNameKeywords.add(keyword.toLowerCase());
            }
        }

        if (json.has("types")) {
            JSONArray array = json.getJSONArray("types");
            for (int i = 0; i < array.length(); ++i) {
                String typeString = array.getString(i);
                this.extensions.add(typeString.toLowerCase());

                FileType fileType = FileType.matchExtension(typeString);
                if (fileType != FileType.UNKNOWN && fileType != FileType.FILE) {
                    this.fileTypes.add(fileType);
                }
            }
        }
        else {
            this.fileTypes.add(FileType.UNKNOWN);
        }
    }

    /**
     * 判断指定的文件类型是否包含在搜索条件里。
     *
     * @param fileLabel
     * @return
     */
    public boolean containsFileType(FileLabel fileLabel) {
        if (this.fileTypes.isEmpty()) {
            return false;
        }

        if (this.fileTypes.contains(fileLabel.getFileType())) {
            return true;
        }

        return this.extensions.contains(fileLabel.getFileExtension().toLowerCase());
    }

    /**
     * 判断指定的文件名是否包含关键词。
     *
     * @param fileLabel
     * @return
     */
    public boolean containsFileName(FileLabel fileLabel) {
        String filename = FileUtils.extractFileName(fileLabel.getFileName()).toLowerCase();
        for (String word : this.lowerCaseNameKeywords) {
            if (filename.contains(word)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断指定的目录是否包含关键词。
     *
     * @param directory
     * @return
     */
    public boolean containsDirectoryName(Directory directory) {
        String name = directory.getName().toLowerCase();
        for (String word : this.lowerCaseNameKeywords) {
            if (name.contains(word)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (null == object || !(object instanceof SearchFilter)) {
            return false;
        }

        SearchFilter other = (SearchFilter) object;
        if (inverseOrder != other.inverseOrder || beginIndex != other.beginIndex
                || endIndex != other.endIndex || fileTypes.size() != other.fileTypes.size()
                || nameKeywords.size() != other.nameKeywords.size()) {
            return false;
        }

        for (FileType type : fileTypes) {
            if (!other.fileTypes.contains(type)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (0 == this.hash) {
            StringBuilder buf = new StringBuilder();

            for (FileType fileType : this.fileTypes) {
                buf.append(fileType.getExtensions()[0]);
            }

            for (String word : this.nameKeywords) {
                buf.append(word);
            }

            buf.append(this.inverseOrder);
            buf.append(this.beginIndex);
            buf.append(this.endIndex);
            this.hash = buf.toString().hashCode();
        }

        return this.hash;
    }
}
