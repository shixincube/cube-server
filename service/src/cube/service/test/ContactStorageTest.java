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

package cube.service.test;

import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.service.contact.ContactStorage;
import cube.storage.StorageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息存储测试。
 */
public class ContactStorageTest {

    private boolean clean = true;

    private ExecutorService executor;

    private ContactStorage storage;

    private List<String> domainList;

    private Group group = null;
    private Contact member = null;

    public ContactStorageTest() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);

        String dbfile = "storage/test-contacts.db";
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
        this.storage = new ContactStorage(this.executor, StorageType.SQLite, config);
    }

    public void setup() {
        System.out.println(this.getClass().getName() + " setup");

        this.storage.open();

        String domain = "shixincube.com";

        this.domainList = new ArrayList<>();
        this.domainList.add(domain);

        this.storage.execSelfChecking(this.domainList);

        Contact owner = new Contact(Utils.generateSerialNumber(), domain, "Cube-SHIXIN");
        this.group = new Group(Utils.generateSerialNumber(), domain, "Group-1", owner, System.currentTimeMillis());
        this.member = new Contact(Utils.generateSerialNumber(), domain, "Cube-" + Utils.randomString(8));
        this.group.addMember(this.member);
    }

    public void teardown() {
        System.out.println(this.getClass().getName() + " teardown");
        this.storage.close();
        this.executor.shutdown();
    }

    public void testWrite() {
        System.out.println(this.getClass().getName() + " testWrite");

        AtomicBoolean completed = new AtomicBoolean(false);

        long time = System.currentTimeMillis();
        this.storage.writeGroup(this.group, new Runnable() {
            @Override
            public void run() {
                System.out.println("testWrite: " + (System.currentTimeMillis() - time) + " ms");
                completed.set(true);
            }
        });

        do {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!completed.get());
    }

    public void testRead() {
        System.out.println(this.getClass().getName() + " testRead");

        long time = System.currentTimeMillis();
        Group actualGroup = this.storage.readGroup(this.group.getDomain().getName(), this.group.getId());
        System.out.println("testRead: " + (System.currentTimeMillis() - time) + " ms");

        if (null == actualGroup) {
            System.err.println("Read group error");
            return;
        }

        if (!actualGroup.equals(this.group)) {
            System.err.println("Group error : \n" + this.group.toJSON().toString() +
                    "\n" + actualGroup.toJSON().toString());
            return;
        }

        System.out.println("testRead - OK");
    }

    public void testAddMember() {
        System.out.println(this.getClass().getName() + " testAddMember");

        AtomicBoolean completed = new AtomicBoolean(false);

        Contact member = new Contact(Utils.generateSerialNumber(), this.group.getDomain(), "Cube-" + Utils.randomString(8));
        this.group.addMember(member);

        long time = System.currentTimeMillis();
        ArrayList<Contact> members = new ArrayList<>();
        members.add(member);
        this.storage.addGroupMembers(this.group, members, this.group.getOwner().getId(), new Runnable() {
            @Override
            public void run() {
                System.out.println("testAddMember: " + (System.currentTimeMillis() - time) + " ms");
                completed.set(true);
            }
        });

        do {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!completed.get());

        Group actualGroup = this.storage.readGroup(this.group.getDomain().getName(), this.group.getId());
        if (!actualGroup.equals(this.group)) {
            System.err.println("Group error : \n" + this.group.toJSON().toString() +
                    "\n" + actualGroup.toJSON().toString());
            return;
        }

        System.out.println("testAddMember - OK");
    }

    public void testRemoveMember() {
        System.out.println(this.getClass().getName() + " testRemoveMember");

        AtomicBoolean completed = new AtomicBoolean(false);

        Contact member = this.group.removeMember(this.group.getMembers().get(this.group.numMembers() - 1));

        long time = System.currentTimeMillis();
        ArrayList<Contact> members = new ArrayList<>();
        members.add(member);
        this.storage.removeGroupMembers(this.group, members, this.group.getOwner().getId(), new Runnable() {
            @Override
            public void run() {
                System.out.println("testRemoveMember: " + (System.currentTimeMillis() - time) + " ms");
                completed.set(true);
            }
        });

        do {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!completed.get());

        Group actualGroup = this.storage.readGroup(this.group.getDomain().getName(), this.group.getId());
        if (!actualGroup.equals(this.group)) {
            System.err.println("Group error : \n" + this.group.toJSON().toString() +
                    "\n" + actualGroup.toJSON().toString());
            return;
        }

        System.out.println("testRemoveMember - OK");
    }

    public void testReadGroupList() {
        System.out.println(this.getClass().getName() + " testReadGroupList");

        List<Group> list = this.storage.readGroupsWithMember(this.group.getDomain().getName(),
                this.member.getId(), 0L, System.currentTimeMillis());
        if (!list.get(0).equals(this.group)) {
            System.err.println("List groups error : \n" + this.group.toJSON().toString());
            return;
        }

        System.out.println("testReadGroupList - OK");
    }

    public static void main(String[] args) {

        ContactStorageTest testCase = new ContactStorageTest();

        testCase.setup();

        testCase.testWrite();

        testCase.testRead();

        testCase.testAddMember();

        testCase.testRemoveMember();

        testCase.testReadGroupList();

        testCase.teardown();
    }
}
