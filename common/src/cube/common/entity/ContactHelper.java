/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 联系人辅助函数。
 */
public class ContactHelper {

    private ContactHelper() {
    }

    public static AbstractContact create(JSONObject data) {
        if (Group.isGroup(data)) {
            return new Group(data);
        }
        else if (AnonymousContact.isAnonymous(data)) {
            return new AnonymousContact(data);
        }
        else {
            return new Contact(data);
        }
    }
}
