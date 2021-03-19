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

package cube.service.conference;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.Conference;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONObject;

import java.util.List;

/**
 * 会议数据存储器。
 */
public class ConferenceStorage implements Storagable {

    private final String conferenceTablePrefix = "conference_";

    private final StorageField[] conferenceFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("subject", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("password", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("summary", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("founder_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("schedule_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("expire_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("max_participants", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("group_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("comm_field_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("cancelled", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private Storage storage;

    public ConferenceStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "ConferenceStorage", config);
    }

    @Override
    public void open() {
        this.storage.open();
    }

    @Override
    public void close() {
        this.storage.close();
    }

    public void writeConference(Conference conference) {
        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, conference.getId().longValue()),
                new StorageField("code", LiteralBase.STRING, conference.getCode()),
                new StorageField("subject", LiteralBase.STRING, conference.getSubject()),
                new StorageField("password", LiteralBase.STRING, conference.getPassword()),
                new StorageField("summary", LiteralBase.STRING, conference.getSummary()),
                new StorageField("founder_id", LiteralBase.LONG, conference.getFounder().getId().longValue()),
                new StorageField("creation", LiteralBase.LONG, conference.getCreation()),
                new StorageField("schedule_time", LiteralBase.LONG, conference.getScheduleTime()),
                new StorageField("expire_time", LiteralBase.LONG, conference.getExpireTime()),
                new StorageField("max_participants", LiteralBase.INT, conference.getMaxParticipants()),
                new StorageField("group_id", LiteralBase.LONG, conference.getParticipantGroup().getId().longValue()),
                new StorageField("comm_field_id", LiteralBase.LONG, (null != conference.getCommField()) ?
                        conference.getCommField().getId().longValue() : 0L),
                new StorageField("cancelled", LiteralBase.INT, conference.isCancelled() ? 1 : 0)
        };

        String table = SQLUtils.correctTableName(this.conferenceTablePrefix + conference.getDomain().getName());
        this.storage.executeInsert(table, fields);
    }

    public Conference readConference(String code) {
        return null;
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            this.checkConferenceTable(domain);
        }
    }

    private void checkConferenceTable(String domain) {
        String table = SQLUtils.correctTableName(this.conferenceTablePrefix + domain);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.conferenceFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }
}
