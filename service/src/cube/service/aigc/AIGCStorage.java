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

package cube.service.aigc;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.atom.Atom;
import cube.common.Storagable;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * AIGC 存储器。
 */
public class AIGCStorage implements Storagable {

    public final static String ITEM_NAME_MODELS = "models";

    private final String appConfigTable = "aigc_app_config";

    private final String appInvitationTable = "aigc_app_invitation";

    private final String appNotificationTable = "aigc_app_notification";

    private final String appEventTable = "aigc_app_event";

    private final String queryAnswerTable = "aigc_query_answer";

    private final String knowledgeProfileTable = "aigc_knowledge_profile";

    private final String knowledgeDocTable = "aigc_knowledge_doc";

    private final String knowledgeArticleTable = "aigc_knowledge_article";

    private final String knowledgeParaphraseTable = "aigc_knowledge_paraphrase";

    private final String chartReactionTable = "aigc_chart_reaction";

    private final String chartSeriesTable = "aigc_chart_series";

    private final String chartAtomTable = "aigc_chart_atom";

    private final String promptWordTable = "aigc_prompt_word";

    private final String promptWordScopeTable = "aigc_prompt_word_scope";

    /**
     * 用量表。
     */
    private final String usageTable = "aigc_usage";

    // 联系人偏好，用于标记联系偏好的内置知识库。
    private final String contactPreferenceTable = "aigc_contact_preference";

    private final StorageField[] appConfigFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("item", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("value", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("comment", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("modified", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private final StorageField[] appInvitationFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("invitation", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("token", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] appNotificationFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("type", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("date", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] appEventFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("event", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("time", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] queryAnswerFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("unit", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_cid", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("answer_cid", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("answer_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("answer_content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 人类反馈的得分
            new StorageField("feedback", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("context_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private final StorageField[] knowledgeProfileFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("max_size", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("scope", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] knowledgeDocFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("activated", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_1
            }),
            new StorageField("num_segments", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("scope", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] knowledgeArticleFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("category", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("author", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("year", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("month", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("date", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] knowledgeParaphraseFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("parent_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("category", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("word", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("paraphrase", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    /**
     * 图表的感应词表。
     * 一个感应词可以对应多个数据序列。
     */
    private final StorageField[] chartReactionFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("primary", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("secondary", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("tertiary", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("quaternary", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("series_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
    };

    private final StorageField[] chartSeriesFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("desc", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("x_axis", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("type", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("option", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
    };

    private final StorageField[] chartAtomFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("label", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("year", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("month", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("date", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("value", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("value_string", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
    };

    private final StorageField[] promptWordFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("act", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("prompt", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
    };

    private final StorageField[] promptWordScopeFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("prompt_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
    };

    private final StorageField[] usageFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.UNIQUE
            }),
            new StorageField("model", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("completion_tokens", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("prompt_tokens", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            })
    };

    private Storage storage;

    public AIGCStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "AIGCStorage", config);
    }

    @Override
    public void open() {
        this.storage.open();
    }

    @Override
    public void close() {
        this.storage.close();
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {
        if (!this.storage.exist(this.appConfigTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.appConfigTable, this.appConfigFields)) {
                Logger.i(this.getClass(), "Created table '" + this.appConfigTable + "' successfully");

                // 插入默认数值
                resetDefaultConfig();
            }
        }

        if (!this.storage.exist(this.appInvitationTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.appInvitationTable, this.appInvitationFields)) {
                Logger.i(this.getClass(), "Created table '" + this.appInvitationTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.appNotificationTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.appNotificationTable, this.appNotificationFields)) {
                Logger.i(this.getClass(), "Created table '" + this.appNotificationTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.appEventTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.appEventTable, this.appEventFields)) {
                Logger.i(this.getClass(), "Created table '" + this.appEventTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.queryAnswerTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.queryAnswerTable, this.queryAnswerFields)) {
                Logger.i(this.getClass(), "Created table '" + this.queryAnswerTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeProfileTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeProfileTable, this.knowledgeProfileFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeProfileTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeDocTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeDocTable, this.knowledgeDocFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeDocTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeArticleTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeArticleTable, this.knowledgeArticleFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeArticleTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeParaphraseTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeParaphraseTable, this.knowledgeParaphraseFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeParaphraseTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.chartReactionTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.chartReactionTable, this.chartReactionFields)) {
                Logger.i(this.getClass(), "Created table '" + this.chartReactionTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.chartSeriesTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.chartSeriesTable, this.chartSeriesFields)) {
                Logger.i(this.getClass(), "Created table '" + this.chartSeriesTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.chartAtomTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.chartAtomTable, this.chartAtomFields)) {
                Logger.i(this.getClass(), "Created table '" + this.chartAtomTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.promptWordTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.promptWordTable, this.promptWordFields)) {
                Logger.i(this.getClass(), "Created table '" + this.promptWordTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.promptWordScopeTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.promptWordScopeTable, this.promptWordScopeFields)) {
                Logger.i(this.getClass(), "Created table '" + this.promptWordScopeTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.usageTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.usageTable, this.usageFields)) {
                Logger.i(this.getClass(), "Created table '" + this.usageTable + "' successfully");
            }
        }
    }

    public ModelConfig getModelConfig(String modelName) {
        List<StorageField[]> result = this.storage.executeQuery(this.appConfigTable, new StorageField[] {
                new StorageField("value", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("item", modelName)
        });

        if (result.isEmpty()) {
            return null;
        }

        return new ModelConfig(new JSONObject(result.get(0)[0].getString()));
    }

    public List<ModelConfig> getModelConfigs() {
        List<ModelConfig> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.appConfigTable, new StorageField[] {
                new StorageField("value", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("item", ITEM_NAME_MODELS)
        });
        if (result.isEmpty()) {
            return list;
        }

        JSONArray models = new JSONArray(result.get(0)[0].getString());
        for (int i = 0; i < models.length(); ++i) {
            String unitName = models.getString(i);
            result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                    Conditional.createEqualTo("item", unitName)
            });

            for (StorageField[] fields : result) {
                Map<String, StorageField> data = StorageFields.get(fields);
                JSONObject value = new JSONObject(data.get("value").getString());
                ModelConfig config = new ModelConfig(value);
                list.add(config);
            }
        }

        return list;
    }

    public String readTokenByInvitation(String invitation) {
        List<StorageField[]> result = this.storage.executeQuery(this.appInvitationTable, new StorageField[] {
                    new StorageField("token", LiteralBase.STRING)
            }, new Conditional[] {
                    Conditional.createEqualTo("invitation", invitation)
            });
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0)[0].getString();
    }

    public boolean writeInvitation(String invitation, String token) {
        return this.storage.executeInsert(this.appInvitationTable, new StorageField[] {
                new StorageField("invitation", invitation),
                new StorageField("token", token)
        });
    }

    public List<AppEvent> readAppEvents(long contactId, String eventName,
                                        long startTimestamp, long endTimestamp) {
        List<AppEvent> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.appEventTable, this.appEventFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("event", eventName),
                        Conditional.createAnd(),
                        Conditional.createBracket(new Conditional[] {
                                Conditional.createGreaterThanEqual(new StorageField("timestamp", startTimestamp)),
                                Conditional.createAnd(),
                                Conditional.createLessThanEqual(new StorageField("timestamp", endTimestamp))
                        })
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            AppEvent event = new AppEvent(data.get("event").getString(), data.get("timestamp").getLong(),
                    data.get("time").getString(), data.get("contact_id").getLong(),
                    new JSONObject(data.get("data").getString()));
            list.add(event);
        }

        return list;
    }

    public boolean writeAppEvent(AppEvent appEvent) {
        return this.storage.executeInsert(this.appEventTable, new StorageField[] {
                new StorageField("event", appEvent.event),
                new StorageField("time", appEvent.time),
                new StorageField("timestamp", appEvent.timestamp),
                new StorageField("contact_id", appEvent.contactId),
                new StorageField("data", appEvent.getSafeData().toString())
        });
    }

    public List<AIGCChatHistory> readChatHistoryByContactId(long contactId) {
        List<AIGCChatHistory> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.queryAnswerTable,
                this.queryAnswerFields, new Conditional[] {
                        Conditional.createEqualTo("query_cid", contactId),
                        Conditional.createOr(),
                        Conditional.createEqualTo("answer_cid", contactId)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            AIGCChatHistory history = new AIGCChatHistory(data.get("id").getLong(), data.get("sn").getLong());
            history.unit = data.get("unit").getString();
            history.queryContactId = data.get("query_cid").getLong();
            history.queryTime = data.get("query_time").getLong();
            history.queryContent = data.get("query_content").getString();
            history.answerContactId = data.get("answer_cid").getLong();
            history.answerTime = data.get("answer_time").getLong();
            history.answerContent = data.get("answer_content").getString();
            history.feedback = data.get("feedback").getInt();
            history.contextId = data.get("context_id").getLong();
            list.add(history);
        }

        return list;
    }

    public void writeChatHistory(AIGCChatHistory history) {
        this.storage.executeInsert(this.queryAnswerTable, new StorageField[] {
                new StorageField("sn", history.sn),
                new StorageField("unit", history.unit),
                new StorageField("query_cid", history.queryContactId),
                new StorageField("query_time", history.queryTime),
                new StorageField("query_content", history.queryContent),
                new StorageField("answer_cid", history.answerContactId),
                new StorageField("answer_time", history.answerTime),
                new StorageField("answer_content", history.answerContent),
                new StorageField("feedback", history.feedback),
                new StorageField("context_id", history.contextId)
        });
    }

    public boolean updateChatHistoryFeedback(long sn, int feedback) {
        return this.storage.executeUpdate(this.queryAnswerTable, new StorageField[] {
                new StorageField("feedback", feedback)
        }, new Conditional[] {
                Conditional.createEqualTo("sn", sn)
        });
    }

    public List<Notification> readEnabledNotifications() {
        List<Notification> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.appNotificationTable, this.appNotificationFields,
                new Conditional[] {
                        Conditional.createEqualTo("state", Notification.STATE_ENABLED)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            Notification notification = new Notification(data.get("id").getLong(),
                    data.get("type").getString(),
                    data.get("state").getInt(),
                    data.get("title").getString(),
                    data.get("content").getString(),
                    data.get("date").getString());
            list.add(notification);
        }

        return list;
    }

    public void writeNotification(Notification notification) {
        this.storage.executeInsert(this.appNotificationTable, new StorageField[] {
                new StorageField("id", notification.id),
                new StorageField("type", notification.type),
                new StorageField("state", notification.state),
                new StorageField("title", notification.title),
                new StorageField("content", notification.content),
                new StorageField("date", notification.date)
        });
    }

    public KnowledgeProfile readKnowledgeProfile(long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeProfileTable, this.knowledgeProfileFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        return new KnowledgeProfile(data.get("id").getLong(), data.get("contact_id").getLong(),
                data.get("domain").getString(), data.get("state").getInt(), data.get("max_size").getLong(),
                KnowledgeScope.parse(data.get("scope").getString()));
    }

    public KnowledgeProfile updateKnowledgeProfile(long contactId, int state, long maxSize, KnowledgeScope scope) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeProfileTable, this.knowledgeProfileFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        if (result.isEmpty()) {
            this.storage.executeInsert(this.knowledgeProfileTable, new StorageField[] {
                    new StorageField("contact_id", contactId),
                    new StorageField("state", state),
                    new StorageField("max_size", maxSize),
                    new StorageField("scope", scope.name)
            });
            return this.readKnowledgeProfile(contactId);
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        long id = data.get("id").getLong();
        String domain = data.get("domain").getString();

        this.storage.executeUpdate(this.knowledgeProfileTable, new StorageField[] {
                new StorageField("state", state),
                new StorageField("max_size", maxSize),
                new StorageField("scope", scope.name)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId)
        });

        return new KnowledgeProfile(id, contactId, domain, state, maxSize, scope);
    }

    public List<KnowledgeDoc> readKnowledgeDocList(String domain) {
        List<KnowledgeDoc> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeDoc doc = new KnowledgeDoc(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("file_code").getString(),
                    data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                    KnowledgeScope.parse(data.get("scope").getString()));
            list.add(doc);
        }

        return list;
    }

    public List<KnowledgeDoc> readKnowledgeDocList(String domain, long contactId) {
        List<KnowledgeDoc> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeDoc doc = new KnowledgeDoc(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("file_code").getString(),
                    data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                    KnowledgeScope.parse(data.get("scope").getString()));
            list.add(doc);
        }

        return list;
    }

    public KnowledgeDoc readKnowledgeDoc(String fileCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("file_code", fileCode)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        KnowledgeDoc doc = new KnowledgeDoc(data.get("id").getLong(), data.get("domain").getString(),
                data.get("contact_id").getLong(), data.get("file_code").getString(),
                data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                KnowledgeScope.parse(data.get("scope").getString()));
        return doc;
    }

    public void writeKnowledgeDoc(KnowledgeDoc doc) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("file_code", doc.fileCode)
        });

        if (!result.isEmpty()) {
            this.storage.executeDelete(this.knowledgeDocTable, new Conditional[] {
                    Conditional.createEqualTo("file_code", doc.fileCode)
            });
        }

        this.storage.executeInsert(this.knowledgeDocTable, new StorageField[] {
                new StorageField("id", doc.getId().longValue()),
                new StorageField("domain", doc.getDomain().getName()),
                new StorageField("contact_id", doc.contactId),
                new StorageField("file_code", doc.fileCode),
                new StorageField("activated", 1),
                new StorageField("num_segments", doc.numSegments),
                new StorageField("scope", doc.scope.name)
        });
    }

    public boolean updateKnowledgeDoc(KnowledgeDoc doc) {
        return this.storage.executeUpdate(this.knowledgeDocTable, new StorageField[] {
                new StorageField("activated", doc.activated ? 1 : 0),
                new StorageField("num_segments", doc.numSegments)
        }, new Conditional[] {
                Conditional.createEqualTo("file_code", doc.fileCode)
        });
    }

    public boolean deleteKnowledgeDoc(String fileCode) {
        return this.storage.executeDelete(this.knowledgeDocTable, new Conditional[] {
                Conditional.createEqualTo("file_code", fileCode)
        });
    }

    public boolean writeKnowledgeArticle(KnowledgeArticle article) {
        return this.storage.executeInsert(this.knowledgeArticleTable, new StorageField[] {
                new StorageField("id", article.getId().longValue()),
                new StorageField("category", article.category),
                new StorageField("title", article.title),
                new StorageField("content", article.content),
                new StorageField("author", article.author),
                new StorageField("year", article.year),
                new StorageField("month", article.month),
                new StorageField("date", article.date),
                new StorageField("timestamp", article.getTimestamp())
        });
    }

    public List<KnowledgeArticle> readKnowledgeArticles(String category) {
        List<KnowledgeArticle> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("category", category)
        });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(),
                    data.get("category").getString(), data.get("title").getString(),
                    data.get("content").getString(), data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong());
            list.add(article);
        }

        return list;
    }

    public List<KnowledgeArticle> readKnowledgeArticles(List<Long> idList) {
        List<KnowledgeArticle> list = new ArrayList<>();
        if (idList.isEmpty()) {
            return list;
        }

        List<Conditional> conditionals = new ArrayList<>();
        for (Long id : idList) {
            conditionals.add(Conditional.createEqualTo("id", (long) id.longValue()));
            conditionals.add(Conditional.createOr());
        }
        conditionals.remove(conditionals.size() - 1);

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                conditionals.toArray(new Conditional[0]));
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(),
                    data.get("category").getString(), data.get("title").getString(),
                    data.get("content").getString(), data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong());
            list.add(article);
        }

        return list;
    }

    public List<KnowledgeArticle> readKnowledgeArticles(String category, int startYear, int startMonth, int startDate) {
        List<KnowledgeArticle> list = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(startYear, startMonth - 1, startDate);
        long timestamp = calendar.getTimeInMillis();
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("category", category),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", timestamp))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(),
                    data.get("category").getString(), data.get("title").getString(),
                    data.get("content").getString(), data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong());
            list.add(article);
        }

        return list;
    }

    /**
     * 删除知识库文章。
     *
     * @param idList
     * @return 返回已删除的文章 ID 列表。
     */
    public List<Long> deleteKnowledgeArticles(List<Long> idList) {
        if (idList.isEmpty()) {
            return null;
        }

        List<Conditional> conditionals = new ArrayList<>();
        for (Long id : idList) {
            conditionals.add(Conditional.createEqualTo("id", (long) id.longValue()));
            conditionals.add(Conditional.createOr());
        }
        conditionals.remove(conditionals.size() - 1);

        List<Long> result = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT `id` FROM " + this.knowledgeArticleTable + " WHERE");
        for (Conditional conditional : conditionals) {
            sql.append(" ").append(conditional.toString());
        }
        List<StorageField[]> ids = this.storage.executeQuery(sql.toString());
        for (StorageField[] data : ids) {
            result.add(data[0].getLong());
        }

        this.storage.executeDelete(this.knowledgeArticleTable,
                conditionals.toArray(new Conditional[0]));
        return result;
    }

    public List<KnowledgeParaphrase> readKnowledgeParaphrases(String category) {
        List<KnowledgeParaphrase> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeParaphraseTable,
                this.knowledgeParaphraseFields, new Conditional[] {
                Conditional.createLike("category", category)
        });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeParaphrase paraphrase = new KnowledgeParaphrase(data.get("id").getLong(),
                    data.get("parent_id").getLong(), data.get("category").getString(),
                    data.get("word").getString(), data.get("paraphrase").getString());
            list.add(paraphrase);
        }

        return list;
    }

    public boolean writeKnowledgeParaphrases(List<KnowledgeParaphrase> list) {
        for (KnowledgeParaphrase paraphrase : list) {
            boolean result = this.storage.executeInsert(this.knowledgeParaphraseTable, new StorageField[] {
                    new StorageField("id", paraphrase.getId().longValue()),
                    new StorageField("parent_id", paraphrase.getParentId()),
                    new StorageField("category", paraphrase.getCategory()),
                    new StorageField("word", paraphrase.getWord()),
                    new StorageField("paraphrase", paraphrase.getParaphrase())
            });
            if (!result) {
                return false;
            }
        }

        return true;
    }

    public List<Long> deleteKnowledgeParaphrases(List<Long> idList) {
        if (idList.isEmpty()) {
            return null;
        }

        List<Conditional> conditionals = new ArrayList<>();
        for (Long id : idList) {
            conditionals.add(Conditional.createEqualTo("id", (long) id.longValue()));
            conditionals.add(Conditional.createOr());
        }
        conditionals.remove(conditionals.size() - 1);

        List<Long> result = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT `id` FROM " + this.knowledgeParaphraseTable + " WHERE");
        for (Conditional conditional : conditionals) {
            sql.append(" ").append(conditional.toString());
        }
        List<StorageField[]> ids = this.storage.executeQuery(sql.toString());
        for (StorageField[] data : ids) {
            result.add(data[0].getLong());
        }

        this.storage.executeDelete(this.knowledgeParaphraseTable,
                conditionals.toArray(new Conditional[0]));
        return result;
    }

    public List<ChartReaction> readChartReactions(String primary, String secondary, String tertiary, String quaternary) {
        List<ChartReaction> list = new ArrayList<>();

        if (null == primary && null == secondary && null == tertiary && null == quaternary) {
            return list;
        }

        List<Conditional> conditionals = new ArrayList<>();
        if (null != primary) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createLike("primary", primary));
        }
        if (null != secondary) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("secondary", secondary));
        }
        if (null != tertiary) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("tertiary", tertiary));
        }
        if (null != quaternary) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("quaternary", quaternary));
        }
        // 时间倒序
        conditionals.add(Conditional.createOrderBy("timestamp", true));
        conditionals.remove(0);

        List<StorageField[]> result = this.storage.executeQuery(this.chartReactionTable, this.chartReactionFields,
                conditionals.toArray(new Conditional[0]));
        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            ChartReaction chartReaction = new ChartReaction(data.get("primary").getString().split(","),
                    data.get("series_name").getString(), data.get("timestamp").getLong());
            chartReaction.sn = data.get("sn").getLong();
            if (!data.get("secondary").isNullValue()) {
                chartReaction.secondary = data.get("secondary").getString();
            }
            if (!data.get("tertiary").isNullValue()) {
                chartReaction.tertiary = data.get("tertiary").getString();
            }
            if (!data.get("quaternary").isNullValue()) {
                chartReaction.quaternary = data.get("quaternary").getString();
            }
            list.add(chartReaction);
        }

        return list;
    }

    public ChartReaction readLastChartReaction(String primary) {
        List<StorageField[]> result = this.storage.executeQuery(this.chartReactionTable,
                this.chartReactionFields, new Conditional[] {
                        Conditional.createLike("primary", primary),
                        Conditional.createOrderBy("timestamp", true),
                        Conditional.createLimit(1)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        ChartReaction chartReaction = new ChartReaction(data.get("primary").getString().split(","),
                data.get("series_name").getString(), data.get("timestamp").getLong());
        chartReaction.sn = data.get("sn").getLong();
        if (!data.get("secondary").isNullValue()) {
            chartReaction.secondary = data.get("secondary").getString();
        }
        if (!data.get("tertiary").isNullValue()) {
            chartReaction.tertiary = data.get("tertiary").getString();
        }
        if (!data.get("quaternary").isNullValue()) {
            chartReaction.quaternary = data.get("quaternary").getString();
        }
        return chartReaction;
    }

    public boolean insertChartReaction(ChartReaction chartReaction, boolean overwrite) {
        if (overwrite) {
            List<Conditional> conditionals = new ArrayList<>();
            conditionals.add(Conditional.createEqualTo("primary", chartReaction.serializePrimary()));
            if (null != chartReaction.secondary) {
                conditionals.add(Conditional.createAnd());
                conditionals.add(Conditional.createEqualTo("secondary", chartReaction.secondary));
            }
            if (null != chartReaction.tertiary) {
                conditionals.add(Conditional.createAnd());
                conditionals.add(Conditional.createEqualTo("tertiary", chartReaction.tertiary));
            }
            if (null != chartReaction.quaternary) {
                conditionals.add(Conditional.createAnd());
                conditionals.add(Conditional.createEqualTo("quaternary", chartReaction.quaternary));
            }

            // 删除符合条件的旧数据
            this.storage.executeDelete(this.chartReactionTable, conditionals.toArray(new Conditional[0]));
        }

        return this.storage.executeInsert(this.chartReactionTable, new StorageField[] {
                new StorageField("primary", chartReaction.serializePrimary()),
                new StorageField("secondary", chartReaction.secondary),
                new StorageField("tertiary", chartReaction.tertiary),
                new StorageField("quaternary", chartReaction.quaternary),
                new StorageField("series_name", chartReaction.seriesName),
                new StorageField("timestamp", chartReaction.timestamp)
        });
    }

    public boolean deleteChartReaction(String primary) {
        return this.storage.executeDelete(this.chartReactionTable, new Conditional[] {
                Conditional.createEqualTo("primary", primary)
        });
    }

    public ChartSeries readLastChartSeries(String seriesName) {
        List<StorageField[]> result = this.storage.executeQuery(this.chartSeriesTable,
                this.chartSeriesFields, new Conditional[] {
                        Conditional.createEqualTo("name", seriesName),
                        Conditional.createOrderBy("timestamp", true),
                        Conditional.createLimit(1)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        ChartSeries chartSeries = new ChartSeries(data.get("name").getString(),
                data.get("desc").getString(), data.get("timestamp").getLong());
        chartSeries.setXAxis(new JSONArray(data.get("x_axis").getString()));
        chartSeries.setData(data.get("type").getString(), new JSONArray(data.get("data").getString()));
        if (!data.get("option").isNullValue()) {
            chartSeries.option = new JSONObject(data.get("option").getString());
        }
        return chartSeries;
    }

    public boolean insertChartSeries(ChartSeries series, boolean overwrite) {
        if (overwrite) {
            this.storage.executeDelete(this.chartSeriesTable, new Conditional[] {
                    Conditional.createEqualTo("name", series.name),
                    Conditional.createOr(),
                    Conditional.createEqualTo("timestamp", series.timestamp)
            });
        }

        return this.storage.executeInsert(this.chartSeriesTable, new StorageField[] {
                new StorageField("name", series.name),
                new StorageField("desc", series.desc),
                new StorageField("timestamp", series.timestamp),
                new StorageField("x_axis", series.getXAxis().toString()),
                new StorageField("type", series.getSeries().type),
                new StorageField("data", series.getSeries().toArray().toString()),
                new StorageField("option", (null != series.option) ? series.option.toString() : null),
        });
    }

    public boolean deleteChartSeries(String seriesName) {
        return this.storage.executeDelete(this.chartSeriesTable, new Conditional[] {
                Conditional.createEqualTo("name", seriesName)
        });
    }

    public int insertAtoms(List<Atom> atomList) {
        if (atomList.isEmpty()) {
            return -1;
        }

        int num = 0;
        for (Atom atom : atomList) {
            boolean success = this.storage.executeInsert(this.chartAtomTable, new StorageField[] {
                    new StorageField("label", atom.label),
                    new StorageField("year", atom.year),
                    new StorageField("month", atom.month),
                    new StorageField("date", atom.date),
                    new StorageField("value", atom.value),
            });

            if (success) {
                ++num;
            }
        }
        return num;
    }

    public int deleteAtoms(List<Long> atomSnList) {
        if (atomSnList.isEmpty()) {
            return -1;
        }

        int num = 0;
        for (long sn : atomSnList) {
            boolean success = this.storage.executeDelete(this.chartAtomTable, new Conditional[] {
                    Conditional.createEqualTo("sn", sn)
            });

            if (success) {
                ++num;
            }
        }
        return num;
    }

    public List<Atom> readAtoms(String label, String year, String month, String date) {
        List<Atom> list = new ArrayList<>();

        ArrayList<Conditional> conditionals = new ArrayList<>();
        conditionals.add(Conditional.createLike("label", label));
        if (null != year) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("year", year));
        }
        if (null != month) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("month", month));
        }
        if (null != date) {
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createEqualTo("date", date));
        }

        List<StorageField[]> result = this.storage.executeQuery(this.chartAtomTable, this.chartAtomFields,
                conditionals.toArray(new Conditional[0]));

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            Atom atom = new Atom(data.get("sn").getLong(), data.get("label").getString(),
                    data.get("year").getString(), data.get("month").getString(), data.get("date").getString(),
                    data.get("value").getInt());
            list.add(atom);
        }

        return list;
    }

    public List<Atom> fullMatching(List<String> labels, String year, String month, String date) {
        List<Atom> atoms = new ArrayList<>();

        LinkedList<Conditional> conditionals = new LinkedList<>();

        LinkedList<Conditional> labelConditionals = new LinkedList<>();
        for (int i = 0; i < 8 && i < labels.size(); ++i) {
            String label = labels.get(i);
            labelConditionals.add(Conditional.createOr());
            labelConditionals.add(Conditional.createLike("label", label));
        }
        labelConditionals.remove(0);
        conditionals.add(Conditional.createBracket(labelConditionals.toArray(new Conditional[0])));

        LinkedList<Conditional> timeConditionals = new LinkedList<>();
        if (null != year) {
            timeConditionals.add(Conditional.createAnd());
            timeConditionals.add(Conditional.createLike("year", year));
        }
        if (null != month) {
            timeConditionals.add(Conditional.createAnd());
            timeConditionals.add(Conditional.createLike("month", month));
        }
        if (null != date) {
            timeConditionals.add(Conditional.createAnd());
            timeConditionals.add(Conditional.createLike("date", date));
        }
        if (!timeConditionals.isEmpty()) {
            timeConditionals.remove(0);
            conditionals.add(Conditional.createAnd());
            conditionals.add(Conditional.createBracket(timeConditionals.toArray(new Conditional[0])));
        }

        List<StorageField[]> result = this.storage.executeQuery(this.chartAtomTable, this.chartAtomFields,
                conditionals.toArray(new Conditional[0]));
        if (result.isEmpty()) {
            return atoms;
        }

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            Atom atom = new Atom(data.get("sn").getLong(), data.get("label").getString(),
                    data.get("year").getString(), data.get("month").getString(), data.get("date").getString(),
                    data.get("value").getInt());
            atoms.add(atom);
        }

        // 排除重复数据
        ArrayList<Atom> atomList = new ArrayList<>();
        for (Atom atom : atoms) {
            if (atomList.contains(atom)) {
                continue;
            }
            atomList.add(atom);
        }

        return atomList;
    }

    public boolean existsAtoms(List<String> labels, String year, String month) {
        LinkedList<Conditional> conditionals = new LinkedList<>();

        LinkedList<Conditional> labelConditionals = new LinkedList<>();
        for (int i = 0; i < 8 && i < labels.size(); ++i) {
            String label = labels.get(i);
            labelConditionals.add(Conditional.createOr());
            labelConditionals.add(Conditional.createLike("label", label));
        }
        labelConditionals.remove(0);
        conditionals.add(Conditional.createBracket(labelConditionals.toArray(new Conditional[0])));

        conditionals.add(Conditional.createAnd());

        LinkedList<Conditional> timeConditionals = new LinkedList<>();
        timeConditionals.add(Conditional.createLike("year", year));
        if (null != month) {
            timeConditionals.add(Conditional.createAnd());
            timeConditionals.add(Conditional.createLike("month", month));
        }
        conditionals.add(Conditional.createBracket(timeConditionals.toArray(new Conditional[0])));

        conditionals.add(Conditional.createLimit(2));

        List<StorageField[]> result = this.storage.executeQuery(this.chartAtomTable, this.chartAtomFields,
                conditionals.toArray(new Conditional[0]));
        return !result.isEmpty();
    }

    /**
     * 读取联系人的提示词。
     * 同时读取 contact_id 为 0 的公共提示词。
     *
     * @param contactId
     * @return
     */
    public List<PromptRecord> readPrompts(long contactId) {
        List<PromptRecord> promptRecords = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(new String[] {
            this.promptWordTable, this.promptWordScopeTable
        }, new StorageField[] {
                new StorageField(this.promptWordTable, "id", LiteralBase.LONG),
                new StorageField(this.promptWordTable, "act", LiteralBase.STRING),
                new StorageField(this.promptWordTable, "prompt", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField(this.promptWordTable, "id", LiteralBase.LONG),
                        new StorageField(this.promptWordScopeTable, "prompt_id", LiteralBase.LONG)),
                Conditional.createAnd(),
                Conditional.createBracket(new Conditional[] {
                        Conditional.createEqualTo(new StorageField(this.promptWordScopeTable,
                                "contact_id", LiteralBase.LONG, contactId)),
                        Conditional.createOr(),
                        Conditional.createEqualTo(new StorageField(this.promptWordScopeTable,
                                "contact_id", LiteralBase.LONG, 0))
                })
        });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            PromptRecord promptRecord = new PromptRecord(data.get("id").getLong(), data.get("act").getString(),
                    data.get("prompt").getString());
            promptRecords.add(promptRecord);
        }

        return promptRecords;
    }

    public PromptRecord readPrompt(String act) {
        List<StorageField[]> result = this.storage.executeQuery(this.promptWordTable, this.promptWordFields,
                new Conditional[] {
                        Conditional.createEqualTo("act", act)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        return new PromptRecord(data.get("id").getLong(), data.get("act").getString(), data.get("prompt").getString());
    }

    public boolean writePrompt(PromptRecord promptRecord, long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.promptWordTable,
                this.promptWordFields, new Conditional[] {
                        Conditional.createEqualTo("id", promptRecord.id)
                });
        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.promptWordTable, new StorageField[] {
                    new StorageField("id", promptRecord.id),
                    new StorageField("act", promptRecord.act),
                    new StorageField("prompt", promptRecord.prompt)
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.promptWordTable, new StorageField[] {
                    new StorageField("act", promptRecord.act),
                    new StorageField("prompt", promptRecord.prompt)
            }, new Conditional[] {
                    Conditional.createEqualTo("id", promptRecord.id)
            });
        }

        result = this.storage.executeQuery(this.promptWordScopeTable, this.promptWordScopeFields,
                new Conditional[] {
                        Conditional.createEqualTo("prompt_id", promptRecord.id),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId)
                });
        if (result.isEmpty()) {
            return this.storage.executeInsert(this.promptWordScopeTable, new StorageField[] {
                    new StorageField("prompt_id", promptRecord.id),
                    new StorageField("contact_id", contactId)
            });
        }
        else {
            return true;
        }
    }

    public void writePrompts(List<PromptRecord> promptRecords, List<Long> contactIds) {
        List<Long> idList = new ArrayList<>();
        for (PromptRecord promptRecord : promptRecords) {
            PromptRecord record = this.readPrompt(promptRecord.act);
            if (null == record) {
                // 插入
                long id = Utils.generateSerialNumber();
                this.storage.executeInsert(this.promptWordTable, new StorageField[]{
                        new StorageField("id", id),
                        new StorageField("act", promptRecord.act),
                        new StorageField("prompt", promptRecord.prompt)
                });
                idList.add(id);
            }
            else {
                // 更新
                this.storage.executeUpdate(this.promptWordTable, new StorageField[] {
                        new StorageField("prompt", promptRecord.prompt)
                }, new Conditional[] {
                            Conditional.createEqualTo("id", record.id)
                });
                idList.add(record.id);
            }
        }

        for (long contactId : contactIds) {
            for (long id : idList) {
                List<StorageField[]> result = this.storage.executeQuery(this.promptWordScopeTable,
                        this.promptWordScopeFields,
                        new Conditional[] {
                                Conditional.createEqualTo("prompt_id", id),
                                Conditional.createAnd(),
                                Conditional.createEqualTo("contact_id", contactId)
                        });

                if (result.isEmpty()) {
                    // 插入新数据
                    this.storage.executeInsert(this.promptWordScopeTable, new StorageField[] {
                            new StorageField("prompt_id", id),
                            new StorageField("contact_id", contactId)
                    });
                }
            }
        }
    }

    public void deletePrompts(List<Long> promptIdList) {
        for (long id : promptIdList) {
            this.storage.executeDelete(this.promptWordTable, new Conditional[] {
                    Conditional.createEqualTo("id", id)
            });

            this.storage.executeDelete(this.promptWordScopeTable, new Conditional[] {
                    Conditional.createEqualTo("prompt_id", id)
            });
        }
    }

    public int deletePrompt(long contactId, String act) {
        int count = 0;

        List<StorageField[]> result = this.storage.executeQuery(new String[] {
                this.promptWordTable, this.promptWordScopeTable
        }, new StorageField[] {
                new StorageField(this.promptWordTable, "id", LiteralBase.LONG),
                new StorageField(this.promptWordScopeTable, "sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo(new StorageField(this.promptWordTable, "id", LiteralBase.LONG),
                        new StorageField(this.promptWordScopeTable, "prompt_id", LiteralBase.LONG)),
                Conditional.createAnd(),
                Conditional.createBracket(new Conditional[] {
                        Conditional.createEqualTo(new StorageField(this.promptWordScopeTable,
                                "contact_id", LiteralBase.LONG, contactId)),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField(this.promptWordTable,
                                "act", LiteralBase.STRING, act))
                })
        });

        if (result.isEmpty()) {
            return count;
        }

        for (StorageField[] fields : result) {
            long promptId = fields[0].getLong();
            long sn = fields[1].getLong();
            this.storage.executeDelete(this.promptWordTable, new Conditional[] {
                    Conditional.createEqualTo("id", promptId)
            });
            this.storage.executeDelete(this.promptWordScopeTable, new Conditional[] {
                    Conditional.createEqualTo("sn", sn)
            });

            ++count;
        }

        return count;
    }

    public synchronized Usage updateUsage(long contactId, String model,
                                          long incrementalCompletion, long incrementalPrompt) {
        Usage usage = this.readUsage(contactId, model);
        if (null == usage) {
            this.storage.executeInsert(this.usageTable, new StorageField[] {
                    new StorageField("contact_id", contactId),
                    new StorageField("model", model),
                    new StorageField("completion_tokens", incrementalCompletion),
                    new StorageField("prompt_tokens", incrementalPrompt),
                    new StorageField("timestamp", System.currentTimeMillis())
            });
            usage = new Usage(model, incrementalCompletion, incrementalPrompt,
                    incrementalCompletion + incrementalPrompt);
        }
        else {
            this.storage.executeUpdate(this.usageTable, new StorageField[] {
                    new StorageField("completion_tokens", incrementalCompletion + usage.completionTokens),
                    new StorageField("prompt_tokens", incrementalPrompt + usage.promptTokens),
                    new StorageField("timestamp", System.currentTimeMillis())
            }, new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("model", model)
            });
            usage = new Usage(model, incrementalCompletion + usage.completionTokens,
                    incrementalPrompt + usage.promptTokens,
                    incrementalCompletion + incrementalPrompt + usage.totalTokens);
        }
        return usage;
    }

    public Usage readUsage(long contactId, String model) {
        List<StorageField[]> result = this.storage.executeQuery(this.usageTable, this.usageFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("model", model)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        long completionTokens = data.get("completion_tokens").getLong();
        long promptTokens = data.get("prompt_tokens").getLong();
        return new Usage(model, completionTokens, promptTokens, completionTokens + promptTokens);
    }

    private void resetDefaultConfig() {
        // 支持中英双语的对话语言模型，具有 62 亿参数。针对中文问答和对话进行了优化。
        // 经过约 1T 标识符的中英双语训练，辅以监督微调、反馈自助、人类反馈强化学习等技术的优化。
        JSONObject parameter = new JSONObject();
        parameter.put("unit", "Chat");
        ModelConfig baizeNLG = new ModelConfig("Baize", "Baize",
                "适合大多数场景的通用模型",
                "http://127.0.0.1:7010/aigc/chat/", parameter);

        // 支持中英双语的功能型对话语言大模型。以轻量化实现高质量效果的模型。在1000亿 Token 中文语料上预训练，累计学习1.5万亿中文 Token，
        // 并且在数百种任务上进行 Prompt 任务式训练。针对理解类任务，如分类、情感分析、抽取等，可以自定义标签体系；针对多种生成任务，
        // 可以进行采样自由生成。
        parameter = new JSONObject();
        parameter.put("unit", "ChatT5G");
        ModelConfig baizeX = new ModelConfig("BaizeX", "BaizeX",
                "适合一般场景且速度较快的通用模型",
                "http://127.0.0.1:7010/aigc/chat/", parameter);

        // 支持中英双语和多种插件的开源对话语言模型。模型具有 160 亿参数。在约七千亿中英文以及代码单词上预训练得到，后续经过对话指令微调、
        // 插件增强学习和人类偏好训练具备多轮对话能力及使用多种插件的能力。
        parameter = new JSONObject();
        ModelConfig baizeNEXT = new ModelConfig("BaizeNEXT", "BaizeNEXT",
                "适合谨慎问答场景的大模型（测试版）",
                "http://127.0.0.1:7010/aigc/conversation/", parameter);

        // 重置列表
        List<StorageField[]> result = this.storage.executeQuery(this.appConfigTable, new StorageField[] {
                new StorageField("value", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("item", ITEM_NAME_MODELS)
        });
        if (!result.isEmpty()) {
            this.storage.executeDelete(this.appConfigTable, new Conditional[] {
                    Conditional.createEqualTo("item", ITEM_NAME_MODELS)
            });
        }
        JSONArray models = new JSONArray();
        models.put(baizeNLG.getName());
        models.put(baizeX.getName());
        models.put(baizeNEXT.getName());
        this.storage.executeInsert(this.appConfigTable, new StorageField[] {
                new StorageField("item", ITEM_NAME_MODELS),
                new StorageField("value", models.toString()),
                new StorageField("comment", "Model List"),
                new StorageField("modified", System.currentTimeMillis())
        });

        // 字段 value 为模型配置的 JSON string

        // Baize
        result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                Conditional.createEqualTo("item", baizeNLG.getName())
        });
        if (!result.isEmpty()) {
            this.storage.executeDelete(this.appConfigTable, new Conditional[] {
                    Conditional.createEqualTo("item", baizeNLG.getName())
            });
        }
        this.storage.executeInsert(this.appConfigTable, new StorageField[] {
                new StorageField("item", baizeNLG.getName()),
                new StorageField("value", baizeNLG.toJSON().toString()),
                new StorageField("comment", "自然语言生成模型"),
                new StorageField("modified", System.currentTimeMillis())
        });

        // BaizeX
        result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                Conditional.createEqualTo("item", baizeX.getName())
        });
        if (!result.isEmpty()) {
            this.storage.executeDelete(this.appConfigTable, new Conditional[] {
                    Conditional.createEqualTo("item", baizeX.getName())
            });
        }
        this.storage.executeInsert(this.appConfigTable, new StorageField[] {
                new StorageField("item", baizeX.getName()),
                new StorageField("value", baizeX.toJSON().toString()),
                new StorageField("comment", "效能较好的自然语言生成模型"),
                new StorageField("modified", System.currentTimeMillis())
        });

        // BaizeNEXT
        result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                Conditional.createEqualTo("item", baizeNEXT.getName())
        });
        if (!result.isEmpty()) {
            this.storage.executeDelete(this.appConfigTable, new Conditional[] {
                    Conditional.createEqualTo("item", baizeNEXT.getName())
            });
        }
        this.storage.executeInsert(this.appConfigTable, new StorageField[] {
                new StorageField("item", baizeNEXT.getName()),
                new StorageField("value", baizeNEXT.toJSON().toString()),
                new StorageField("comment", "支持下游任务的大语言生成模型"),
                new StorageField("modified", System.currentTimeMillis())
        });
    }
}
