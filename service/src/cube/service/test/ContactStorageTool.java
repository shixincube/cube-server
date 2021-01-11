/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.service.contact.ContactStorage;
import cube.storage.StorageType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 联系人存储工具。
 */
public class ContactStorageTool {

    private ExecutorService executor;

    private ContactStorage storage;

    public ContactStorageTool() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(1);

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
        long beginning = now - 7 * 24 * 60 * 60 * 1000L;

        List<Group> list = this.storage.readGroupsWithMember(domainName, memberId, beginning, now);
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

        for (Contact member : group.getMembers()) {
            buf.append(member.toCompactJSON()).append("\n");
        }

        return buf.toString();
    }

    public void open() {
        this.storage.open();

        List<String> domainList = new ArrayList<>();
        domainList.add("shixincube.com");
        this.storage.execSelfChecking(domainList);
    }

    public void close() {
        this.storage.close();
        this.executor.shutdown();
    }

    public static void main(String[] args) {
        ContactStorageTool tool = new ContactStorageTool();
        tool.open();

        System.out.println(tool.printGroups("shixincube.com", 50001001L));

//        System.out.println(tool.printGroupMembers("shixincube.com", 3960496863L));

        tool.close();
    }
}
