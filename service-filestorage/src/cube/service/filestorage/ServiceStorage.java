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

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 文件码存储器。
 * 用于管理文件码存储。
 */
public class ServiceStorage implements Storagable {

    private final String labelTablePrefix = "label_";

    private final String descriptorTablePrefix = "descriptor_";

    private final String hierarchyTablePrefix = "hierarchy_";

    private final String recyclebinTablePrefix = "recyclebin_";

    private final String sharingTagTablePrefix = "sharing_tag_";

    private final String visitTraceTablePrefix = "visit_trace_";

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
     * 层级表字段。
     */
    private final StorageField[] hierarchyFields = new StorageField[] {
            new StorageField("node_id", LiteralBase.LONG),
            new StorageField("data", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
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
            //new StorageField("reserved", LiteralBase.STRING)
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
            })
    };

    private final StorageField[] visitTraceFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("ip", LiteralBase.STRING, new Constraint[] {
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
            new StorageField("referrer", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("language", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("user_agent", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("event", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("event_tag", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("event_param", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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
     * 访问追踪数据表。
     */
    private Map<String, String> visitTraceTableNameMap;

    public ServiceStorage(ExecutorService executorService, StorageType type, JSONObject config) {
        this.executor = executorService;
        this.storage = StorageFactory.getInstance().createStorage(type, "FileStructStorage", config);
        this.labelTableNameMap = new HashMap<>();
        this.descriptorTableNameMap = new HashMap<>();
        this.hierarchyTableNameMap = new HashMap<>();
        this.recyclebinTableNameMap = new HashMap<>();
        this.sharingTagTableNameMap = new HashMap<>();
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

            // 检查层级表
            this.checkHierarchyTable(domain);

            // 检查回收站表
            this.checkRecyclebinTable(domain);

            this.checkSharingTagTable(domain);
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
    public boolean deleteFile(String domain, String fileCode) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return false;
        }

        boolean result = this.storage.executeDelete(labelTable, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });

        String descriptorTable = this.descriptorTableNameMap.get(domain);
        if (null != descriptorTable) {
            this.storage.executeDelete(descriptorTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }

        String recyclebinTable = this.recyclebinTableNameMap.get(domain);
        this.storage.executeDelete(recyclebinTable, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });

        return result;
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
                Conditional.createLessThan(new StorageField("expiry_time", LiteralBase.LONG, expiryTime))
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
     * 写入节点数据。
     *
     * @param domain
     * @param nodeId
     * @param json
     */
    public void writeHierarchyNode(String domain, Long nodeId, JSONObject json) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("node_id", LiteralBase.LONG, nodeId),
                new StorageField("data", LiteralBase.STRING, json.toString())
        };

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", LiteralBase.LONG, nodeId))
        });

        if (result.isEmpty()) {
            this.storage.executeInsert(table, fields);
        }
        else {
            this.storage.executeUpdate(table, fields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("node_id", LiteralBase.LONG, nodeId))
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
    public JSONObject readHierarchyNode(String domain, Long nodeId) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.hierarchyFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", LiteralBase.LONG, nodeId))
        });

        if (!result.isEmpty()) {
            JSONObject data = new JSONObject(result.get(0)[1].getString());
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
    public void deleteHierarchyNode(String domain, Long nodeId) {
        String table = this.hierarchyTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo(new StorageField("node_id", LiteralBase.LONG, nodeId))
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
                    new StorageField("password", sharingTag.getConfig().getPassword())
            });

            storage.executeInsert(sharingCodeTable, new StorageField[] {
                    new StorageField("code", sharingTag.getCode()),
                    new StorageField("domain", sharingTag.getDomain().getName()),
                    new StorageField("timestamp", sharingTag.getTimestamp())
            });
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
                map.get("password").isNullValue() ? null : map.get("password").getString());

        return sharingTag;
    }

    /**
     * 获取指定联系人的分享标签。
     *
     * @param domain
     * @param contactId
     * @param inExpiry
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<SharingTag> listSharingTags(String domain, long contactId, boolean inExpiry, int beginIndex, int endIndex) {
        List<SharingTag> list = new ArrayList<>();

        String table = this.sharingTagTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        Conditional[] conditionals = null;
        if (inExpiry) {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("expiry", (long) 0),
                            Conditional.createOr(),
                            Conditional.createGreaterThanEqual(new StorageField("expiry", System.currentTimeMillis()))
                    }),
                    Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
            };
        }
        else {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createLessThan(new StorageField("expiry", System.currentTimeMillis())),
                    Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
            };
        }

        // 查库
        List<StorageField[]> result = this.storage.executeQuery(table, this.sharingTagFields, conditionals);
        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            // 读取文件标签
            FileLabel fileLabel = this.readFileLabel(domain, map.get("file_code").getString());
            if (null == fileLabel) {
                continue;
            }

            Contact contact = new Contact(new JSONObject(map.get("contact").getString()));
            Device device = new Device(new JSONObject(map.get("device").getString()));

            SharingTag sharingTag = new SharingTag(map.get("id").getLong(), domain, map.get("timestamp").getLong(),
                    map.get("code").getString(), map.get("expiry").getLong(),
                    contact, device, fileLabel, map.get("duration").getLong(),
                    map.get("password").isNullValue() ? null : map.get("password").getString());

            list.add(sharingTag);
        }

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
                    new StorageField("time", visitTrace.time),
                    new StorageField("ip", visitTrace.ip),
                    new StorageField("domain", visitTrace.domain),
                    new StorageField("url", visitTrace.url),
                    new StorageField("title", visitTrace.title),
                    new StorageField("screen", visitTrace.getScreenJSON().toString()),
                    new StorageField("referrer", visitTrace.referrer),
                    new StorageField("language", visitTrace.language),
                    new StorageField("user_agent", visitTrace.userAgent),
                    new StorageField("event", visitTrace.event),
                    new StorageField("event_tag", visitTrace.eventTag),
                    new StorageField("event_param", (null != visitTrace.eventParam) ?
                            visitTrace.eventParam.toString() : null)
            });
        });
    }

    public List<VisitTrace> listVisitTraces(String domain, String code, int beginIndex, int endIndex) {
        List<VisitTrace> list = new ArrayList<>();

        String table = this.visitTraceTableNameMap.get(domain);
        if (null == table) {
            return list;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.visitTraceFields, new Conditional[] {
                Conditional.createEqualTo("code", code),
                Conditional.createLimit(beginIndex, endIndex - beginIndex + 1)
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            VisitTrace visitTrace = new VisitTrace(map.get("time").getLong(), map.get("ip").getString(),
                    map.get("domain").getString(), map.get("url").getString(), map.get("title").getString(),
                    new JSONObject(map.get("screen").getString()), map.get("referrer").getString(),
                    map.get("language").getString(), map.get("user_agent").getString(),
                    map.get("event").isNullValue() ? null : map.get("event").getString(),
                    map.get("event_tag").isNullValue() ? null : map.get("event_tag").getString(),
                    map.get("event_param").isNullValue() ? null : new JSONObject(map.get("event_param").getString()));
            list.add(visitTrace);
        }

        return list;
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

    private void checkHierarchyTable(String domain) {
        String table = this.hierarchyTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.hierarchyTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("node_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE
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
