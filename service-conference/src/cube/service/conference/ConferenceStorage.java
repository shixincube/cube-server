/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.common.entity.Invitation;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会议数据存储器。
 */
public class ConferenceStorage implements Storagable {

    private final String conferenceTablePrefix = "conference_";

    private final String participantTablePrefix = "participant_";

    /**
     * 会议数据字段。
     */
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
            new StorageField("presenter_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
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

    /**
     * 会议参与者字段。
     */
    private final StorageField[] participantFields = new StorageField[] {
            new StorageField("conference_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("participant_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("invitee", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("display_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("accepted", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("acception_time", LiteralBase.LONG, new Constraint[] {
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

    /**
     *
     * @param conference
     */
    public void writeConference(Conference conference) {
        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, conference.getId().longValue()),
                new StorageField("code", LiteralBase.STRING, conference.getCode()),
                new StorageField("subject", LiteralBase.STRING, conference.getSubject()),
                new StorageField("password", LiteralBase.STRING, conference.getPassword()),
                new StorageField("summary", LiteralBase.STRING, conference.getSummary()),
                new StorageField("founder_id", LiteralBase.LONG, conference.getFounderId().longValue()),
                new StorageField("presenter_id", LiteralBase.LONG, conference.getPresenterId().longValue()),
                new StorageField("creation", LiteralBase.LONG, conference.getCreation()),
                new StorageField("schedule_time", LiteralBase.LONG, conference.getScheduleTime()),
                new StorageField("expire_time", LiteralBase.LONG, conference.getExpireTime()),
                new StorageField("max_participants", LiteralBase.INT, conference.getRoom().getMaxParticipants()),
                new StorageField("group_id", LiteralBase.LONG, conference.getRoom().getParticipantGroupId().longValue()),
                new StorageField("comm_field_id", LiteralBase.LONG, conference.getRoom().getCommFieldId().longValue()),
                new StorageField("cancelled", LiteralBase.INT, conference.isCancelled() ? 1 : 0)
        };

        String table = SQLUtils.correctTableName(this.conferenceTablePrefix + conference.getDomain().getName());
        // 插入数据
        if (this.storage.executeInsert(table, fields)) {
            // 插入会议邀请人列表
            for (Invitation invitation : conference.getInvitees()) {
                this.writeParticipant(conference.getDomain().getName(),
                        conference.getId(), conference.getCreation(), invitation);
            }
        }
    }

    /**
     *
     * @param domain
     * @param founderId
     * @return
     */
    public List<Conference> readConferences(String domain, Long founderId) {
        String table = SQLUtils.correctTableName(this.conferenceTablePrefix + domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.conferenceFields, new Conditional[] {
                Conditional.createEqualTo("founder_id", LiteralBase.LONG, founderId)
        });

        if (result.isEmpty()) {
            return null;
        }

        List<Conference> list = new ArrayList<>(result.size());

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);
            Conference conference = new Conference(map.get("id").getLong(), domain, map.get("code").getString(),
                    map.get("subject").getString(), map.get("password").getString(), map.get("summary").getString(),
                    map.get("founder_id").getLong(), map.get("presenter_id").getLong(),
                    map.get("creation").getLong(), map.get("schedule_time").getLong(), map.get("expire_time").getLong(),
                    map.get("group_id").getLong(), map.get("comm_field_id").getLong());
            conference.getRoom().setMaxParticipants(map.get("max_participants").getInt());
            conference.setCancelled(map.get("cancelled").getInt() == 1);
            list.add(conference);

            // 查询邀请
            List<Invitation> invitations = this.readParticipant(domain, conference.getId());
            if (null != invitations) {
                conference.setInvitees(invitations);
            }
        }

        return list;
    }

    /**
     *
     * @param domain
     * @param conferenceId
     * @return
     */
    public Conference readConference(String domain, Long conferenceId) {
        String table = SQLUtils.correctTableName(this.conferenceTablePrefix + domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.conferenceFields, new Conditional[] {
                Conditional.createEqualTo("id", LiteralBase.LONG, conferenceId)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Conference conference = new Conference(map.get("id").getLong(), domain, map.get("code").getString(),
                map.get("subject").getString(), map.get("password").getString(), map.get("summary").getString(),
                map.get("founder_id").getLong(), map.get("presenter_id").getLong(),
                map.get("creation").getLong(), map.get("schedule_time").getLong(), map.get("expire_time").getLong(),
                map.get("group_id").getLong(), map.get("comm_field_id").getLong());
        conference.getRoom().setMaxParticipants(map.get("max_participants").getInt());
        conference.setCancelled(map.get("cancelled").getInt() == 1);

        // 查询邀请
        List<Invitation> invitations = this.readParticipant(domain, conference.getId());
        if (null != invitations) {
            conference.setInvitees(invitations);
        }

        return conference;
    }

    /**
     *
     *
     * @param domain
     * @param conferenceId
     * @param invitation
     */
    public void writeParticipant(String domain, Long conferenceId, Invitation invitation) {
        String table = SQLUtils.correctTableName(this.participantTablePrefix + domain);

        StorageField[] participant = new StorageField[] {
                new StorageField("invitee", LiteralBase.STRING, invitation.getInvitee()),
                new StorageField("display_name", LiteralBase.STRING, invitation.getDisplayName()),
                new StorageField("accepted", LiteralBase.INT, (invitation.getAccepted() ? 1 : 0)),
                new StorageField("acception_time", LiteralBase.LONG, invitation.getAcceptionTime())
        };

        this.storage.executeUpdate(table, participant, new Conditional[] {
                Conditional.createEqualTo("conference_id", LiteralBase.LONG, conferenceId.longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("participant_id", LiteralBase.LONG, invitation.getId().longValue())
        });
    }

    /**
     *
     * @param domain
     * @param conferenceId
     * @param invitation
     */
    public void writeParticipant(String domain, Long conferenceId, long timestamp, Invitation invitation) {
        String table = SQLUtils.correctTableName(this.participantTablePrefix + domain);

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
            new StorageField("timestamp", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("conference_id", LiteralBase.LONG, conferenceId.longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("participant_id", LiteralBase.LONG, invitation.getId().longValue())
        });

        StorageField[] participant = new StorageField[] {
                new StorageField("conference_id", LiteralBase.LONG, conferenceId.longValue()),
                new StorageField("timestamp", LiteralBase.LONG, timestamp),
                new StorageField("participant_id", LiteralBase.LONG, invitation.getId().longValue()),
                new StorageField("invitee", LiteralBase.STRING, invitation.getInvitee()),
                new StorageField("display_name", LiteralBase.STRING, invitation.getDisplayName()),
                new StorageField("accepted", LiteralBase.INT, (invitation.getAccepted() ? 1 : 0)),
                new StorageField("acception_time", LiteralBase.LONG, invitation.getAcceptionTime())
        };

        if (result.isEmpty()) {
            // 没有记录，插入数据
            this.storage.executeInsert(table, participant);
        }
        else {
            // 有记录，更新数据
            this.storage.executeUpdate(table, participant, new Conditional[] {
                    Conditional.createEqualTo("conference_id", LiteralBase.LONG, conferenceId.longValue()),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("participant_id", LiteralBase.LONG, invitation.getId().longValue())
            });
        }
    }

    /**
     *
     * @param domain
     * @param conferenceId
     * @return
     */
    public List<Invitation> readParticipant(String domain, Long conferenceId) {
        String table = SQLUtils.correctTableName(this.participantTablePrefix + domain);

        List<StorageField[]> result = this.storage.executeQuery(table, this.participantFields, new Conditional[] {
                Conditional.createEqualTo("conference_id", LiteralBase.LONG, conferenceId.longValue())
        });

        if (result.isEmpty()) {
            return null;
        }

        List<Invitation> list = new ArrayList<>();
        for (StorageField[] data : result) {
            Map<String, StorageField> map = StorageFields.get(data);
            Invitation invitation = new Invitation(map.get("participant_id").getLong(),
                    map.get("invitee").getString(), map.get("display_name").getString(),
                    (map.get("accepted").getInt() == 1), map.get("acception_time").getLong());
            list.add(invitation);
        }
        return list;
    }

    /**
     * 查询包含指定参与人的会议 ID 列表。
     *
     * @param domain
     * @param participantId
     * @param beginning
     * @param ending
     * @return
     */
    public List<Long> listConferenceId(String domain, Long participantId, long beginning, long ending) {
        List<Long> result = new ArrayList<>();

        // 从参与人表里查询 ID
        String table = SQLUtils.correctTableName(this.participantTablePrefix + domain);

        List<StorageField[]> ptcpResult = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("conference_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("participant_id", LiteralBase.LONG, participantId),
                Conditional.createAnd(),
                Conditional.createBracket(new Conditional[] {
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", LiteralBase.LONG, beginning)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("timestamp", LiteralBase.LONG, ending))
                })
        });

        if (!ptcpResult.isEmpty()) {
            for (StorageField[] data : ptcpResult) {
                result.add(data[0].getLong());
            }
        }

        // 从会议表里查询 ID
        table = SQLUtils.correctTableName(this.conferenceTablePrefix + domain);

        List<StorageField[]> confResult = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("founder_id", LiteralBase.LONG, participantId),
                Conditional.createAnd(),
                Conditional.createBracket(new Conditional[] {
                        Conditional.createGreaterThanEqual(new StorageField("creation", LiteralBase.LONG, beginning)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("creation", LiteralBase.LONG, ending))
                })
        });

        if (!confResult.isEmpty()) {
            for (StorageField[] data : confResult) {
                result.add(data[0].getLong());
            }
        }

        return result;
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            this.checkConferenceTable(domain);
            this.checkParticipantTable(domain);
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

    private void checkParticipantTable(String domain) {
        String table = SQLUtils.correctTableName(this.participantTablePrefix + domain);

        if (!this.storage.exist(table)) {
            // 表不存在，键表
            if (this.storage.executeCreate(table, this.participantFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }
}
