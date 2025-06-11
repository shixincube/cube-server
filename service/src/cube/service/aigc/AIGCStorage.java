/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.ModelConfig;
import cube.aigc.app.Notification;
import cube.aigc.atom.Atom;
import cube.aigc.psychology.composition.Emotion;
import cube.common.Storagable;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.EmojiFilter;
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

    private final String knowledgeFrameTable = "aigc_knowledge_frame";

    private final String knowledgeDocTable = "aigc_knowledge_doc";

    private final String knowledgeSegmentTable = "aigc_knowledge_segment";

    private final String knowledgeArticleTable = "aigc_knowledge_article";

    private final String knowledgeParaphraseTable = "aigc_knowledge_paraphrase";

    private final String chartTable = "aigc_chart";

    private final String chartAtomTable = "aigc_chart_atom";

    private final String promptWordTable = "aigc_prompt_word";

    private final String promptWordScopeTable = "aigc_prompt_word_scope";

    /**
     * 用量表。
     */
    private final String usageTable = "aigc_usage";

    /**
     * 联系人偏好。
     */
    private final String contactPreferenceTable = "aigc_contact_preference";

    private final String emotionTable = "aigc_speech_emotion";

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
            }),
            new StorageField("remark", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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
            new StorageField("channel", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("unit", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_cid", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("query_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("query_files", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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
            new StorageField("answer_files", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("thought", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("context", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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

    private final StorageField[] knowledgeFrameFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("base", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("display_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("category", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("unit_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("store_size", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
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
            new StorageField("base", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("file_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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

    private final StorageField[] knowledgeSegmentFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("doc_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("uuid", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("category", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] knowledgeArticleFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("base", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
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
            new StorageField("summarization", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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
            }),
            new StorageField("scope", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("activated", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("segments", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
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

    private final StorageField[] chartFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
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
            new StorageField("year", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("month", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("date", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("chart", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
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
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("act", LiteralBase.STRING, new Constraint[] {
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
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] usageFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL,
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

    private final StorageField[] contactPreferenceFields = new StorageField[] {
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("models", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] emotionFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTO_INCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("emotion", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 情绪数据来源：语音、文本等
            new StorageField("source", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("source_data", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
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

        if (!this.storage.exist(this.knowledgeFrameTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeFrameTable, this.knowledgeFrameFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeFrameTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeDocTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeDocTable, this.knowledgeDocFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeDocTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.knowledgeSegmentTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeSegmentTable, this.knowledgeSegmentFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeSegmentTable + "' successfully");

                String sql = "alter table `" + this.knowledgeSegmentTable + "` convert to character set utf8mb4 collate utf8mb4_unicode_ci";
                this.storage.execute(sql);
            }
        }

        if (!this.storage.exist(this.knowledgeArticleTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeArticleTable, this.knowledgeArticleFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeArticleTable + "' successfully");

                String sql = "alter table `" + this.knowledgeArticleTable + "` convert to character set utf8mb4 collate utf8mb4_unicode_ci";
                this.storage.execute(sql);
            }
        }

        if (!this.storage.exist(this.knowledgeParaphraseTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.knowledgeParaphraseTable, this.knowledgeParaphraseFields)) {
                Logger.i(this.getClass(), "Created table '" + this.knowledgeParaphraseTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.chartTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.chartTable, this.chartFields)) {
                Logger.i(this.getClass(), "Created table '" + this.chartTable + "' successfully");
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

        if (!this.storage.exist(this.contactPreferenceTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.contactPreferenceTable, this.contactPreferenceFields)) {
                Logger.i(this.getClass(), "Created table '" + this.contactPreferenceTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.emotionTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.emotionTable, this.emotionFields)) {
                Logger.i(this.getClass(), "Created table '" + this.emotionTable + "' successfully");
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
            String model = models.getString(i);
            result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                    Conditional.createEqualTo("item", model)
            });

            for (StorageField[] fields : result) {
                Map<String, StorageField> data = StorageFields.get(fields);
                JSONObject value = new JSONObject(data.get("value").getString());
                try {
                    ModelConfig config = new ModelConfig(model, value);
                    list.add(config);
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#getModelConfigs - ModelConfig error: " + e.getMessage());
                }
            }
        }

        return list;
    }

    public List<ModelConfig> getModelConfigs(List<String> modelNames) {
        List<ModelConfig> list = new ArrayList<>();

        for (String modelName : modelNames) {
            List<StorageField[]> result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                    Conditional.createEqualTo("item", modelName.trim())
            });

            for (StorageField[] fields : result) {
                Map<String, StorageField> data = StorageFields.get(fields);
                JSONObject value = new JSONObject(data.get("value").getString());
                try {
                    ModelConfig config = new ModelConfig(modelName, value);
                    list.add(config);
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#getModelConfigs - ModelConfig error: " + e.getMessage());
                }
            }
        }

        return list;
    }

    public JSONObject getAppVersion() {
        List<StorageField[]> result = this.storage.executeQuery(this.appConfigTable, this.appConfigFields, new Conditional[] {
                Conditional.createEqualTo("item", "AppVersion")
        });
        if (result.isEmpty()) {
            return null;
        }

        StorageField[] fields = result.get(0);
        Map<String, StorageField> data = StorageFields.get(fields);
        return new JSONObject(data.get("value").getString());
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

    public List<AppEvent> readAppEvents(long contactId, String eventName, int limit) {
        List<AppEvent> list = new ArrayList<>();
        List<StorageField[]> result = this.storage.executeQuery(this.appEventTable, this.appEventFields,
            new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("event", eventName),
                    Conditional.createOrderBy("timestamp", true),
                    Conditional.createLimit(limit)
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

    public List<AIGCChatHistory> readHistoriesByContactId(long contactId, String domain, long startTime, long endTime) {
        List<AIGCChatHistory> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.queryAnswerTable,
                this.queryAnswerFields, new Conditional[] {
                        Conditional.createEqualTo("query_cid", contactId),
                        Conditional.createAnd(),
                        Conditional.createBracket(new Conditional[] {
                                Conditional.createIsNull("query_domain"),
                                Conditional.createOr(),
                                Conditional.createEqualTo("query_domain", domain)
                        }),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("query_time", startTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("query_time", endTime))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            AIGCChatHistory history = new AIGCChatHistory(data.get("id").getLong(), data.get("sn").getLong(),
                    data.get("channel").getString(), domain);
            history.unit = data.get("unit").getString();
            history.queryContactId = data.get("query_cid").getLong();
            history.queryTime = data.get("query_time").getLong();
            history.queryContent = data.get("query_content").getString();
            history.answerContactId = data.get("answer_cid").getLong();
            history.answerTime = data.get("answer_time").getLong();
            history.answerContent = data.get("answer_content").getString();
            history.feedback = data.get("feedback").getInt();
            history.contextId = data.get("context_id").getLong();

            try {
                if (!data.get("query_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("query_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.queryFileLabels = files;
                }

                if (!data.get("answer_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("answer_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.answerFileLabels = files;
                }

                if (!data.get("thought").isNullValue()) {
                    history.thought = data.get("thought").getString();
                }

                if (!data.get("context").isNullValue()) {
                    String jsonString = data.get("context").getString();
                    if (jsonString.length() > 3) {
                        ComplexContext context = new ComplexContext(new JSONObject(jsonString));
                        history.context = context;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#readHistoriesByContactId", e);
            }

            list.add(history);
        }

        return list;
    }

    public List<AIGCChatHistory> readHistoriesByFeedback(int feedback, String domain, long startTime, long endTime) {
        List<AIGCChatHistory> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.queryAnswerTable,
                this.queryAnswerFields, new Conditional[] {
                        Conditional.createEqualTo("feedback", feedback),
                        Conditional.createAnd(),
                        Conditional.createBracket(new Conditional[] {
                                Conditional.createIsNull("query_domain"),
                                Conditional.createOr(),
                                Conditional.createEqualTo("query_domain", domain)
                        }),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("query_time", startTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("query_time", endTime))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            AIGCChatHistory history = new AIGCChatHistory(data.get("id").getLong(), data.get("sn").getLong(),
                    data.get("channel").getString(), domain);
            history.unit = data.get("unit").getString();
            history.queryContactId = data.get("query_cid").getLong();
            history.queryTime = data.get("query_time").getLong();
            history.queryContent = data.get("query_content").getString();
            history.answerContactId = data.get("answer_cid").getLong();
            history.answerTime = data.get("answer_time").getLong();
            history.answerContent = data.get("answer_content").getString();
            history.feedback = data.get("feedback").getInt();
            history.contextId = data.get("context_id").getLong();

            try {
                if (!data.get("query_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("query_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.queryFileLabels = files;
                }

                if (!data.get("answer_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("answer_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.answerFileLabels = files;
                }

                if (!data.get("thought").isNullValue()) {
                    history.thought = data.get("thought").getString();
                }

                if (!data.get("context").isNullValue()) {
                    String jsonString = data.get("context").getString();
                    if (jsonString.length() > 3) {
                        ComplexContext context = new ComplexContext(new JSONObject(jsonString));
                        history.context = context;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#readHistoriesByFeedback", e);
            }

            list.add(history);
        }

        return list;
    }

    public List<AIGCChatHistory> readHistoriesByChannel(String channelCode, long startTime, long endTime) {
        List<AIGCChatHistory> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.queryAnswerTable,
                this.queryAnswerFields, new Conditional[] {
                        Conditional.createEqualTo("channel", channelCode),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("query_time", startTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("query_time", endTime))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            AIGCChatHistory history = new AIGCChatHistory(data.get("id").getLong(), data.get("sn").getLong(),
                    data.get("channel").getString(),
                    data.get("query_domain").isNullValue() ? null : data.get("query_domain").getString());
            history.unit = data.get("unit").getString();
            history.queryContactId = data.get("query_cid").getLong();
            history.queryTime = data.get("query_time").getLong();
            history.queryContent = data.get("query_content").getString();
            history.answerContactId = data.get("answer_cid").getLong();
            history.answerTime = data.get("answer_time").getLong();
            history.answerContent = data.get("answer_content").getString();
            history.feedback = data.get("feedback").getInt();
            history.contextId = data.get("context_id").getLong();

            try {
                if (!data.get("query_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("query_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.queryFileLabels = files;
                }

                if (!data.get("answer_files").isNullValue()) {
                    JSONArray array = new JSONArray(data.get("answer_files").getString());
                    List<FileLabel> files = new ArrayList<>();
                    for (int i = 0; i < array.length(); ++i) {
                        FileLabel file = new FileLabel(array.getJSONObject(i));
                        files.add(file);
                    }
                    history.answerFileLabels = files;
                }

                if (!data.get("thought").isNullValue()) {
                    history.thought = data.get("thought").getString();
                }

                if (!data.get("context").isNullValue()) {
                    String jsonString = data.get("context").getString();
                    if (jsonString.length() > 3) {
                        ComplexContext context = new ComplexContext(new JSONObject(jsonString));
                        history.context = context;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#readHistoriesByChannel", e);
            }

            list.add(history);
        }

        return list;
    }

    public void writeHistory(AIGCChatHistory history) {
        JSONArray queryFiles = null;
        if (null != history.queryFileLabels) {
            queryFiles = new JSONArray();
            for (FileLabel fileLabel : history.queryFileLabels) {
                queryFiles.put(fileLabel.toJSON());
            }
        }

        JSONArray answerFiles = null;
        if (null != history.answerFileLabels) {
            answerFiles = new JSONArray();
            for (FileLabel fileLabel : history.answerFileLabels) {
                answerFiles.put(fileLabel.toJSON());
            }
        }

        this.storage.executeInsert(this.queryAnswerTable, new StorageField[] {
                new StorageField("sn", history.sn),
                new StorageField("channel", history.channelCode),
                new StorageField("unit", history.unit),
                new StorageField("query_cid", history.queryContactId),
                new StorageField("query_domain", history.getDomain().getName()),
                new StorageField("query_time", history.queryTime),
                new StorageField("query_content", EmojiFilter.filterEmoji(history.queryContent)),
                (null != queryFiles) ? new StorageField("query_files", queryFiles.toString()) :
                        new StorageField("query_files", LiteralBase.STRING),
                new StorageField("answer_cid", history.answerContactId),
                new StorageField("answer_time", history.answerTime),
                new StorageField("answer_content", EmojiFilter.filterEmoji(history.answerContent)),
                (null != answerFiles) ? new StorageField("answer_files", answerFiles.toString()) :
                        new StorageField("answer_files", LiteralBase.STRING),
                (null != history.thought) ? new StorageField("thought", history.thought) :
                        new StorageField("thought", LiteralBase.STRING),
                (null != history.context) ? new StorageField("context", history.context.toJSON().toString()) :
                        new StorageField("context", LiteralBase.STRING),
                new StorageField("feedback", history.feedback),
                new StorageField("context_id", history.contextId)
        });
    }

    public boolean updateHistoryFeedback(long sn, int feedback) {
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

    public KnowledgeProfile updateKnowledgeProfile(long contactId, String domain, int state, long maxSize,
                                                   KnowledgeScope scope) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeProfileTable, this.knowledgeProfileFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        if (result.isEmpty()) {
            this.storage.executeInsert(this.knowledgeProfileTable, new StorageField[] {
                    new StorageField("contact_id", contactId),
                    new StorageField("domain", domain),
                    new StorageField("state", state),
                    new StorageField("max_size", maxSize),
                    new StorageField("scope", scope.name)
            });
            return this.readKnowledgeProfile(contactId);
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        long id = data.get("id").getLong();

        this.storage.executeUpdate(this.knowledgeProfileTable, new StorageField[] {
                new StorageField("state", state),
                new StorageField("max_size", maxSize),
                new StorageField("scope", scope.name)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId)
        });

        return new KnowledgeProfile(id, contactId, domain, state, maxSize, scope);
    }

    /**
     * 读取联系人的知识库信息。
     *
     * @param contactId
     * @return
     */
    public List<KnowledgeBaseInfo> readKnowledgeBaseInfo(long contactId) {
        List<KnowledgeBaseInfo> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeFrameTable, this.knowledgeFrameFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeBaseInfo info = new KnowledgeBaseInfo(data.get("contact_id").getLong(),
                    data.get("base").getString(), data.get("display_name").getString(),
                    data.get("category").isNullValue() ? null : data.get("category").getString(),
                    data.get("unit_id").getLong(), data.get("store_size").getLong(), data.get("timestamp").getLong());
            list.add(info);
        }

        return list;
    }

    public KnowledgeBaseInfo readKnowledgeBaseInfo(long contactId, String baseName) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeFrameTable, this.knowledgeFrameFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        KnowledgeBaseInfo info = new KnowledgeBaseInfo(data.get("contact_id").getLong(),
                data.get("base").getString(), data.get("display_name").getString(),
                data.get("category").isNullValue() ? null : data.get("category").getString(),
                data.get("unit_id").getLong(), data.get("store_size").getLong(), data.get("timestamp").getLong());
        return info;
    }

    public boolean deleteKnowledgeBaseInfo(long contactId, String baseName) {
        return this.storage.executeDelete(this.knowledgeFrameTable, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("base", baseName)
        });
    }

    public boolean updateKnowledgeBaseInfo(long contactId, KnowledgeBaseInfo info) {
        return this.storage.executeUpdate(this.knowledgeFrameTable, new StorageField[] {
                new StorageField("display_name", info.displayName),
                new StorageField("category", info.category),
                new StorageField("timestamp", info.timestamp)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("base", info.name)
        });
    }

    public List<KnowledgeBaseInfo> readKnowledgeBaseInfoByCategory(long contactId, String category) {
        List<KnowledgeBaseInfo> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeFrameTable, this.knowledgeFrameFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("category", category)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeBaseInfo info = new KnowledgeBaseInfo(data.get("contact_id").getLong(),
                    data.get("base").getString(), data.get("display_name").getString(),
                    data.get("category").isNullValue() ? null : data.get("category").getString(),
                    data.get("unit_id").getLong(), data.get("store_size").getLong(), data.get("timestamp").getLong());
            list.add(info);
        }

        return list;
    }

    public boolean writeKnowledgeBaseInfo(KnowledgeBaseInfo info) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeFrameTable,
                this.knowledgeFrameFields, new Conditional[] {
                        Conditional.createEqualTo("contact_id", info.contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", info.name)
                });
        if (result.isEmpty()) {
            return this.storage.executeInsert(this.knowledgeFrameTable, new StorageField[] {
                    new StorageField("contact_id", info.contactId),
                    new StorageField("base", info.name),
                    new StorageField("display_name", info.displayName),
                    new StorageField("category", info.category),
                    new StorageField("unit_id", info.unitId),
                    new StorageField("store_size", info.storeSize),
                    new StorageField("timestamp", info.timestamp)
            });
        }
        else {
            return this.storage.executeUpdate(this.knowledgeFrameTable, new StorageField[] {
                    new StorageField("display_name", info.displayName),
                    new StorageField("unit_id", info.unitId),
                    new StorageField("store_size", info.storeSize),
                    new StorageField("timestamp", info.timestamp),
                    new StorageField("category", info.category)
            }, new Conditional[] {
                    Conditional.createEqualTo("contact_id", info.contactId),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("base", info.name)
            });
        }
    }

    public List<KnowledgeDocument> readKnowledgeDocList(String domain, String baseName) {
        List<KnowledgeDocument> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeDocument doc = new KnowledgeDocument(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("file_code").getString(), data.get("base").getString(),
                    data.get("file_name").isNullValue() ? null : data.get("file_name").getString(),
                    data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                    KnowledgeScope.parse(data.get("scope").getString()));
            list.add(doc);
        }

        return list;
    }

    public List<KnowledgeDocument> readKnowledgeDocList(String domain, long contactId, String baseName) {
        List<KnowledgeDocument> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeDocument doc = new KnowledgeDocument(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("file_code").getString(), data.get("base").getString(),
                    data.get("file_name").isNullValue() ? null : data.get("file_name").getString(),
                    data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                    KnowledgeScope.parse(data.get("scope").getString()));
            list.add(doc);
        }

        return list;
    }

    public KnowledgeDocument readKnowledgeDoc(String baseName, String fileCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, this.knowledgeDocFields,
                new Conditional[] {
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("file_code", fileCode)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        KnowledgeDocument doc = new KnowledgeDocument(data.get("id").getLong(), data.get("domain").getString(),
                data.get("contact_id").getLong(), data.get("file_code").getString(), data.get("base").getString(),
                data.get("file_name").isNullValue() ? null : data.get("file_name").getString(),
                data.get("activated").getInt() == 1, data.get("num_segments").getInt(),
                KnowledgeScope.parse(data.get("scope").getString()));
        return doc;
    }

    public void writeKnowledgeDoc(KnowledgeDocument doc) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeDocTable, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("base", doc.baseName),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_code", doc.fileCode)
        });

        if (!result.isEmpty()) {
            this.storage.executeDelete(this.knowledgeDocTable, new Conditional[] {
                    Conditional.createEqualTo("base", doc.baseName),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("file_code", doc.fileCode)
            });
        }

        this.storage.executeInsert(this.knowledgeDocTable, new StorageField[] {
                new StorageField("id", doc.getId().longValue()),
                new StorageField("domain", doc.getDomain().getName()),
                new StorageField("contact_id", doc.contactId),
                new StorageField("file_code", doc.fileCode),
                new StorageField("base", doc.baseName),
                new StorageField("file_name", doc.fileName),
                new StorageField("activated", doc.activated ? 1 : 0),
                new StorageField("num_segments", doc.numSegments),
                new StorageField("scope", doc.scope.name)
        });
    }

    public boolean updateKnowledgeDoc(KnowledgeDocument doc) {
        return this.storage.executeUpdate(this.knowledgeDocTable, new StorageField[] {
                new StorageField("activated", doc.activated ? 1 : 0),
                new StorageField("num_segments", doc.numSegments),
                new StorageField("file_name", doc.fileName)
        }, new Conditional[] {
                Conditional.createEqualTo("base", doc.baseName),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_code", doc.fileCode)
        });
    }

    public boolean deleteKnowledgeDoc(String baseName, String fileCode) {
        return this.storage.executeDelete(this.knowledgeDocTable, new Conditional[] {
                Conditional.createEqualTo("base", baseName),
                Conditional.createAnd(),
                Conditional.createEqualTo("file_code", fileCode)
        });
    }

    public List<String[]> matchKnowledgeDocWithFileName(String baseName, List<String> words) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT `file_code`,`file_name` FROM `").append(this.knowledgeDocTable).append("`");
        sql.append(" WHERE `base`='").append(baseName).append("' AND `file_name` REGEXP '");
        for (String word : words) {
            sql.append(word).append("|");
        }
        sql.delete(sql.length() - 1, sql.length());
        sql.append("'");

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return null;
        }

        List<String[]> codeAndNameList = new ArrayList<>();
        for (StorageField[] fields : result) {
            String[] data = new String[] {
                    fields[0].getString(),
                    fields[1].getString() };
            codeAndNameList.add(data);
        }
        return codeAndNameList;
    }

    public List<KnowledgeSegment> readKnowledgeSegments(long docId, int startIndex, int endIndex) {
        List<KnowledgeSegment> list = new ArrayList<>();
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeSegmentTable,
                this.knowledgeSegmentFields, new Conditional[] {
                        Conditional.createEqualTo("doc_id", docId),
                        Conditional.createLimit(startIndex, endIndex - startIndex + 1)
                });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeSegment segment = new KnowledgeSegment(data.get("sn").getLong(),
                    data.get("doc_id").getLong(), data.get("uuid").getString(), data.get("content").getString(),
                    data.get("category").isNullValue() ? null : data.get("category").getString());
            list.add(segment);
        }
        return list;
    }

    public int countKnowledgeSegments(long docId) {
        String sql = "SELECT COUNT(sn) FROM " + this.knowledgeSegmentTable +
                " WHERE doc_id=" + docId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return result.get(0)[0].getInt();
    }

    public void writeKnowledgeSegments(List<KnowledgeSegment> segments) {
        for (KnowledgeSegment segment : segments) {
            this.storage.executeInsert(this.knowledgeSegmentTable, new StorageField[] {
                    new StorageField("doc_id", segment.docId),
                    new StorageField("uuid", segment.uuid),
                    new StorageField("content", segment.content),
                    new StorageField("category", segment.category)
            });
        }
    }

    public boolean deleteKnowledgeSegments(long docId) {
        return this.storage.executeDelete(this.knowledgeSegmentTable, new Conditional[] {
                Conditional.createEqualTo("doc_id", docId)
        });
    }

    public boolean writeKnowledgeArticle(KnowledgeArticle article) {
        return this.storage.executeInsert(this.knowledgeArticleTable, new StorageField[] {
                new StorageField("id", article.getId().longValue()),
                new StorageField("domain", article.getDomain().getName()),
                new StorageField("contact_id", article.contactId),
                new StorageField("base", article.baseName),
                new StorageField("category", article.category),
                new StorageField("title", article.title),
                new StorageField("content", article.content),
                new StorageField("summarization", article.summarization),
                new StorageField("author", article.author),
                new StorageField("year", article.year),
                new StorageField("month", article.month),
                new StorageField("date", article.date),
                new StorageField("timestamp", article.getTimestamp()),
                new StorageField("scope", article.scope.name),
                new StorageField("activated", article.activated ? 1 : 0),
                new StorageField("segments", article.numSegments)
        });
    }

    public boolean updateKnowledgeArticleSummarization(long articleId, String summarization) {
        return this.storage.executeUpdate(this.knowledgeArticleTable, new StorageField[] {
                new StorageField("summarization", summarization)
        }, new Conditional[] {
                Conditional.createEqualTo("id", articleId)
        });
    }

    public boolean updateKnowledgeArticleActivated(long articleId, boolean activated, int numSegments) {
        return this.storage.executeUpdate(this.knowledgeArticleTable, new StorageField[] {
                new StorageField("activated", activated ? 1 : 0),
                new StorageField("segments", numSegments)
        }, new Conditional[] {
                Conditional.createEqualTo("id", articleId)
        });
    }

    public KnowledgeArticle updateKnowledgeArticle(KnowledgeArticle article) {
        if (this.storage.executeUpdate(this.knowledgeArticleTable, new StorageField[] {
                new StorageField("base", article.baseName),
                new StorageField("title", article.title),
                new StorageField("author", article.author),
                new StorageField("category", article.category),
                new StorageField("content", article.content),
                new StorageField("summarization", article.summarization),
                new StorageField("year", article.year),
                new StorageField("month", article.month),
                new StorageField("date", article.date),
                new StorageField("timestamp", article.getTimestamp()),
                new StorageField("scope", article.scope.name),
                new StorageField("activated", article.activated ? 1 : 0),
                new StorageField("segments", article.numSegments)
        }, new Conditional[] {
                Conditional.createEqualTo("id", article.getId().longValue())
        })) {
            return article;
        }
        else {
            return null;
        }
    }

    public KnowledgeArticle readKnowledgeArticle(long articleId) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("id", articleId)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                data.get("contact_id").getLong(), data.get("base").getString(),
                data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                data.get("author").getString(),
                data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                data.get("activated").getInt() == 1, data.get("segments").getInt());
        return article;
    }

    public List<KnowledgeArticle> readKnowledgeArticles(String domain, long contactId, String baseName) {
        List<KnowledgeArticle> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createOrderBy("timestamp", true)
                });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
            list.add(article);
        }

        return list;
    }

    public List<KnowledgeArticle> readKnowledgeArticlesByTitle(String domain, long contactId, String baseName, String title) {
        List<KnowledgeArticle> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("title", title),
                        Conditional.createOrderBy("timestamp", true)
        });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
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
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
            list.add(article);
        }

        return list;
    }

    public List<KnowledgeArticle> readKnowledgeArticles(String baseName, String category, long startTime, long endTime) {
        List<KnowledgeArticle> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("category", category),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", startTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("timestamp", endTime))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
            list.add(article);
        }

        return list;
    }

    public List<KnowledgeArticle> readKnowledgeArticles(long contactId, String baseName, long startTime, long endTime) {
        List<KnowledgeArticle> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", startTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("timestamp", endTime))
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
            list.add(article);
        }

        return list;
    }

    /**
     * 使用关键字匹配文章标题和摘要。
     *
     * @param domain
     * @param contactId
     * @param baseName
     * @param keyword
     * @return
     */
    public List<KnowledgeArticle> matchKnowledgeArticles(String domain, long contactId, String baseName, String keyword) {
        List<StorageField[]> result = this.storage.executeQuery(this.knowledgeArticleTable, this.knowledgeArticleFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domain),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("base", baseName),
                        Conditional.createAnd(),
                        Conditional.createBracket(new Conditional[] {
                                Conditional.createLike("title", keyword),
                                Conditional.createOr(),
                                Conditional.createLike("summarization", keyword)
                        }),
                        Conditional.createOrderBy("timestamp", true)
                });

        List<KnowledgeArticle> list = new ArrayList<>();
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            KnowledgeArticle article = new KnowledgeArticle(data.get("id").getLong(), data.get("domain").getString(),
                    data.get("contact_id").getLong(), data.get("base").getString(),
                    data.get("category").getString(), data.get("title").getString(), data.get("content").getString(),
                    data.get("summarization").isNullValue() ? null : data.get("summarization").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(), data.get("month").getInt(), data.get("date").getInt(),
                    data.get("timestamp").getLong(), KnowledgeScope.parse(data.get("scope").getString()),
                    data.get("activated").getInt() == 1, data.get("segments").getInt());
            list.add(article);
        }

        List<KnowledgeArticle> resultList = new ArrayList<>();
        for (KnowledgeArticle article : list) {
            if (article.scope == KnowledgeScope.Private && article.contactId != contactId) {
                // 跳过不属于联系人的文章
                continue;
            }
            resultList.add(article);
        }

        return resultList;
    }

    public List<String> queryAllArticleCategories(long contactId) {
        List<String> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT `category` FROM `").append(this.knowledgeArticleTable);
        sql.append("` WHERE `contact_id`=").append(contactId);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        for (StorageField[] fields : result) {
            list.add(fields[0].getString());
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

    public Chart readLastChart(String name) {
        List<StorageField[]> result = this.storage.executeQuery(this.chartTable,
                this.chartFields, new Conditional[] {
                        Conditional.createEqualTo("name", name),
                        Conditional.createOrderBy("timestamp", true),
                        Conditional.createLimit(1)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        Chart chart = new Chart(new JSONObject(data.get("chart").getString()));
        return chart;
    }

    public boolean insertChart(Chart chart) {
        Date date = new Date(chart.timestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return this.storage.executeInsert(this.chartTable, new StorageField[] {
                new StorageField("name", chart.name),
                new StorageField("desc", chart.desc),
                new StorageField("timestamp", chart.timestamp),
                new StorageField("year", calendar.get(Calendar.YEAR)),
                new StorageField("month", calendar.get(Calendar.MONTH) + 1),
                new StorageField("date", calendar.get(Calendar.DATE)),
                new StorageField("chart", chart.toJSON().toString())
        });
    }

    public boolean deleteChart(String name) {
        return this.storage.executeDelete(this.chartTable, new Conditional[] {
                Conditional.createEqualTo("name", name)
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

    public List<PromptRecord> readPrompts() {
        return this.readPrompts(0);
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
                new StorageField(this.promptWordTable, "title", LiteralBase.STRING),
                new StorageField(this.promptWordTable, "content", LiteralBase.STRING),
                new StorageField(this.promptWordTable, "act", LiteralBase.STRING),
                new StorageField(this.promptWordScopeTable, "contact_id", LiteralBase.LONG)
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
            PromptRecord promptRecord = new PromptRecord(data.get("id").getLong(), data.get("title").getString(),
                    data.get("content").getString(), data.get("act").getString(),
                    data.get("contact_id").getLong() == 0);
            promptRecords.add(promptRecord);
        }

        return promptRecords;
    }

    public PromptRecord readPrompt(String title) {
        List<StorageField[]> result = this.storage.executeQuery(this.promptWordTable, this.promptWordFields,
                new Conditional[] {
                        Conditional.createEqualTo("title", title)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        return new PromptRecord(data.get("id").getLong(),
                data.get("title").getString(),
                data.get("content").getString(),
                data.get("act").getString(),
                false);
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
                    new StorageField("title", promptRecord.title),
                    new StorageField("content", promptRecord.content),
                    new StorageField("act", promptRecord.act)
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.promptWordTable, new StorageField[] {
                    new StorageField("title", promptRecord.title),
                    new StorageField("content", promptRecord.content),
                    new StorageField("act", promptRecord.act)
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
            PromptRecord record = this.readPrompt(promptRecord.title);
            if (null == record) {
                // 插入
                long id = Utils.generateSerialNumber();
                this.storage.executeInsert(this.promptWordTable, new StorageField[]{
                        new StorageField("id", id),
                        new StorageField("title", promptRecord.title),
                        new StorageField("content", promptRecord.content),
                        new StorageField("act", promptRecord.act)
                });
                idList.add(id);
            }
            else {
                // 更新
                this.storage.executeUpdate(this.promptWordTable, new StorageField[] {
                        new StorageField("content", promptRecord.content),
                        new StorageField("act", promptRecord.act)
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

    public int deletePrompt(long contactId, String title) {
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
                                "title", LiteralBase.STRING, title))
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

    public ContactPreference readContactPreference(long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.contactPreferenceTable,
                this.contactPreferenceFields, new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        if (data.get("models").isNullValue()) {
            return null;
        }

        JSONArray models = new JSONArray(data.get("models").getString());
        return new ContactPreference(contactId, models);
    }

    public boolean writeEmotionRecord(EmotionRecord emotionRecord) {
        return this.storage.executeInsert(this.emotionTable, new StorageField[] {
                new StorageField("contact_id", emotionRecord.contactId),
                new StorageField("emotion", emotionRecord.emotion.name()),
                new StorageField("timestamp", emotionRecord.getTimestamp()),
                new StorageField("source", emotionRecord.source),
                new StorageField("source_data", LiteralBase.STRING,
                        (null != emotionRecord.sourceData) ? emotionRecord.sourceData.toString() : null),
        });
    }

    public List<EmotionRecord> readEmotionRecords(long contactId) {
        List<EmotionRecord> list = new ArrayList<>();
        List<StorageField[]> result = this.storage.executeQuery(this.emotionTable, this.emotionFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId)
        });
        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            EmotionRecord record = new EmotionRecord(data.get("id").getLong(),
                    data.get("contact_id").getLong(),
                    Emotion.parse(data.get("emotion").getString()),
                    data.get("timestamp").getLong(),
                    data.get("source").getString());
            if (!data.get("source_data").isNullValue()) {
                record.sourceData = new JSONObject(data.get("source_data").getString());
            }
            list.add(record);
        }
        return list;
    }

    private void resetDefaultConfig() {
        // 支持中英双语的对话语言模型，具有 70 亿参数。针对中文问答和对话进行了优化。
        // 经过约 1T 标识符的中英双语训练，辅以监督微调、反馈自助、人类反馈强化学习等技术的优化。
        JSONObject parameter = new JSONObject();
        parameter.put("unit", "Baize");
        ModelConfig baizeNLG = new ModelConfig("Baize", "Baize",
                "适合大多数场景的通用模型",
                "http://127.0.0.1:7010/aigc/chat/", parameter);

        // 支持中英双语的功能型对话语言大模型。以轻量化实现高质量效果的模型。在1000亿 Token 中文语料上预训练，累计学习1.5万亿中文 Token，
        // 并且在数百种任务上进行 Prompt 任务式训练。针对理解类任务，如分类、情感分析、抽取等，可以自定义标签体系；针对多种生成任务，
        // 可以进行采样自由生成。
        parameter = new JSONObject();
        parameter.put("unit", "BaizeX");
        ModelConfig baizeX = new ModelConfig("BaizeX", "BaizeX",
                "适合一般场景且速度较快的通用模型",
                "http://127.0.0.1:7010/aigc/chat/", parameter);

        // 支持中英双语和多种插件的开源对话语言模型。模型具有 130 亿参数。在约七千亿中英文以及代码单词上预训练得到，后续经过对话指令微调、
        // 插件增强学习和人类偏好训练具备多轮对话能力及使用多种插件的能力。
        parameter = new JSONObject();
        ModelConfig baizeNEXT = new ModelConfig("BaizeNext", "BaizeNext",
                "支持下游任务的大语言生成模型",
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
                new StorageField("comment", "适合大多数场景的通用模型"),
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
                new StorageField("comment", "适合一般场景且速度较快的通用模型"),
                new StorageField("modified", System.currentTimeMillis())
        });

        // BaizeNext
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
