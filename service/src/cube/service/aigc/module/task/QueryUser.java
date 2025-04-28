/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module.task;

import cube.aigc.Generatable;
import cube.auth.AuthToken;
import cube.common.entity.Contact;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.service.aigc.module.FlowTask;
import cube.service.contact.ContactManager;

import java.util.ArrayList;
import java.util.List;

public class QueryUser extends FlowTask {

    public final static String TASK_NAME = "{{app_query_user}}";

    private AuthToken token;

    public QueryUser(AIGCService service, AuthToken token, String query) {
        super(service, query);
        this.token = token;
    }

    @Override
    public GeneratingRecord generate(Generatable generator) {
        List<String> platforms = new ArrayList<>();
        platforms.add("Android");
        platforms.add("iOS");
        List<Contact> contacts = ContactManager.getInstance().listContacts(this.token.getDomain(), platforms);
        
        return null;
    }

    private String makeContactTable(List<Contact> contacts) {
        StringBuilder buf = new StringBuilder();
        buf.append("| ID |");
        buf.append(" 账号名 |");
        buf.append(" 注册日期 |");
        return buf.toString();
    }
}
