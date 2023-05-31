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
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.Notification;
import cube.common.Storagable;
import cube.common.entity.AIGCChatHistory;
import cube.common.entity.KnowledgeDoc;
import cube.common.entity.KnowledgeProfile;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AIGC 存储器。
 */
public class AIGCStorage implements Storagable {

    public final static String ITEM_NAME_MODELS = "models";

    private final String appConfigTable = "aigc_app_config";

    private final String appInvitationTable = "aigc_app_invitation";

    private final String appNotificationTable = "aigc_app_notification";

    private final String queryAnswerTable = "aigc_query_answer";

    private final String knowledgeProfileTable = "aigc_knowledge_profile";

    private final String knowledgeDocTable = "aigc_knowledge_doc";

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
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("max_size", LiteralBase.LONG, new Constraint[] {
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
            String modelName = models.getString(i);
            result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                    Conditional.createEqualTo("item", modelName)
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
                data.get("state").getInt(), data.get("max_size").getInt());
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
                    data.get("activated").getInt() == 1);
            list.add(doc);
        }

        return list;
    }

    private void resetDefaultConfig() {
        // 支持中英双语的对话语言模型，具有 62 亿参数。针对中文问答和对话进行了优化。经过约 1T 标识符的中英双语训练，辅以监督微调、反馈自助、人类反馈强化学习等技术的优化。
        JSONObject parameter = new JSONObject();
        parameter.put("unit", "Chat");
        ModelConfig baizeNLG = new ModelConfig("BaizeNLG",
                "适合大多数场景的通用模型",
                "http://36.133.49.214:7010/aigc/chat/", parameter);

        // 支持中英双语的功能型对话语言大模型。以轻量化实现高质量效果的模型。在1000亿 Token 中文语料上预训练，累计学习1.5万亿中文 Token，并且在数百种任务上进行 Prompt 任务式训练。针对理解类任务，如分类、情感分析、抽取等，可以自定义标签体系；针对多种生成任务，可以进行采样自由生成。
        parameter = new JSONObject();
        parameter.put("unit", "ChatT5G");
        ModelConfig baizeX = new ModelConfig("BaizeX",
                "适合一般场景且速度较快的通用模型",
                "http://36.133.49.214:7010/aigc/chat/", parameter);

        // 支持中英双语和多种插件的开源对话语言模型。模型具有 160 亿参数。在约七千亿中英文以及代码单词上预训练得到，后续经过对话指令微调、插件增强学习和人类偏好训练具备多轮对话能力及使用多种插件的能力。
        parameter = new JSONObject();
        ModelConfig baizeNEXT = new ModelConfig("BaizeNEXT",
                "适合谨慎问答场景的大模型（测试版）",
                "http://36.133.49.214:7010/aigc/conversation/", parameter);

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

        // BaizeNLG
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
