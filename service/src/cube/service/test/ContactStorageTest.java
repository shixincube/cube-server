/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cube.auth.AuthConsts;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.GroupState;
import cube.service.contact.ContactStorage;
import cube.storage.StorageType;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息存储测试。
 */
public class ContactStorageTest {

    private boolean useSQLite = false;

    private ExecutorService executor;

    private ContactStorage storage;

    private List<String> domainList;

    private Group group = null;
    private Contact member = null;

    public ContactStorageTest() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);

        JSONObject config = new JSONObject();
        if (useSQLite) {
            String dbfile = "storage/test-contacts.db";
            File file = new File(dbfile);
            if (file.exists()) {
                file.delete();
            }

            config.put("file", dbfile);
            this.storage = new ContactStorage(this.executor, StorageType.SQLite, config);
        }
        else {
            config.put("host", "211.157.135.146");
            config.put("port", 63307);
            config.put("schema", "cube_3");
            config.put("user", "root");
            config.put("password", "Cube_2020");
            this.storage = new ContactStorage(this.executor, StorageType.MySQL, config);
        }
    }

    public void setup() {
        System.out.println(this.getClass().getName() + " setup");

        this.storage.open();

        String domain = AuthConsts.DEFAULT_DOMAIN;

        this.domainList = new ArrayList<>();
        this.domainList.add(domain);

        this.storage.execSelfChecking(this.domainList);

        Contact owner = new Contact(Utils.generateSerialNumber(), domain, "Cube-SHIXIN");
        this.group = new Group(Utils.generateSerialNumber(), domain, "Group-1", owner.getId(), System.currentTimeMillis());
        this.member = new Contact(Utils.generateSerialNumber(), domain, "Cube-" + Utils.randomString(8));
        this.group.addMember(this.member.getId());
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
        this.group.addMember(member.getId());

        long time = System.currentTimeMillis();
        ArrayList<Long> members = new ArrayList<>();
        members.add(member.getId());
        this.storage.addGroupMembers(this.group, members, this.group.getOwnerId(), new Runnable() {
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

        Long memberId = this.group.removeMember(this.group.getMembers().get(this.group.numMembers() - 1));

        long time = System.currentTimeMillis();
        ArrayList<Long> members = new ArrayList<>();
        members.add(memberId);
        this.storage.removeGroupMembers(this.group, members, this.group.getOwnerId(), new Runnable() {
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
                this.member.getId(), 0L, System.currentTimeMillis(), GroupState.Normal.code);
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
