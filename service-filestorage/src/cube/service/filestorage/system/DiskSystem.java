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

package cube.service.filestorage.system;

import cell.core.net.Endpoint;
import cell.util.log.Logger;
import cube.storage.StorageType;
import cube.util.CrossDomainHandler;
import cube.util.HttpServer;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 磁盘文件系统。
 */
public class DiskSystem implements FileSystem {

    private Path managingPath;

    private String contextPath = "/files/";

    private Endpoint endpoint;

    private String url;

    private HttpServer httpServer;

    private List<String> writingFiles;

    private Endpoint masterEndpoint;

    private DiskCluster diskCluster;

    public DiskSystem(String managingPath, String host, int port) {
        this(managingPath, host, port, null, 0);
    }

    public DiskSystem(String managingPath, String host, int port, String masterHost, int masterPort) {
        this.managingPath = Paths.get(managingPath);
        if (!Files.exists(this.managingPath)) {
            try {
                Files.createDirectories(this.managingPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.endpoint = new Endpoint(host, port);

        if (null != masterHost && 0 != masterPort) {
            this.masterEndpoint = new Endpoint(masterHost, masterPort);

            if (Logger.isDebugLevel()) {
                Logger.i(DiskSystem.class, "Disk cluster use master node: " + this.masterEndpoint.toString());
            }
        }

        this.url = "http://" + host + ":" + port + this.contextPath;

        this.writingFiles = new ArrayList<>();
    }

    @Override
    public void start() {
        this.httpServer = new HttpServer();

        Resource resource = new PathResource(this.managingPath);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setAcceptRanges(true);

        ContextHandler context = new ContextHandler();
        context.setContextPath(this.contextPath);
        context.setBaseResource(resource);
        context.setHandler(resourceHandler);

        this.httpServer.addContextHandler(context);

        this.httpServer.addContextHandler(new TransferHandler());

        this.httpServer.start(this.endpoint.getPort());
    }

    @Override
    public void stop() {
        this.httpServer.stop();

        if (null != this.diskCluster) {
            this.diskCluster.close();
            this.diskCluster = null;
        }
    }

    /**
     * 激活集群。
     *
     * @param type
     * @param config
     */
    public void activateCluster(StorageType type, JSONObject config) {
        if (null != this.diskCluster) {
            this.diskCluster.close();
        }

        this.diskCluster = new DiskCluster(this.endpoint.getHost(), this.endpoint.getPort(), type, config);
        this.diskCluster.setContextPath(this.contextPath);

        if (null != this.masterEndpoint) {
            this.diskCluster.setMaster(this.masterEndpoint.getHost(), this.masterEndpoint.getPort());
        }

        this.diskCluster.open();
        this.diskCluster.execSelfChecking(null);
    }

    @Override
    public FileDescriptor writeFile(String fileCode, File file) {
        synchronized (this.writingFiles) {
            if (!this.writingFiles.contains(fileCode)) {
                this.writingFiles.add(fileCode);
            }
        }

        long size = 0;
        FileDescriptor descriptor = new FileDescriptor("disk", fileCode, this.url + fileCode);

        if (null != this.diskCluster && this.diskCluster.useMaster()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                // 存储到集群
                size = this.diskCluster.saveFile(fileCode, fis);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        else {
            Path target = Paths.get(this.managingPath.toString(), fileCode);

            try {
                Files.copy(Paths.get(file.getPath()), target, StandardCopyOption.REPLACE_EXISTING);
                size = Files.size(target);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#writeFile(String,File)", e);
                synchronized (this.writingFiles) {
                    this.writingFiles.remove(fileCode);
                }
                return null;
            }

            if (null != this.diskCluster) {
                this.diskCluster.addFile(fileCode);
            }

            // 本地路径
            descriptor.attr("path", target.toString());
        }

        descriptor.attr("file", fileCode);
        descriptor.attr("size", size);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Write file : " + descriptor);
        }

        synchronized (this.writingFiles) {
            this.writingFiles.remove(fileCode);
        }

        return descriptor;
    }

    @Override
    public FileDescriptor writeFile(String fileCode, InputStream inputStream) {
        synchronized (this.writingFiles) {
            if (!this.writingFiles.contains(fileCode)) {
                this.writingFiles.add(fileCode);
            }
        }

        long size = 0;
        FileDescriptor descriptor = new FileDescriptor("disk", fileCode, this.url + fileCode);

        if (null != this.diskCluster && this.diskCluster.useMaster()) {
            // 文件存储到集群
            size = this.diskCluster.saveFile(fileCode, inputStream);
        }
        else {
            Path target = Paths.get(this.managingPath.toAbsolutePath().toString(), fileCode);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(target.toFile());

                byte[] buf = new byte[64 * 1024];
                int length = 0;
                while ((length = inputStream.read(buf)) > 0) {
                    fos.write(buf, 0, length);
                    size += length;
                }
                fos.flush();
            } catch (IOException e) {
                Logger.e(this.getClass(), "#writeFile(String,InputStream)", e);
                synchronized (this.writingFiles) {
                    this.writingFiles.remove(fileCode);
                }
                return null;
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }

            if (null != this.diskCluster) {
                this.diskCluster.addFile(fileCode);
            }

            // 本地路径
            descriptor.attr("path", target.toString());
        }

        descriptor.attr("file", fileCode);
        descriptor.attr("size", size);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Write file stream : " + descriptor);
        }

        synchronized (this.writingFiles) {
            this.writingFiles.remove(fileCode);
        }

        return descriptor;
    }

    @Override
    public boolean isWriting(String fileCode) {
        synchronized (this.writingFiles) {
            return this.writingFiles.contains(fileCode);
        }
    }

    @Override
    public boolean deleteFile(String fileCode) {
        Path file = Paths.get(this.managingPath.toString(), fileCode);
        try {
            // 从集群系统中删除
            this.diskCluster.removeFile(fileCode);

            if (Files.exists(file)) {
                Files.delete(file);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean existsFile(String fileCode) {
        Path file = Paths.get(this.managingPath.toString(), fileCode);
        return Files.exists(file);
    }

    @Override
    public String loadFileToDisk(String fileCode) {
        if (this.existsFile(fileCode)) {
            Path path = Paths.get(this.managingPath.toString(), fileCode);
            return path.toAbsolutePath().toString();
        }
        else if (null != this.diskCluster) {
            // 从集群加载数据
            File file = new File(this.managingPath.toFile(), fileCode);
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(file);
                this.diskCluster.loadFile(fileCode, fos);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#loadFileToDisk", e);
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }

            return file.exists() ? file.getAbsolutePath() : null;
        }
        else {
            return null;
        }
    }

    /**
     * 传输 Handler 。
     */
    private class TransferHandler extends ContextHandler {

        public TransferHandler() {
            super("/transfer/");
            setHandler(new Handler());
        }

        private class Handler extends CrossDomainHandler {

            private Handler() {
                super();
            }

            @Override
            public void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                String fileCode = request.getParameter("fc");
                if (null == fileCode) {
                    Logger.w(TransferHandler.class, "#doPost - Read file code parameter error");
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                if (Logger.isDebugLevel()) {
                    Logger.d(TransferHandler.class, "#doPost - " + fileCode);
                }

                InputStream inputStream = request.getInputStream();

                Path target = Paths.get(managingPath.toAbsolutePath().toString(), fileCode);

                long size = 0;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(target.toFile());

                    byte[] buf = new byte[10240];
                    int length = 0;
                    while ((length = inputStream.read(buf)) > 0) {
                        fos.write(buf, 0, length);
                        size += length;
                    }
                    fos.flush();
                } catch (IOException e) {
                    Logger.e(this.getClass(), "#doPost()", e);
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                }

                JSONObject responseData = new JSONObject();
                responseData.put("fileCode", fileCode);
                responseData.put("size", size);
                this.respondOk(response, responseData);
                this.complete();
            }
        }
    }
}
