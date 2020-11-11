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

package cube.service.filestorage.test;

import cell.util.CachedQueueExecutor;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.entity.HierarchyNode;
import cube.service.filestorage.FileStructStorage;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.hierarchy.FileHierarchyManager;
import cube.storage.StorageType;
import cube.util.Assert;

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

    private FileStructStorage fileStructStorage;

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

        this.manager = new FileHierarchyManager(this.fileStructStorage);
    }

    public void test() {
        Logger.i(getClass(), "test");

        String domainName = this.domainList.get(0);

        // 获取根目录
        FileHierarchy fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);

        HierarchyNode root = fileHierarchy.getRoot();
        Assert.equals(contactId, root.getId());
        Assert.equals(domainName, root.getDomain().getName());
        Assert.equals(0, root.numChildren());

        // 创建子目录
        String dir1Name = "这是一级目录-相册文件夹";
        HierarchyNode dir1 = fileHierarchy.createDirectory(root, dir1Name);
        Assert.equals(dir1Name, fileHierarchy.getDirectoryName(dir1));
        // 判断子目录数量
        Assert.equals(1, root.numChildren());

        // 再创创建子目录
        String dir2Name = "这是一级目录-资料文件夹";
        HierarchyNode dir2 = fileHierarchy.createDirectory(root, dir2Name);
        Assert.equals(dir2Name, fileHierarchy.getDirectoryName(dir2));
        // 判断子目录数量
        Assert.equals(2, root.numChildren());

        // 清空内存
        this.manager.clearMemory();

        // 重新获取根目录
        fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);
        root = fileHierarchy.getRoot();
        Assert.equals(2, root.numChildren());

        // 删除第二个目录
        dir2 = root.getChildren().get(1);
        fileHierarchy.deleteDirectory(dir2);
        // 判断目录数量
        Assert.equals(1, root.numChildren());

        // 删除第一个目录
        dir1 = root.getChildren().get(0);
        fileHierarchy.deleteDirectory(dir1);
        // 判断目录数量
        Assert.equals(0, root.numChildren());

        this.manager.clearMemory();
    }

    public void testDepth() {
        Logger.i(getClass(), "test");

        String domainName = this.domainList.get(0);

        // 获取根目录
        FileHierarchy fileHierarchy = this.manager.getFileHierarchy(contactId, domainName);

        HierarchyNode parent = fileHierarchy.getRoot();

        int depth = 32;
        for (int i = 0; i < depth; ++i) {
            HierarchyNode dir = fileHierarchy.createDirectory(parent, "这是第[" + (0+1) + "]层");
            parent = dir;
        }

        // 清空内存
        this.manager.clearMemory();


    }

    public void teardown() {
        Logger.i(getClass(), "teardown");

        this.fileStructStorage.close();

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
        this.fileStructStorage = new FileStructStorage(this.executor, StorageType.SQLite, config);

        this.fileStructStorage.open();

        this.fileStructStorage.execSelfChecking(this.domainList);
    }

    public static void main(String[] args) {
        FileHierarchyTest testCase = new FileHierarchyTest();

        testCase.setup();

        testCase.test();

        testCase.testDepth();

        testCase.teardown();
    }
}
