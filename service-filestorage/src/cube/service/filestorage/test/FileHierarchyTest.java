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

package cube.service.filestorage.test;

import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.service.filestorage.ServiceStorage;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.hierarchy.FileHierarchyManager;
import cube.storage.StorageType;
import cube.util.Assert;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 文件层级测试用例。
 */
public class FileHierarchyTest {

    private ExecutorService executor;

    private ServiceStorage serviceStorage;

    private List<String> domainList;

    private FileHierarchyManager manager;

    private Long contactId = 5001967L;

    public FileHierarchyTest() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);
        this.domainList = new ArrayList<>();
        this.domainList.add("shixincube.com");
    }

    public void setup() {
        Logger.i(getClass(), "setup");
        this.initStorage();

        this.manager = new FileHierarchyManager(this.serviceStorage, null);
    }

    public void test() {
        Logger.i(getClass(), "test");

        String domainName = this.domainList.get(0);

        // 获取根目录
        FileHierarchy fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);

        Directory root = fileHierarchy.getRoot();
        Assert.equals(contactId, root.getId());
        Assert.equals(domainName, root.getDomain().getName());
        Assert.equals(0, root.numDirectories());

        // 创建子目录
        String dir1Name = "这是一级目录-相册文件夹";
        Directory dir1 = root.createDirectory(dir1Name);
        Assert.equals(dir1Name, dir1.getName());
        // 判断子目录数量
        Assert.equals(1, root.numDirectories());

        // 再创创建子目录
        String dir2Name = "这是一级目录-资料文件夹";
        Directory dir2 = root.createDirectory(dir2Name);
        Assert.equals(dir2Name, dir2.getName());
        // 判断子目录数量
        Assert.equals(2, root.numDirectories());

        // 清空内存
        this.manager.clearMemory();

        // 重新获取根目录
        fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);
        root = fileHierarchy.getRoot();
        Assert.equals(2, root.numDirectories());

        // 删除第二个目录
        List<Directory> dirs = root.getDirectories();
        dir2 = dirs.get(1);
        Assert.equals("delete dir2", true, root.deleteDirectory(dir2, false));
        // 判断目录数量
        Assert.equals(1, root.numDirectories());

        // 删除第一个目录
        dirs = root.getDirectories();
        dir1 = dirs.get(0);
        root.deleteDirectory(dir1, false);
        // 判断目录数量
        Assert.equals(0, root.numDirectories());

        this.manager.clearMemory();
    }

    public void testDepth() {
        Logger.i(getClass(), "test");

        String domainName = this.domainList.get(0);

        // 读取
        FileHierarchy fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);

        Directory parent = fileHierarchy.getRoot();

        int depth = 32;
        for (int i = 0; i < depth; ++i) {
            Directory dir = parent.createDirectory("这是第[" + (i+1) + "]层");
            System.out.println("New dir : " + dir.getName());
            parent = dir;
        }

        // 清空内存
        this.manager.clearMemory();

        // 逐层遍历
        int count = 0;
        fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);
        parent = fileHierarchy.getRoot();
        while (parent.numDirectories() > 0) {
            List<Directory> subs = parent.getDirectories();
            Directory dir = subs.get(0);
            System.out.println("Dir : " + dir.getName());
            parent = dir;
            ++count;
        }

        Assert.equals("Depth", depth, count);
    }

    public void teardown() {
        Logger.i(getClass(), "teardown");

        this.serviceStorage.close();

        this.executor.shutdown();
    }

    private void initStorage() {
        String file = "storage/test-FileStorageService.db";

        try {
            Files.delete(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject config = new JSONObject();
        try {
            config.put("file", file);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.serviceStorage = new ServiceStorage(this.executor, StorageType.SQLite, config);

        this.serviceStorage.open();

        this.serviceStorage.execSelfChecking(this.domainList);
    }

    public static void main(String[] args) {
        FileHierarchyTest testCase = new FileHierarchyTest();

        testCase.setup();

        testCase.test();

        testCase.testDepth();

        testCase.teardown();
    }
}
