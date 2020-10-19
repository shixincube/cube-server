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

package cube.service.messaging.test;

import cell.util.CachedQueueExecutor;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.service.messaging.MessageStorage;
import cube.storage.StorageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 消息存储测试。
 */
public class MessageStorageTest {

    private ExecutorService executor;

    private MessageStorage storage;

    private List<String> domainList;

    public MessageStorageTest() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);

        JSONObject config = new JSONObject();
        try {
            config.put("file", "storage/test-messages.db");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.storage = new MessageStorage(this.executor, StorageType.SQLite, config);
    }

    public void setup() {
        this.domainList = new ArrayList<>();
        this.domainList.add("shixincube.com");

        this.storage.execSelfChecking(this.domainList);

        this.storage.open();
    }

    public void teardown() {
        this.storage.close();
    }

    public void testWrite() {
        
    }

    public static void main(String[] args) {

        MessageStorageTest testCase = new MessageStorageTest();

        testCase.setup();

        testCase.testWrite();

        testCase.teardown();
    }
}
