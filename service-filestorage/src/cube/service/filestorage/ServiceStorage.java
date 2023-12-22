/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.UniqueKey;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.filestorage.recycle.DirectoryTrash;
import cube.service.filestorage.recycle.FileTrash;
import cube.service.filestorage.system.FileDescriptor;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.FileType;
import cube.util.SQLUtils;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件码存储器。
 * 用于管理文件码存储。
 */
public class ServiceStorage implements Storagable {

    /**
     * 文件标签表。
     */
    private final String labelTablePrefix = "label_";

    /**
     * 文件描述符表。
     */
    private final String descriptorTablePrefix = "descriptor_";

    /**
     * 文件层级结构节点表。
     */
    private final String hierarchyTablePrefix = "hierarchy_";

    /**
     * 文件系统偏好配置表。
     */
    private final String performanceTablePrefix = "file_performance_";

    /**
     * 回收站表。
     */
    private final String recyclebinTablePrefix = "recyclebin_";

    /**
     * 分享标签表。
     */
    private final String sharingTagTablePrefix = "sharing_tag_";

    /**
     * 分享标签的预览图表。
     */
    private final String sharingTagPreviewTablePrefix = "sharing_tag_preview_";

    /**
     * 分享追踪记录表。
     */
    private final String visitTraceTablePrefix = "sharing_trace_";

    /**
     * 分享码表。
     */
    private final String sharingCodeTable = "sharing_code";

    /**
     * 文件标签字段。
     */
    private final StorageField[] labelFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("file_code", LiteralBase.STRING),
            new StorageField("owner_id", LiteralBase.LONG),
            new StorageField("file_name", LiteralBase.STRING),
            new StorageField("file_size", LiteralBase.LONG),
            new StorageField("last_modified", LiteralBase.LONG),
            new StorageField("completed_time", LiteralBase.LONG),
            new StorageField("expiry_time", LiteralBase.LONG),
            new StorageField("file_type", LiteralBase.STRING),
            new StorageField("md5", LiteralBase.STRING),
            new StorageField("sha1", LiteralBase.STRING),
            new StorageField("file_url", LiteralBase.STRING),
            new StorageField("file_secure_url", LiteralBase.STRING),
            new StorageField("direct_url", LiteralBase.STRING)
    };

    /**
     * 文件描述符字段。
     */
    private final StorageField[] descriptorFields = new StorageField[] {
            new StorageField("file_code", LiteralBase.STRING),
            new StorageField("system", LiteralBase.STRING),
            new StorageField("file_name", LiteralBase.STRING),
            new StorageField("url", LiteralBase.STRING),
            new StorageField("descriptor", LiteralBase.STRING),
            new StorageField("timestamp", LiteralBase.LONG)
    };

    /**
     * 联系人文件存储服务偏好配置。
     */
    private final StorageField[] performanceFields = new StorageField[] {
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("max_space_size", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("upload_threshold", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("download_threshold", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("max_sharing_num", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("sharing_watermark_enabled", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_1
            }),
            new StorageField("sharing_preview_enabled", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_1
            })
    };

    /**
     * 层级表字段。
     */
    private final StorageField[] hierarchyFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("node_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("root_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("last_modified", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    /**
     * 回收站表字段。
     */
    private final StorageField[] recyclebinFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("timestamp", LiteralBase.LONG),
            new StorageField("root_id", LiteralBase.LONG),
            new StorageField("parent_id", LiteralBase.LONG),
            new StorageField("original_id", LiteralBase.LONG),
            new StorageField("file_code", LiteralBase.STRING),
            new StorageField("data", LiteralBase.STRING)
    };

    private final StorageField[] sharingTagFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("duration", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expiry", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("password", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("preview", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("download", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_1
            }),
            new StorageField("trace_download", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_1
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private final StorageField[] sharingTagPreviewFields = new StorageField[]{
            new StorageField("sn", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 标签 ID
            new StorageField("tag_id", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 标签码
            new StorageField("tag_code", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("file_label", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] visitTraceFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("platform", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("address", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("url", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("screen", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("language", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("user_agent", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("agent", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("event", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("event_tag", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("event_param", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("contact_domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("sharer", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("parent", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private final StorageField[] sharingCodeFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private ExecutorService executor;

    private Storage storage;

    /**
     * 文件标签表。
     */
    private Map<String, String> labelTableNameMap;

    /**
     * 文件描述符表。
     */
    private Map<String, String> descriptorTableNameMap;

    /**
     * 服务偏好设置表。
     */
    private Map<String, String> performanceTableNameMap;

    /**
     * 层级表。
     */
    private Map<String, String> hierarchyTableNameMap;

    /**
     * 回收站表。
     */
    private Map<String, String> recyclebinTableNameMap;

    /**
     * 分享标签表。
     */
    private Map<String, String> sharingTagTableNameMap;

    /**
     * 分享预览文件标签表。
     */
    private Map<String, String> sharingTagPreviewTableNameMap;

    /**
     * 访问追踪数据表。
     */
    private Map<String, String> visitTraceTableNameMap;

    public ServiceStorage(ExecutorService executorService, StorageType type, JSONObject config) {
        this.executor = executorService;
        this.storage = StorageFactory.getInstance().createStorage(type, "FileStructStorage", config);
        this.labelTableNameMap = new HashMap<>();
        this.descriptorTableNameMap = new HashMap<>();
        this.performanceTableNameMap = new HashMap<>();
        this.hierarchyTableNameMap = new HashMap<>();
        this.recyclebinTableNameMap = new HashMap<>();
        this.sharingTagTableNameMap = new HashMap<>();
        this.sharingTagPreviewTableNameMap = new HashMap<>();
        this.visitTraceTableNameMap = new HashMap<>();
    }

    public StorageType getType() {
        return this.storage.getType();
    }

    public JSONObject getConfig() {
        return this.storage.getConfig();
    }

    @Override
    public void open() {
        this.storage.open();
    }

    @Override
    public void close() {
        this.storage.close();
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            // 检查标签表
            this.checkLabelTable(domain);

            // 检查描述符表
            this.checkDescriptorTable(domain);

            // 检测偏好配置表
            this.checkPerformanceTable(domain);

            // 检查层级表
            this.checkHierarchyTable(domain);

            // 检查回收站表
            this.checkRecyclebinTable(domain);

            this.checkSharingTagTable(domain);
            this.checkSharingTagPreviewTable(domain);
            this.checkVisitTraceTable(domain);
        }

        this.checkSharingCodeTable();
    }

    /**
     * 是否存在该文件标签。
     *
     * @param domain
     * @param fileCode
     * @return
     */
    public boolean existsFileLabel(String domain, String fileCode) {
        String labelTable = this.labelTableNameMap.get(domain);

        List<StorageField[]> result = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileCode))
        });

        return (!result.isEmpty());
    }

    /**
     * 更新文件标签。
     *
     * @param fileLabel
     */
    public void updateFileLabel(final FileLabel fileLabel) {
        String labelTable = this.labelTableNameMap.get(fileLabel.getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeUpdate(labelTable, new StorageField[] {
                        new StorageField("file_name", LiteralBase.STRING, fileLabel.getFileName()),
                        new StorageField("last_modified", LiteralBase.LONG, fileLabel.getLastModified()),
                        new StorageField("expiry_time", LiteralBase.LONG, fileLabel.getExpiryTime())
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()))
                });
            }
        });
    }

    /**
     * 写入文件标签。
     *
     * @param fileLabel
     * @param fileDescriptor
     */
    public void writeFileLabel(final FileLabel fileLabel, final FileDescriptor fileDescriptor) {
        String labelTable = this.labelTableNameMap.get(fileLabel.getDomain().getName());
        String descriptorTable = this.descriptorTableNameMap.get(fileLabel.getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] labelFields = new StorageField[] {
                        new StorageField("id", LiteralBase.LONG, fileLabel.getId()),
                        new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()),
                        new StorageField("owner_id", LiteralBase.LONG, fileLabel.getOwnerId()),
                        new StorageField("file_name", LiteralBase.STRING, fileLabel.getFileName()),
                        new StorageField("file_size", LiteralBase.LONG, fileLabel.getFileSize()),
                        new StorageField("last_modified", LiteralBase.LONG, fileLabel.getLastModified()),
                        new StorageField("completed_time", LiteralBase.LONG, fileLabel.getCompletedTime()),
                        new StorageField("expiry_time", LiteralBase.LONG, fileLabel.getExpiryTime()),
                        new StorageField("file_type", LiteralBase.STRING, fileLabel.getFileType().getPreferredExtension()),
                        new StorageField("md5", LiteralBase.STRING, fileLabel.getMD5Code()),
                        new StorageField("sha1", LiteralBase.STRING, fileLabel.getSHA1Code()),
                        new StorageField("file_url", LiteralBase.STRING, fileLabel.getFileURL()),
                        new StorageField("file_secure_url", LiteralBase.STRING, fileLabel.getFileSecureURL()),
                        new StorageField("direct_url", LiteralBase.STRING, fileLabel.getDirectURL())
                };

                StorageField[] descriptorFields = new StorageField[]{
                        new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()),
                        new StorageField("system", LiteralBase.STRING, fileDescriptor.getFileSystem()),
                        new StorageField("file_name", LiteralBase.STRING, fileDescriptor.getFileName()),
                        new StorageField("url", LiteralBase.STRING, fileDescriptor.getURL()),
                        new StorageField("descriptor", LiteralBase.STRING, fileDescriptor.getDescriptor().toString()),
                        new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
                };

                // 判断是否已经写入数据
                List<StorageField[]> result = storage.executeQuery(labelTable, new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("file_code", fileLabel.getFileCode()))
                });

                if (result.isEmpty()) {
                    // 没有数据，插入新数据
                    // 标签表
                    storage.executeInsert(labelTable, labelFields);

                    // 描述符表
                    storage.executeInsert(descriptorTable, descriptorFields);
                }
                else {
                    // 更新数据
                    // 标签表
                    storage.executeUpdate(labelTable, labelFields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("file_code", fileLabel.getFileCode()))
                    });

                    // 描述符表
                    storage.executeUpdate(descriptorTable, descriptorFields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("file_code", fileLabel.getFileCode()))
                    });
                }
            }
        });
    }

    /**
     * 读取指定域下对应文件码的文件标签。
     *
     * @param domain
     * @param fileCode
     * @return
     */
    public FileLabel readFileLabel(String domain, String fileCode) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(labelTable, this.labelFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("file_code", fileCode))
        });

        if (!result.isEmpty()) {
            StorageField[] fields = result.get(0);

            // 将字段转为映射关系，便于代码阅读
            Map<String, StorageField> map = StorageFields.get(fields);

            FileLabel label = new FileLabel(map.get("id").getLong(), domain, map.get("file_code").getString(),
                    map.get("owner_id").getLong(), map.get("file_name").getString(), map.get("file_size").getLong(),
                    map.get("last_modified").getLong(), map.get("completed_time").getLong(), map.get("expiry_time").getLong());

            label.setFileType(FileType.matchExtension(map.get("file_type").getString()));

            if (!map.get("md5").isNullValue()) {
                label.setMD5Code(map.get("md5").getString());
            }

            if (!map.get("sha1").isNullValue()) {
                label.setSHA1Code(map.get("sha1").getString());
            }

            label.setFileURLs(map.get("file_url").getString(),
                    (map.get("file_secure_url").isNullValue()) ? null : map.get("file_secure_url").getString());

            label.setDirectURL(map.get("direct_url").getString());

            return label;
        }

        return null;
    }

    /**
     * 删除文件记录。
     *
     * @param domain
     * @param fileCode
     * @return
     */
    public FileLabel deleteFile(String domain, String fileCode) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        FileLabel deleted = this.readFileLabel(domain, fileCode);
        if (null == deleted) {
            return null;
        }

        this.storage.executeDelete(labelTable, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });

        String descriptorTable = this.descriptorTableNameMap.get(domain);
        if (null != descriptorTable) {
            this.storage.executeDelete(descriptorTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }

        String recyclebinTable = this.recyclebinTableNameMap.get(domain);
        if (null != recyclebinTable) {
            this.storage.executeDelete(recyclebinTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }

        return deleted;
    }

    /**
     * 删除文件记录。
     *
     * @param domain
     * @param contactId
     * @param fileCode
     * @return
     */
    public FileLabel deleteFile(String domain, long contactId, String fileCode) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        FileLabel deleted = this.readFileLabel(domain, fileCode);
        if (null == deleted) {
            return null;
        }

        this.storage.executeDelete(labelTable, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_code", fileCode)
        });

        String descriptorTable = this.descriptorTableNameMap.get(domain);
        if (null != descriptorTable) {
            this.storage.executeDelete(descriptorTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }

        String recyclebinTable = this.recyclebinTableNameMap.get(domain);
        if (null != recyclebinTable) {
            this.storage.executeDelete(recyclebinTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }

        return deleted;
    }

    /**
     * 通过 MD5 码删除文件。
     *
     * @param domain
     * @param contactId
     * @param md5Code
     * @return 返回删除的文件标签列表。
     */
    public List<FileLabel> deleteFileByMD5(String domain, long contactId, String md5Code) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        List<StorageField[]> list = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("file_code", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("md5", md5Code)
        });

        if (list.isEmpty()) {
            return null;
        }

        List<FileLabel> fileList = new ArrayList<>(list.size());

        for (StorageField[] data : list) {
            String fileCode = data[0].getString();
            FileLabel fileLabel = this.readFileLabel(domain, fileCode);
            if (null != fileLabel) {
                fileList.add(fileLabel);
            }
        }

        this.storage.executeDelete(labelTable, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("md5", md5Code)
        });

        String descriptorTable = this.descriptorTableNameMap.get(domain);
        if (null != descriptorTable) {
            for (FileLabel file : fileList) {
                this.storage.executeDelete(descriptorTable, new Conditional[] {
                        Conditional.createEqualTo("file_code", file.getFileCode())
                });
            }
        }

        String recyclebinTable = this.recyclebinTableNameMap.get(domain);
        if (null != recyclebinTable) {
            for (FileLabel file : fileList) {
                this.storage.executeDelete(recyclebinTable, new Conditional[] {
                        Conditional.createEqualTo("file_code", file.getFileCode())
                });
            }
        }

        return fileList;
    }

    /**
     * 查找所有指定超期时间的文件标签。
     *
     * @param domain
     * @param expiryTime
     * @return
     */
    public List<FileLabel> listFileLabel(String domain, long expiryTime) {
        List<FileLabel> list = new ArrayList<>();
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(labelTable, this.labelFields, new Conditional[] {
                Conditional.createLessThan(new StorageField("expiry_time", expiryTime)),
                Conditional.createAnd(),
                Conditional.createUnequalTo("expiry_time", 0)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            FileLabel label = new FileLabel(map.get("id").getLong(), domain, map.get("file_code").getString(),
                    map.get("owner_id").getLong(), map.get("file_name").getString(), map.get("file_size").getLong(),
                    map.get("last_modified").getLong(), map.get("completed_time").getLong(), map.get("expiry_time").getLong());

            label.setFileType(FileType.matchExtension(map.get("file_type").getString()));

            if (!map.get("md5").isNullValue()) {
                label.setMD5Code(map.get("md5").getString());
            }

            if (!map.get("sha1").isNullValue()) {
                label.setSHA1Code(map.get("sha1").getString());
            }

            label.setFileURLs(map.get("file_url").getString(),
                    (map.get("file_secure_url").isNullValue()) ? null : map.get("file_secure_url").getString());

            label.setDirectURL(map.get("direct_url").getString());

            list.add(label);
        }

        return list;
    }

    /**
     * 获取指定所有人的全部文件。
     *
     * @param domain
     * @param ownerId
     * @return
     */
    public List<FileLabel> listFileLabelByOwnerId(String domain, long ownerId) {
        List<FileLabel> list = new ArrayList<>();
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(labelTable, this.labelFields, new Conditional[] {
                Conditional.createEqualTo("owner_id", ownerId)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            FileLabel label = new FileLabel(map.get("id").getLong(), domain, map.get("file_code").getString(),
                    map.get("owner_id").getLong(), map.get("file_name").getString(), map.get("file_size").getLong(),
                    map.get("last_modified").getLong(), map.get("completed_time").getLong(), map.get("expiry_time").getLong());

            label.setFileType(FileType.matchExtension(map.get("file_type").getString()));

            if (!map.get("md5").isNullValue()) {
                label.setMD5Code(map.get("md5").getString());
            }

            if (!map.get("sha1").isNullValue()) {
                label.setSHA1Code(map.get("sha1").getString());
            }

            label.setFileURLs(map.get("file_url").getString(),
                    (map.get("file_secure_url").isNullValue()) ? null : map.get("file_secure_url").getString());

            label.setDirectURL(map.get("direct_url").getString());

            list.add(label);
        }

        return list;
    }

    /**
     * 查找指定文件名、修改日期和文件大小的文件。
     *
     * @param domain
     * @param contactId
     * @param fileName
     * @param lastModified
     * @param fileSize
     * @return 如果找到该文件返回文件码，否则返回 {@code null} 值。
     */
    public String findFile(String domain, Long contactId, String fileName, long lastModified, long fileSize) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        String fileCode = null;
        List<StorageField[]> result = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("file_code", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId.longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_name", fileName),
                Conditional.createAnd(),
                Conditional.createEqualTo("last_modified", lastModified),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_size", fileSize)
        });

        if (!result.isEmpty()) {
            fileCode = result.get(0)[0].getString();
        }

        return fileCode;
    }

    /**
     * 按照文件的 MD5 码查找文件。
     *
     * @param domain
     * @param contactId
     * @param md5
     * @return 如果找到该文件返回文件码，否则返回 {@code null} 值。
     */
    public List<String> findFilesByMD5(String domain, long contactId, String md5) {
        List<String> result = new ArrayList<>();

        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        List<StorageField[]> list = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("file_code", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("md5", md5)
        });

        for (StorageField[] data : list) {
            result.add(data[0].getString());
        }

        return result;
    }

    /**
     * 按照文件名查找文件。
     *
     * @param domain
     * @param contactId
     * @param fileName
     * @return
     */
    public List<String> findFilesByFileName(String domain, long contactId, String fileName) {
        List<String> result = new ArrayList<>();

        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return result;
        }

        List<StorageField[]> list = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("file_code", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("owner_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_name", fileName)
        });

        for (StorageField[] data : list) {
            result.add(data[0].getString());
        }

        return result;
    }

    /**
     * 读取指定文件的完成时间。
     *
     * @param domain
     * @param fileCode
     * @return
     */
    public long readFileCompletedTime(String domain, String fileCode) {
        String table = this.labelTableNameMap.get(domain);
        if (null == table) {
            return -1;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("completed_time", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });
        if (result.isEmpty()) {
            return -1;
        }

        return result.get(0)[0].getLong();
    }

    /**
     * 读取偏好配置。
     *
     * @param domain
     * @param contactId
     * @return
     */
    public FileStoragePerformance readPerformance(String domain, long contactId) {
        String table = this.performanceTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.performanceFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        FileStoragePerformance performance = new FileStoragePerformance(contactId, map.get("max_space_size").getLong(),
                map.get("upload_threshold").getLong(), map.get("download_threshold").getLong(),
                map.get("max_sharing_num").getInt(),
                map.get("sharing_watermark_enabled").getInt() == 1,
                map.get("sharing_preview_enabled").getInt() == 1);

        return performance;
    }

    /**
     * 写入偏好配置。
     *
     * @param domain
     * @param performance
     */
    public void writePerformance(String domain, FileStoragePerformance performance) {
        String table = this.performanceTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("contact_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", performance.getContactId())
        });

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("contact_id", performance.getContactId()),
                    new StorageField("max_space_size", performance.getMaxSpaceSize()),
                    new StorageField("upload_threshold", performance.getUploadThreshold()),
                    new StorageField("download_threshold", performance.getDownloadThreshold()),
                    new StorageField("max_sharing_num", performance.getMaxSharingNum()),
                    new StorageField("sharing_watermark_enabled", performance.isSharingWatermarkEnabled() ? 1 : 0),
                    new StorageField("sharing_preview_enabled", performance.isSharingPreviewEnabled() ? 1 : 0)
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(table, new StorageField[] {
                    new StorageField("max_space_size", performance.getMaxSpaceSize()),
                    new StorageField("upload_threshold", performance.getUploadThreshold()),
                    new StorageField("download_threshold", performance.getDownloadThreshold()),
                    new StorageField("max_sharing_num", performance.getMaxSharingNum()),
                    new StorageField("sharing_watermark_enabled", performance.isSharingWatermarkEnabled() ? 1 : 0),
                    new StorageField("sharing_preview_enabled", performance.isSharingPreviewEnabled() ? 1 : 0)
            }, new Conditional[] {
                    Conditional.createEqualTo("contact_id", performance.getContactId())
            });
        }
    }

    /**
     * 统计指定联系人所属的所有文件大小。
     * @param domain
     * @param contactId
     * @return 如果统计出错返回 {@code -1} 值。
     */
    public long countFileTotalSize(String domain, long contactId) {
        String table = this.labelTableNameMap.get(domain);
        if (null == table) {
            return -1;
        }

        long total = 0;
        StringBuilder sql = new StringBuilder("SELECT SUM(`file_size`) FROM `");
        sql.append(table);
        sql.append("` WHERE `owner_id`=");
        sql.append(contactId);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (!result.isEmpty()) {
            total = result.get(0)[0].getLong();
        }

        return total;
    }

    /**
     * 过滤指定 ROOT 里时间段内的节点数据。
     *
     * @param domain
     * @param rootId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<JSONObject> filterHierarchyNodes(String domain, long rootId, long beginTime, long endTime) {
        List<JSONObject> list = new ArrayList<>();
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.hierarchyFields, new Conditional[] {
                Conditional.createEqualTo("root_id", rootId),
                Conditional.createAnd(),
                Conditional.createGreaterThanEqual(new StorageField("last_modified", beginTime)),
                Conditional.createAnd(),
                Conditional.createLessThan(new StorageField("last_modified", endTime))
        });

        for (StorageField[] fields : result) {
            list.add(new JSONObject(fields[4].getString()));
        }

        return list;
    }

    /**
     * 写入节点数据。
     *
     * @param domain
     * @param nodeId
     * @param json
     */
    public void writeHierarchyNode(String domain, long nodeId, JSONObject json) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("node_id", nodeId),
                new StorageField("root_id", json.getJSONObject("context").getLong("root")),
                new StorageField("last_modified", json.getJSONObject("context").getLong("lastModified")),
                new StorageField("data", json.toString())
        };

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", nodeId))
        });

        if (result.isEmpty()) {
            this.storage.executeInsert(table, fields);
        }
        else {
            this.storage.executeUpdate(table, fields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("node_id", nodeId))
            });
        }
    }

    /**
     * 读取节点数据。
     *
     * @param domain
     * @param nodeId
     * @return
     */
    public JSONObject readHierarchyNode(String domain, long nodeId) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.hierarchyFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", nodeId))
        });

        if (!result.isEmpty()) {
            JSONObject data = new JSONObject(result.get(0)[4].getString());

            // 兼容低版本数据库结构
            AtomicLong rootId = new AtomicLong(result.get(0)[2].getLong());
            if (0 == rootId.get()) {
                if (data.has("parent")) {
                    rootId.set(UniqueKey.extractId(data.getString("parent")));
                }
                else {
                    rootId.set(nodeId);
                }
                this.executor.execute(() -> {
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("root_id", rootId.get())
                    }, new Conditional[] {
                            Conditional.createEqualTo("sn", result.get(0)[0].getLong())
                    });
                });
            }

            return data;
        }

        return null;
    }

    /**
     * 删除节点数据。
     *
     * @param domain
     * @param nodeId
     */
    public void deleteHierarchyNode(String domain, long nodeId) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", nodeId))
        });
    }

    /**
     * 列出指定索引范围内的回收站数据。
     *
     * @param domain
     * @param rootId
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<JSONObject> listTrash(String domain, Long rootId, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return null;
        }

        String table = this.recyclebinTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("data", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField("root_id", LiteralBase.LONG, rootId)),
                Conditional.createLimit(beginIndex, (endIndex - beginIndex + 1))
        });

        List<JSONObject> jsonList = new ArrayList<>();
        for (StorageField[] data : result) {
            jsonList.add(new JSONObject(data[0].getString()));
        }
        return jsonList;
    }

    /**
     * 将废弃目录写入回收站。
     *
     * @param trash
     */
    public void writeDirectoryTrash(DirectoryTrash trash) {
        String table = this.recyclebinTableNameMap.get(trash.getDomainName());
        if (null == table) {
            return;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, trash.getId()),
                new StorageField("timestamp", LiteralBase.LONG, trash.getTimestamp()),
                new StorageField("root_id", LiteralBase.LONG, trash.getRoot().getId()),
                new StorageField("parent_id", LiteralBase.LONG, trash.getParent().getId()),
                new StorageField("original_id", LiteralBase.LONG, trash.getOriginalId()),
                new StorageField("data", LiteralBase.STRING, trash.toJSON().toString())
        };

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeInsert(table, fields);
            }
        });
    }

    /**
     * 将废弃文件写入回收站。
     *
     * @param trash
     */
    public void writeFileTrash(FileTrash trash) {
        String table = this.recyclebinTableNameMap.get(trash.getDomainName());
        if (null == table) {
            return;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, trash.getId()),
                new StorageField("timestamp", LiteralBase.LONG, trash.getTimestamp()),
                new StorageField("root_id", LiteralBase.LONG, trash.getRoot().getId()),
                new StorageField("parent_id", LiteralBase.LONG, trash.getParent().getId()),
                new StorageField("original_id", LiteralBase.LONG, trash.getOriginalId()),
                new StorageField("file_code", LiteralBase.STRING, trash.getFileCode()),
                new StorageField("data", LiteralBase.STRING, trash.toJSON().toString())
        };

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeInsert(table, fields);
            }
        });
    }

    /**
     * 读取在回收站的废弃目录的数据。
     *
     * @param domain
     * @param rootId
     * @param id
     * @return
     */
    public JSONObject readTrash(String domain, Long rootId, Long id) {
        String table = this.recyclebinTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("data", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, id)),
                Conditional.createAnd(),
                Conditional.createEqualTo(new StorageField("root_id", LiteralBase.LONG, rootId))
        });

        if (result.isEmpty()) {
            return null;
        }

        return new JSONObject(result.get(0)[0].getString());
    }

    /**
     * 删除废弃的数据。
     *
     * @param domain
     * @param rootId
     * @param id
     */
    public void deleteTrash(String domain, Long rootId, Long id) {
        String table = this.recyclebinTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, id)),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField("root_id", LiteralBase.LONG, rootId))
                });
            }
        });
    }

    /**
     * 清空垃圾文件。
     *
     * @param domain
     * @param rootId
     */
    public void emptyTrash(String domain, Long rootId) {
        String table = this.recyclebinTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo("root_id", LiteralBase.LONG, rootId)
                });
            }
        });
    }

    /**
     * 查询指定分享码对应的域。
     *
     * @param code
     * @return
     */
    public String querySharingCodeDomain(String code) {
        List<StorageField[]> result = this.storage.executeQuery(this.sharingCodeTable, new StorageField[] {
                        new StorageField("domain", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("code", code)
                });

        if (result.isEmpty()) {
            return null;
        }

        return result.get(0)[0].getString();
    }

    /**
     * 写入分享标签。
     *
     * @param sharingTag
     */
    public void writeSharingTag(SharingTag sharingTag) {
        String table = this.sharingTagTableNameMap.get(sharingTag.getDomain().getName());
        if (null == table) {
            return;
        }

        this.executor.execute(() -> {
            storage.executeInsert(table, new StorageField[] {
                    new StorageField("id", sharingTag.getId()),
                    new StorageField("timestamp", sharingTag.getTimestamp()),
                    new StorageField("code", sharingTag.getCode()),
                    new StorageField("contact_id", sharingTag.getConfig().getContact().getId()),
                    new StorageField("contact", sharingTag.getConfig().getContact().toJSON().toString()),
                    new StorageField("device", sharingTag.getConfig().getDevice().toJSON().toString()),
                    new StorageField("file_code", sharingTag.getConfig().getFileLabel().getFileCode()),
                    new StorageField("duration", sharingTag.getConfig().getDuration()),
                    new StorageField("expiry", sharingTag.getExpiryDate()),
                    new StorageField("password", sharingTag.getConfig().getPassword()),
                    new StorageField("preview", sharingTag.getConfig().isPreview() ? 1 : 0),
                    new StorageField("download", sharingTag.getConfig().isDownloadAllowed() ? 1 : 0),
                    new StorageField("trace_download", sharingTag.getConfig().isTraceDownload() ? 1 : 0),
                    new StorageField("state", sharingTag.getState())
            });

            storage.executeInsert(sharingCodeTable, new StorageField[] {
                    new StorageField("code", sharingTag.getCode()),
                    new StorageField("domain", sharingTag.getDomain().getName()),
                    new StorageField("timestamp", sharingTag.getTimestamp())
            });

            // 写入预览文件
            writeSharingPreview(sharingTag.getDomain().getName(), sharingTag.getId(), sharingTag.getCode(),
                    sharingTag.getPreviewList());
        });
    }

    /**
     * 读取指定分享码所属的联系人 ID 。
     *
     * @param domain
     * @param sharingCode
     * @return
     */
    public long readSharingContactId(String domain, String sharingCode) {
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return 0;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("contact_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("code", sharingCode)
        });

        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getLong();
    }

    /**
     * 读取指定的分享标签。
     *
     * @param domain
     * @param code
     * @return
     */
    public SharingTag readSharingTag(String domain, String code) {
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.sharingTagFields, new Conditional[] {
                Conditional.createEqualTo("code", code)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        SharingTag sharingTag = this.makeSharingTag(domain, map);
        return sharingTag;
    }

    /**
     * 取消指定的分享标签。
     *
     * @param domain
     * @param code
     * @return 返回预览图文件标签。
     */
    public List<FileLabel> cancelSharingTag(String domain, String code) {
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        this.storage.executeUpdate(table, new StorageField[] {
                new StorageField("state", SharingTag.STATE_CANCEL)
        }, new Conditional[] {
                Conditional.createEqualTo("code", code)
        });

        return this.deleteSharingPreview(domain, code);
    }

    /**
     * 删除指定的分享标签。
     *
     * @param domain
     * @param code
     * @return 返回预览图文件标签。
     */
    public List<FileLabel> deleteSharingTag(String domain, String code) {
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        this.storage.executeUpdate(table, new StorageField[] {
                new StorageField("state", SharingTag.STATE_DELETE)
        }, new Conditional[] {
                Conditional.createEqualTo("code", code)
        });

        return this.deleteSharingPreview(domain, code);
    }

    /**
     * 计算分享标签数量。
     * @param domain
     * @param contactId
     * @param valid
     * @return
     */
    public int countSharingTag(String domain, long contactId, boolean valid) {
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return 0;
        }

        String sql = null;
        if (valid) {
            sql = "SELECT COUNT(id) FROM `" + table
                    + "` WHERE `contact_id`=" + contactId + " AND `state`=0"
                    + " AND (`expiry`=0 OR `expiry`>" + System.currentTimeMillis() + ")";
        }
        else {
            sql = "SELECT COUNT(id) FROM `" + table
                    + "` WHERE `contact_id`=" + contactId + " AND `state`=0"
                    + " AND (`expiry`<>0 AND `expiry`<" + System.currentTimeMillis() + ")";
        }

        List<StorageField[]> result = this.storage.executeQuery(sql);
        return result.get(0)[0].getInt();
    }

    /**
     * 计算分享标签的文件类型数量。
     * @param domain
     * @param contactId
     * @param valid
     * @return
     */
    public Map<String, AtomicInteger> countSharingFileType(String domain, long contactId, boolean valid) {
        Map<String, AtomicInteger> data = new HashMap<>();

        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return data;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT(file_code) FROM `").append(table).append("`");
        sql.append(" WHERE `contact_id`=").append(contactId);
        sql.append(" AND `state`=0");
        if (valid) {
            sql.append(" AND (`expiry`=0 OR `expiry`>").append(System.currentTimeMillis()).append(")");
        }
        else {
            sql.append(" AND (`expiry`<>0 AND `expiry`<").append(System.currentTimeMillis()).append(")");
        }
        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return data;
        }

        // 从 File Label 查询文件类型
        table = this.labelTableNameMap.get(domain);
        for (StorageField[] fields : result) {
            // 文件码
            String fileCode = fields[0].getString();

            sql = new StringBuilder();
            sql.append("SELECT `file_type` FROM `").append(table).append("`");
            sql.append(" WHERE `file_code`='").append(fileCode).append("'");
            List<StorageField[]> fileTypeResult = this.storage.executeQuery(sql.toString());
            if (!fileTypeResult.isEmpty()) {
                String fileType = fileTypeResult.get(0)[0].getString();

                AtomicInteger count = data.get(fileType);
                if (null == count) {
                    count = new AtomicInteger(0);
                    data.put(fileType, count);
                }
                count.incrementAndGet();
            }
        }

        return data;
    }

    /**
     * 按照时间戳检索分享标签。
     *
     * @param domain
     * @param contactId
     * @param valid
     * @param beginTime
     * @param endTime
     * @param descending
     * @return
     */
    public List<SharingTag> searchSharingTags(String domain, long contactId, boolean valid, long beginTime, long endTime,
                                              boolean descending) {
        List<SharingTag> list = new ArrayList<>();
        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        Conditional[] conditionals = null;
        if (valid) {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", 0),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("expiry", (long) 0),
                            Conditional.createOr(),
                            Conditional.createGreaterThanEqual(new StorageField("expiry", System.currentTimeMillis()))
                    }),
                    Conditional.createAnd(),
                    Conditional.createGreaterThanEqual(new StorageField("timestamp", beginTime)),
                    Conditional.createAnd(),
                    Conditional.createLessThan(new StorageField("timestamp", endTime)),
                    Conditional.createOrderBy("timestamp", descending)
            };
        }
        else {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", 0),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createUnequalTo("expiry", (long) 0),
                            Conditional.createAnd(),
                            Conditional.createLessThan(new StorageField("expiry", System.currentTimeMillis()))
                    }),
                    Conditional.createAnd(),
                    Conditional.createGreaterThanEqual(new StorageField("timestamp", beginTime)),
                    Conditional.createAnd(),
                    Conditional.createLessThan(new StorageField("timestamp", endTime)),
                    Conditional.createOrderBy("timestamp", descending)
            };
        }

        // 查库
        List<StorageField[]> result = this.storage.executeQuery(table, this.sharingTagFields, conditionals);
        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            SharingTag sharingTag = this.makeSharingTag(domain, map);
            if (null == sharingTag) {
                continue;
            }

            list.add(sharingTag);
        }

        return list;
    }

    /**
     * 获取指定联系人的分享标签。
     *
     * @param domain
     * @param contactId
     * @param valid
     * @param beginIndex
     * @param endIndex
     * @param descending 是否降序。
     * @return
     */
    public List<SharingTag> listSharingTags(String domain, long contactId, boolean valid, int beginIndex, int endIndex,
                                            boolean descending) {
        List<SharingTag> list = new ArrayList<>();

        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        Conditional[] conditionals = null;
        if (valid) {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", 0),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("expiry", (long) 0),
                            Conditional.createOr(),
                            Conditional.createGreaterThanEqual(new StorageField("expiry", System.currentTimeMillis()))
                    }),
                    Conditional.createOrderBy("timestamp", descending),
                    Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
            };
        }
        else {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", 0),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createUnequalTo("expiry", (long) 0),
                            Conditional.createAnd(),
                            Conditional.createLessThan(new StorageField("expiry", System.currentTimeMillis()))
                    }),
                    Conditional.createOrderBy("timestamp", descending),
                    Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
            };
        }

        // 查库
        List<StorageField[]> result = this.storage.executeQuery(table, this.sharingTagFields, conditionals);
        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            SharingTag sharingTag = this.makeSharingTag(domain, map);
            if (null == sharingTag) {
                continue;
            }

            list.add(sharingTag);
        }

        return list;
    }

    private void writeSharingPreview(String domain, long tagId, String tagCode, List<FileLabel> previewList) {
        if (null == previewList) {
            return;
        }

        String table = this.sharingTagPreviewTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo("tag_id", tagId),
                Conditional.createOr(),
                Conditional.createEqualTo("tag_code", tagCode)
        });

        for (FileLabel fileLabel : previewList) {
            this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("tag_id", tagId),
                    new StorageField("tag_code", tagCode),
                    new StorageField("file_code", fileLabel.getFileCode()),
                    new StorageField("file_label", fileLabel.toJSON().toString())
            });
        }
    }

    private List<FileLabel> readSharingPreview(String domain, String code) {
        String table = this.sharingTagPreviewTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<FileLabel> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("file_label", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("tag_code", code)
        });

        for (StorageField[] fields : result) {
            JSONObject json = new JSONObject(fields[0].getString());
            list.add(new FileLabel(json));
        }

        return list;
    }

    /**
     * 删除预览图文件。
     *
     * @param domain
     * @param code
     * @return
     */
    private List<FileLabel> deleteSharingPreview(String domain, String code) {
        String table = this.sharingTagPreviewTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<FileLabel> list = readSharingPreview(domain, code);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo("tag_code", code)
                });
            }
        });

        return list;
    }

    public void writeVisitTrace(String domain, String code, VisitTrace visitTrace) {
        this.executor.execute(() -> {
            String table = this.visitTraceTableNameMap.get(domain);
            if (null == table) {
                return;
            }

            this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("code", code),
                    new StorageField("platform", visitTrace.platform),
                    new StorageField("time", visitTrace.time),
                    new StorageField("address", visitTrace.address),
                    new StorageField("domain", visitTrace.domain),
                    new StorageField("url", visitTrace.url),
                    new StorageField("title", visitTrace.title),
                    new StorageField("screen", visitTrace.getScreenJSON().toString()),
                    new StorageField("language", visitTrace.language),
                    new StorageField("user_agent", visitTrace.userAgent),
                    new StorageField("agent", (null != visitTrace.agent) ? visitTrace.agent.toString() : null),
                    new StorageField("event", visitTrace.event),
                    new StorageField("event_tag", visitTrace.eventTag),
                    new StorageField("event_param", (null != visitTrace.eventParam) ?
                            visitTrace.eventParam.toString() : null),
                    new StorageField("contact_id", visitTrace.contactId),
                    new StorageField("contact_domain", visitTrace.contactDomain),
                    new StorageField("sharer", visitTrace.getSharerId()),
                    new StorageField("parent", visitTrace.getParentId())
            });
        });
    }

    private SharingTag makeSharingTag(String domain, Map<String, StorageField> map) {
        // 读取文件标签
        FileLabel fileLabel = this.readFileLabel(domain, map.get("file_code").getString());
        if (null == fileLabel) {
            return null;
        }

        Contact contact = new Contact(new JSONObject(map.get("contact").getString()));
        Device device = new Device(new JSONObject(map.get("device").getString()));

        SharingTag sharingTag = new SharingTag(map.get("id").getLong(), domain, map.get("timestamp").getLong(),
                map.get("code").getString(), map.get("expiry").getLong(),
                contact, device, fileLabel, map.get("duration").getLong(),
                map.get("password").isNullValue() ? null : map.get("password").getString(),
                map.get("preview").getInt() == 1, map.get("download").getInt() == 1,
                map.get("trace_download").getInt() == 1,
                map.get("state").getInt());

        // 预览文件列表
        List<FileLabel> previewList = readSharingPreview(domain, sharingTag.getCode());
        sharingTag.setPreviewList(previewList);

        return sharingTag;
    }

    private VisitTrace makeVisitTrace(Map<String, StorageField> map) {
        VisitTrace visitTrace = new VisitTrace(map.get("platform").getString(), map.get("time").getLong(),
                map.get("address").getString(), map.get("domain").getString(), map.get("url").getString(),
                map.get("title").getString(),
                new JSONObject(map.get("screen").getString()), map.get("language").getString(),
                map.get("user_agent").isNullValue() ? null : map.get("user_agent").getString(),
                map.get("agent").isNullValue() ? null : new JSONObject(map.get("agent").getString()),
                map.get("event").isNullValue() ? null : map.get("event").getString(),
                map.get("event_tag").isNullValue() ? null : map.get("event_tag").getString(),
                map.get("event_param").isNullValue() ? null : new JSONObject(map.get("event_param").getString()));
        visitTrace.contactId = map.get("contact_id").getLong();
        visitTrace.contactDomain = map.get("contact_domain").isNullValue() ? null : map.get("contact_domain").getString();
        visitTrace.sharerId = map.get("sharer").getLong();
        visitTrace.parentId = map.get("parent").getLong();
        visitTrace.code = map.get("code").getString();
        return visitTrace;
    }

    /**
     * 批量获取指定分享人的分享访问记录。
     *
     * @param domain
     * @param contactId
     * @param code
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<VisitTrace> listVisitTraces(String domain, long contactId, String code, int beginIndex, int endIndex) {
        List<VisitTrace> list = new ArrayList<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.visitTraceFields, new Conditional[] {
                Conditional.createEqualTo("code", code),
                Conditional.createAnd(),
                Conditional.createEqualTo("parent", contactId),
                Conditional.createOrderBy("time", true),
                Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            VisitTrace visitTrace = this.makeVisitTrace(map);
            list.add(visitTrace);
        }

        return list;
    }

    /**
     * 按照时间检索指定分享人的分享访问记录。
     *
     * @param domain
     * @param contactId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<VisitTrace> searchVisitTraces(String domain, long contactId, long beginTime, long endTime) {
        List<VisitTrace> list = new ArrayList<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.visitTraceFields, new Conditional[] {
                Conditional.createBracket(new Conditional[] {
                        Conditional.createGreaterThanEqual(new StorageField("time", beginTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThan(new StorageField("time", endTime))
                }),
                Conditional.createAnd(),
                Conditional.createEqualTo("parent", contactId)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            VisitTrace visitTrace = this.makeVisitTrace(map);
            list.add(visitTrace);
        }

        return list;
    }

    /**
     * 按照指定的 Sharer 查询记录。
     * @param domain
     * @param code
     * @param sharerId
     * @return
     */
    public List<VisitTrace> queryVisitTraceBySharer(String domain, String code, long sharerId) {
        List<VisitTrace> list = new ArrayList<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.visitTraceFields, new Conditional[] {
                Conditional.createEqualTo("code", code),
                Conditional.createAnd(),
                Conditional.createEqualTo("sharer", sharerId),
                Conditional.createOrderBy("time", true)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            VisitTrace visitTrace = this.makeVisitTrace(map);
            list.add(visitTrace);
        }

        return list;
    }

    /**
     * 查询下一级访问痕迹。
     *
     * @param domain
     * @param code
     * @param parentId
     * @return
     */
    public List<VisitTrace> queryVisitTraceByParent(String domain, String code, long parentId) {
        List<VisitTrace> list = new ArrayList<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.visitTraceFields, new Conditional[] {
                Conditional.createEqualTo("code", code),
                Conditional.createAnd(),
                Conditional.createEqualTo("parent", parentId),
                Conditional.createAnd(),
                Conditional.createUnequalTo("sharer", parentId),
                Conditional.createOrderBy("time", true)
        });

        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            VisitTrace visitTrace = this.makeVisitTrace(map);
            list.add(visitTrace);
        }

        return list;
    }

    /**
     * 计算记录数量。
     *
     * @param domain
     * @param code
     * @return
     */
    public int countVisitTraces(String domain, String code) {
        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return 0;
        }

        String sql = "SELECT COUNT(sn) FROM `" + table + "` WHERE `code`='" + code + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return result.get(0)[0].getInt();
    }

    /**
     * 计算联系人分享出去的所有链接指定事件的数量。
     * @param domain
     * @param contactId
     * @param event
     * @return
     */
    public int countTraceEvent(String domain, long contactId, String event) {
        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return 0;
        }

        String sql = "SELECT COUNT(sn) FROM `" + table + "` WHERE `event`='" +
                    event + "' AND `parent`=" + contactId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return result.get(0)[0].getInt();
    }

    /**
     * 查询指定事件对应的分享码数量。
     * @param domain
     * @param countId
     * @param event
     * @return
     */
    public Map<String, Integer> queryCodeCountByEvent(String domain, long countId, String event) {
        Map<String, Integer> map = new HashMap<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return map;
        }

        String sql = "SELECT DISTINCT(code) FROM `" + table + "` WHERE `event`='" + event + "' AND `parent`="
                + countId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (!result.isEmpty()) {
            for (StorageField[] fields : result) {
                String code = fields[0].getString();
                // 查询指定 code 对应事件的数量
                sql = "SELECT COUNT(sn) FROM `" + table + "` WHERE `event`='" + event + "' AND `parent`="
                        + countId + " AND `code`='" + code + "'";
                List<StorageField[]> countResult = this.storage.executeQuery(sql);
                int count = countResult.get(0)[0].getInt();
                map.put(code, count);
            }
        }
        return map;
    }

    /**
     * 查询事件时间线数据。
     * @param domain
     * @param countId
     * @param event
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<TimePoint> queryEventTimeline(String domain, long countId, String event,
                                                 long beginTime, long endTime) {
        List<TimePoint> list = new ArrayList<>();
        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT `code`,`time`,`address`,`user_agent`,`agent` FROM `").append(table).append("`");
        sql.append(" WHERE `event`='").append(event).append("'");
        sql.append(" AND `parent`=").append(countId);
        sql.append(" AND `time`>").append(beginTime);
        sql.append(" AND `time`<=").append(endTime);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] fields : result) {
            String code = fields[0].getString();
            long time = fields[1].getLong();

            TimePoint point = new TimePoint(time, code);
            point.event = event;
            point.address = fields[2].getString();
            point.userAgent = fields[3].isNullValue() ? null : fields[3].getString();
            point.agent = fields[4].isNullValue() ? null : fields[4].getString();
            list.add(point);
        }

        return list;
    }

    protected class TimePoint {

        public long time;

        public String code;

        public String event;

        public String address;

        public String userAgent;

        public String agent;

        public TimePoint(long time, String code) {
            this.time = time;
            this.code = code;
        }
    }

    private void checkLabelTable(String domain) {
        String table = this.labelTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.labelTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[]{
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("owner_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_size", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("last_modified", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("completed_time", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("expiry_time", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_type", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("md5", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("sha1", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("file_url", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_secure_url", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("direct_url", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkDescriptorTable(String domain) {
        String table = this.descriptorTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.descriptorTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("system", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("url", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("descriptor", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkPerformanceTable(String domain) {
        String table = this.performanceTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.performanceTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.performanceFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkHierarchyTable(String domain) {
        String table = this.hierarchyTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.hierarchyTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.hierarchyFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkRecyclebinTable(String domain) {
        String table = this.recyclebinTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.recyclebinTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[]{
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY
                    }),
                    new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("root_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("parent_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("original_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("data", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkSharingTagTable(String domain) {
        String table = this.sharingTagTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.sharingTagTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.sharingTagFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkSharingTagPreviewTable(String domain) {
        String table = this.sharingTagPreviewTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.sharingTagPreviewTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.sharingTagPreviewFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkVisitTraceTable(String domain) {
        String table = this.visitTraceTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.visitTraceTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.visitTraceFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkSharingCodeTable() {
        if (!this.storage.exist(this.sharingCodeTable)) {
            // 表不存在，建表
            if (this.storage.executeCreate(this.sharingCodeTable, this.sharingCodeFields)) {
                Logger.i(this.getClass(), "Created table '" + this.sharingCodeTable + "' successfully");
            }
        }
    }
}
