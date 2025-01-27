/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry;

import cell.adapter.CelletAdapter;
import cube.common.ModuleEvent;
import cube.common.UniqueKey;
import cube.common.entity.Contact;
import cube.ferry.DomainInfo;
import cube.ferry.DomainMember;
import cube.ferry.FerryAction;
import cube.service.contact.ContactManager;
import cube.service.ferry.tenet.Tenet;

import java.util.List;

/**
 * 驱动终端的 Tenet 管理器。
 */
public class TenetManager {

    private final static TenetManager instance = new TenetManager();

    private FerryService service;

    private FerryStorage storage;

    private CelletAdapter contactsAdapter;

    private TenetManager() {
    }

    public static TenetManager getInstance() {
        return TenetManager.instance;
    }

    public void start(FerryService service, FerryStorage storage, CelletAdapter contactsAdapter) {
        this.service = service;
        this.storage = storage;
        this.contactsAdapter = contactsAdapter;
    }

    public void stop() {
    }

    public void triggerTenet(Tenet tenet) {
        List<DomainMember> list = this.storage.queryMembers(tenet.getDomain(), DomainMember.NORMAL);

        // 向所有成员写入 Tenet
        for (DomainMember member : list) {
            // 入库
            this.storage.writeTenet(member.getContactId(), tenet);

            // 发送事件
            ModuleEvent event = new ModuleEvent(FerryService.NAME, FerryAction.Tenet.name, tenet.toJSON());
            String key = UniqueKey.make(member.getContactId(), member.getDomain());
            this.contactsAdapter.publish(key, event.toJSON());
        }
    }
}
