/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 磁盘文件系统。
 */
public class DiskSystem implements FileSystem {

    private Path managingPath;

    private String contextPath = "/filestorage/";

    private Endpoint endpoint;

    private String url;

    private HttpServer httpServer;

    public DiskSystem(String managingPath, String host, int port) {
        this.managingPath = Paths.get(managingPath);
        if (!Files.exists(this.managingPath)) {
            try {
                Files.createDirectories(this.managingPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.endpoint = new Endpoint(host, port);

        this.url = "http://" + host + ":" + port + this.contextPath;
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

        this.httpServer.start(this.endpoint.getPort());
    }

    @Override
    public void stop() {
        this.httpServer.stop();
    }

    @Override
    public FileDescriptor writeFile(File file) {
        Path target = Paths.get(this.managingPath.toString(), file.getName());
        long size = 0;
        try {
            Files.copy(Paths.get(file.getPath()), target);
            size = Files.size(target);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileDescriptor descriptor = new FileDescriptor("disk", file.getName(), this.url + file.getName());
        descriptor.attr("file", file.getName());
        descriptor.attr("path", target.toAbsolutePath().toString());
        descriptor.attr("size", size);
        return descriptor;
    }

    @Override
    public FileDescriptor writeFile(String fileName, InputStream inputStream) {
        Path target = Paths.get(this.managingPath.toAbsolutePath().toString(), fileName);

        long size = 0;
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
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        FileDescriptor descriptor = new FileDescriptor("disk", fileName, this.url + fileName);
        descriptor.attr("file", fileName);
        descriptor.attr("path", target.toString());
        descriptor.attr("size", size);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Write file : " + descriptor);
        }

        return descriptor;
    }

    @Override
    public byte[] readFile(FileDescriptor descriptor) {
        return new byte[0];
    }

    @Override
    public String readFileToDisk(String fileName) {
        return null;
    }
}
