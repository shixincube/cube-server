/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cube.auth.AuthConsts;
import cube.common.entity.Group;
import cube.common.entity.GroupState;
import cube.service.contact.ContactStorage;
import cube.storage.StorageType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 联系人存储工具。
 */
public class ContactStorageTool {

    private ExecutorService executor;

    private ContactStorage storage;

    public ContactStorageTool() {
        this.executor = Executors.newCachedThreadPool();

        JSONObject config = new JSONObject();
        try {
            config.put("file", "storage/ContactService.db");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.storage = new ContactStorage(this.executor, StorageType.SQLite, config);
    }

    public String printGroups(String domainName, Long memberId) {
        StringBuilder buf = new StringBuilder();

        long now = System.currentTimeMillis();
        long beginning = now - 7L * 24 * 60 * 60 * 1000L;

        List<Group> list = this.storage.readGroupsWithMember(domainName, memberId, beginning, now, GroupState.Normal.code);
        for (Group group : list) {
            buf.append(group.toCompactJSON());
            buf.append("\n");
        }

        return buf.toString();
    }

    public String printGroupMembers(String domainName, Long groupId) {
        Group group = this.storage.readGroup(domainName, groupId);
        StringBuilder buf = new StringBuilder();
        buf.append(group.toCompactJSON()).append("\n");

        for (Long memberId : group.getMembers()) {
            buf.append(memberId).append("\n");
        }

        return buf.toString();
    }

    public void open() {
        this.storage.open();

        List<String> domainList = new ArrayList<>();
        domainList.add(AuthConsts.DEFAULT_DOMAIN);
        this.storage.execSelfChecking(domainList);
    }

    public void close() {
        this.storage.close();
        this.executor.shutdown();
    }

    public static void main(String[] args) {
        ContactStorageTool tool = new ContactStorageTool();
        tool.open();

        System.out.println(tool.printGroups(AuthConsts.DEFAULT_DOMAIN, 50001001L));

//        System.out.println(tool.printGroupMembers(AuthConsts.DEFAULT_DOMAIN, 3960496863L));

        tool.close();
    }
}
