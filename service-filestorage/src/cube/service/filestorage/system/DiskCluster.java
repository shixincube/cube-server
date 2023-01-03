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

package cube.service.filestorage.system;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 磁盘文件集群。
 */
public class DiskCluster implements Storagable  {

    private final String clusterFileTable = "disk_cluster_file";

    private final StorageField[] clusterFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("host", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("port", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private Storage storage;

    private String localHost;

    private int localPort;

    private String contextPath = "/files/";

    private String masterHost;

    private int masterPort;

    private String masterContextPath = "/transfer/";

    public DiskCluster(String localHost, int localPort, StorageType type, JSONObject config) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.storage = StorageFactory.getInstance().createStorage(type, "FileDiskCluster", config);
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
        if (!this.storage.exist(this.clusterFileTable)) {
            // 表不存在，建表
            if (this.storage.executeCreate(this.clusterFileTable, this.clusterFields)) {
                Logger.i(this.getClass(), "Created table '" + this.clusterFileTable + "' successfully");
            }
        }
    }

    public void setContextPath(String value) {
        this.contextPath = value;
    }

    public void setMaster(String host, int port) {
        this.masterHost = host;
        this.masterPort = port;
    }

    public boolean useMaster() {
        return (null != this.masterHost && 0 != this.masterPort);
    }

    /**
     * 添加文件信息。
     *
     * @param fileCode
     */
    public void addFile(String fileCode) {
        this.updateCluster(fileCode, this.localHost, this.localPort);
    }

    /**
     * 添加文件信息。
     *
     * @param fileCode
     */
    private void updateCluster(String fileCode, String host, int port) {
        String table = this.clusterFileTable;

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });

        if (result.isEmpty()) {
            // 插入新数据
            this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("file_code", fileCode),
                    new StorageField("host", host),
                    new StorageField("port", port),
                    new StorageField("timestamp", System.currentTimeMillis())
            });
        }
        else {
            // 更新数据
            this.storage.executeUpdate(table, new StorageField[] {
                    new StorageField("host", host),
                    new StorageField("port", port),
                    new StorageField("timestamp", System.currentTimeMillis())
            }, new Conditional[] {
                    Conditional.createEqualTo("file_code", fileCode)
            });
        }
    }

    /**
     * 移除文件记录。
     *
     * @param fileCode
     */
    public void removeFile(String fileCode) {
        String table = this.clusterFileTable;

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });
    }

    /**
     * 加载指定文件码的文件数据到输出流。
     *
     * @param fileCode
     * @param outputStream
     * @throws IOException
     */
    public void loadFile(String fileCode, OutputStream outputStream) throws IOException {
        List<StorageField[]> result = this.storage.executeQuery(this.clusterFileTable, this.clusterFields,
            new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });

        if (result.isEmpty()) {
            throw new IOException("Can NOT find file");
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(map.get("host").getString());
        urlString.append(":");
        urlString.append(map.get("port").getInt());
        urlString.append(this.contextPath);
        urlString.append(fileCode);

        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                byte[] bytes = new byte[10240];
                int length = 0;
                while ((length = is.read(bytes)) > 0) {
                    outputStream.write(bytes, 0, length);
                }
                outputStream.flush();
            }
            else {
                throw new IOException("Read http stream error");
            }
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
    }

    public long saveFile(String fileCode, InputStream inputStream) {
        long size = 0;

        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(this.masterHost);
        urlString.append(":");
        urlString.append(this.masterPort);
        urlString.append(this.masterContextPath);
        urlString.append("?fc=");
        urlString.append(fileCode);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#saveFile: URL - " + urlString.toString());
        }

        HttpURLConnection connection = null;

        URL url = null;
        try {
            url = new URL(urlString.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);

            connection.connect();

            OutputStream os = connection.getOutputStream();
            byte[] buf = new byte[10240];
            int length = 0;
            while ((length = inputStream.read(buf)) > 0) {
                os.write(buf, 0, length);
                // 更新大小
                size += length;
            }
            os.flush();
            // 关闭流
            os.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                // 结果
                length = connection.getInputStream().read(buf);
                if (length > 0) {
                    JSONObject json = new JSONObject(new String(buf, 0, length));
                    long responseSize = json.getLong("size");
                    if (responseSize != size) {
                        Logger.w(this.getClass(), "#saveFile - data size error: " + size + " - " + responseSize);
                    }
                }
            }
            else {
                Logger.w(this.getClass(), "#saveFile - status code: " + code);
                size = 0;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }

        if (size > 0) {
            this.updateCluster(fileCode, this.masterHost, this.masterPort);
        }

        return size;
    }
}
