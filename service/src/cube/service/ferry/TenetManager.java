/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
