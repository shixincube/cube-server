/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.conference;

import cube.common.entity.Contact;

/**
 * 会议管理的联系人描述。
 */
public class ConferenceAdministrator extends Contact {

    public ConferenceAdministrator(Long id, String domainName, String name) {
        super(id, domainName, name);
    }
}
