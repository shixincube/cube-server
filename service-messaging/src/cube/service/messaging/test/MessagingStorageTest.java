/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cell.util.Utils;
import cube.auth.AuthConsts;
import cube.common.entity.Message;
import cube.service.messaging.MessagingStorage;
import cube.storage.StorageType;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息存储测试。
 */
public class MessagingStorageTest {

    private boolean clean = true;

    private ExecutorService executor;

    private MessagingStorage storage;

    private List<String> domainList;

    private List<Message> messageList;
    private HashMap<Long, Message> messageMap;

    public MessagingStorageTest() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);

        String dbfile = "storage/test-messages.db";
        if (clean) {
            File file = new File(dbfile);
            if (file.exists()) {
                file.delete();
            }
        }

        JSONObject config = new JSONObject();
        try {
            config.put("file", dbfile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.storage = new MessagingStorage(this.executor, StorageType.SQLite, config);
    }

    public void setup() {
        System.out.println(this.getClass().getName() + " setup");

        this.storage.open();

        this.domainList = new ArrayList<>();
        this.domainList.add(AuthConsts.DEFAULT_DOMAIN);

        this.storage.execSelfChecking(this.domainList);

        this.messageList = new ArrayList<>();
        this.messageMap = new HashMap<>();
        for (int i = 0; i < 9; ++i) {
            JSONObject json = new JSONObject();
            try {
                JSONObject payload = new JSONObject();
                payload.put("content", Utils.randomString(128));

                json.put("domain", this.domainList.get(0));
                json.put("id", Utils.generateSerialNumber());
                json.put("from", 500100L);
                json.put("to", 500200L);
                json.put("source", 0L);
                json.put("lts", System.currentTimeMillis() + i);
                json.put("rts", System.currentTimeMillis() + Utils.randomInt(1000, 9999));
                json.put("payload", payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(json);
            this.messageList.add(message);
            this.messageMap.put(message.getId(), message);
        }

        // 特殊消息
        this.makeSpecialMessage();
    }

    public void teardown() {
        System.out.println(this.getClass().getName() + " teardown");
        this.storage.close();
        this.executor.shutdown();
    }

    public void testWrite() {
        System.out.println(this.getClass().getName() + " testWrite");

        AtomicInteger count = new AtomicInteger(this.messageList.size());

        for (Message message : this.messageList) {
            this.storage.write(message, new Runnable() {
                @Override
                public void run() {
                    count.decrementAndGet();
                }
            });
        }

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Wait for writing " + count.get());
        } while (count.get() > 0);
    }

    public void testRead() {
        System.out.println(this.getClass().getName() + " testRead");

        List<Long> idList = new ArrayList<>();
        for (Message message : this.messageList) {
            idList.add(message.getId());
            System.out.println("Message id: " + message.getId());
        }

        List<Message> actualList = this.storage.read(this.domainList.get(0), 500100L, idList);
        if (actualList.size() != this.messageList.size()) {
            System.err.println("Error size : " + actualList.size() + " != " + this.messageList.size());
            return;
        }

        for (int i = 0; i < actualList.size(); ++i) {
            Message actual = actualList.get(i);
            Message expected = this.messageMap.get(actual.getId());
            if (!actual.equals(expected)) {
                System.err.println("Message error - actual : " + actual.getId() + " - " + actual.getPayload().toString());
                System.err.println("Message error - expected : " + expected.getId() + " - " + expected.getPayload().toString());
                return;
            }
            else {
                if (!actual.getPayload().toString().equals(expected.getPayload().toString())) {
                    System.err.println("Message error - actual : " + actual.getId() + " - " + actual.getPayload().toString());
                    System.err.println("Message error - expected : " + expected.getId() + " - " + expected.getPayload().toString());
                    return;
                }
            }
        }

        System.out.println("testRead - OK (" + actualList.size() + ")");
    }

    public void makeSpecialMessage() {
        JSONObject json = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put("content", Utils.randomString(16) + "_" +
                    "A 'boy', a [girl], a \"中文\", 20% , (新世纪), {新征程}");

            json.put("domain", this.domainList.get(0));
            json.put("id", Utils.generateSerialNumber());
            json.put("from", 500100L);
            json.put("to", 500200L);
            json.put("source", 0L);
            json.put("lts", System.currentTimeMillis());
            json.put("rts", System.currentTimeMillis() + Utils.randomInt(1000, 9999));
            json.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Message message = new Message(json);
        this.messageList.add(message);
        this.messageMap.put(message.getId(), message);
    }

    public static void main(String[] args) {

        MessagingStorageTest testCase = new MessagingStorageTest();

        testCase.setup();

        testCase.testWrite();

        testCase.testRead();

        testCase.teardown();
    }
}
