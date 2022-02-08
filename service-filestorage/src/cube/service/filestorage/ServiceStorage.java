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
import cube.common.entity.FileLabel;
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
import org.json.JSONException;
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

    private final String version = "1.0";

    private final String labelTablePrefix = "label_";

    private final String descriptorTablePrefix = "descriptor_";

    private final String hierarchyTablePrefix = "hierarchy_";

    private final String recyclebinTablePrefix = "recyclebin_";

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
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 文件描述符字段。
     */
    private final StorageField[] descriptorFields = new StorageField[] {
            new StorageField("file_code", LiteralBase.STRING),
            new StorageField("system", LiteralBase.STRING),
            new StorageField("file_name", LiteralBase.STRING),
            new StorageField("url", LiteralBase.STRING),
            new StorageField("descriptor", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
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

    public ServiceStorage(ExecutorService executorService, StorageType type, JSONObject config) {
        this.executor = executorService;
        this.storage = StorageFactory.getInstance().createStorage(type, "FileStructStorage", config);
        this.labelTableNameMap = new HashMap<>();
        this.descriptorTableNameMap = new HashMap<>();
        this.hierarchyTableNameMap = new HashMap<>();
        this.recyclebinTableNameMap = new HashMap<>();
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
        }
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
                        new StorageField("descriptor", LiteralBase.STRING, fileDescriptor.getDescriptor().toString())
                };

                // 判断是否已经写入数据
                List<StorageField[]> result = storage.executeQuery(labelTable, new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()))
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
                            Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()))
                    });

                    // 描述符表
                    storage.executeUpdate(descriptorTable, descriptorFields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileLabel.getFileCode()))
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
                Conditional.createEqualTo(new StorageField("file_code", LiteralBase.STRING, fileCode))
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
     * 查找指定文件名、修改日期和文件大小的文件。
     *
     * @param domain
     * @param fileName
     * @param lastModified
     * @param fileSize
     * @return 如果找到该文件返回文件码，否则返回 {@code null} 值。
     */
    public String findFile(String domain, String fileName, long lastModified, long fileSize) {
        String labelTable = this.labelTableNameMap.get(domain);
        if (null == labelTable) {
            return null;
        }

        String fileCode = null;
        List<StorageField[]> result = this.storage.executeQuery(labelTable, new StorageField[] {
                new StorageField("file_code", LiteralBase.STRING)
        }, new Conditional[] {
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
                            Constraint.UNIQUE
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
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
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
}
