/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.aigc.scene;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.ReportSuggestion;
import cube.aigc.psychology.composition.Scale;
import cube.common.Storagable;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.EmojiFilter;
import cube.util.JSONUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 心理学场景的存储器。
 */
public class PsychologyStorage implements Storagable {

    private final String reportTable = "psychology_report";

    private final String paintingTable = "psychology_painting";

    private final String reportTextTable = "psychology_report_text";

    @Deprecated
    private final String reportBehaviorTable = "psychology_report_behavior";

    @Deprecated
    private final String reportParagraphTable = "psychology_report_paragraph";

    private final String scaleTable = "psychology_scale";

    private final String scaleAnswerTable = "psychology_scale_answer";

    private final StorageField[] reportFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("gender", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("age", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("strict", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("theme", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("finished_timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("summary", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("evaluation_data", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] paintingFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("report_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    @Deprecated
    private final StorageField[] reportBehaviorFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("report_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("term", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("description", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("behavior", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("suggestion", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] reportTextFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("report_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("indicator", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL,
            }),
            new StorageField("report", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("suggestion", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    @Deprecated
    private final StorageField[] reportParagraphFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("report_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("score", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("description", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("opinion", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("features", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("suggestions", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] scaleFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("gender", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("age", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("complete", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] scaleAnswerFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("scale_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("sheet", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("num_answers", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private Storage storage;

    public final int limit = 5;

    public PsychologyStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "PsychologyStorage", config);
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
        if (!this.storage.exist(this.reportTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.reportTable, this.reportFields)) {
                Logger.i(this.getClass(), "Created table '" + this.reportTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.paintingTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.paintingTable, this.paintingFields)) {
                Logger.i(this.getClass(), "Created table '" + this.paintingTable + "' successfully");
            }
        }

//        if (!this.storage.exist(this.reportBehaviorTable)) {
//            // 不存在，建新表
//            if (this.storage.executeCreate(this.reportBehaviorTable, this.reportBehaviorFields)) {
//                Logger.i(this.getClass(), "Created table '" + this.reportBehaviorTable + "' successfully");
//            }
//        }

        if (!this.storage.exist(this.reportTextTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.reportTextTable, this.reportTextFields)) {
                Logger.i(this.getClass(), "Created table '" + this.reportTextTable + "' successfully");
            }
        }

//        if (!this.storage.exist(this.reportParagraphTable)) {
//            // 不存在，建新表
//            if (this.storage.executeCreate(this.reportParagraphTable, this.reportParagraphFields)) {
//                Logger.i(this.getClass(), "Created table '" + this.reportParagraphTable + "' successfully");
//            }
//        }

        if (!this.storage.exist(this.scaleTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.scaleTable, this.scaleFields)) {
                Logger.i(this.getClass(), "Created table '" + this.scaleTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.scaleAnswerTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.scaleAnswerTable, this.scaleAnswerFields)) {
                Logger.i(this.getClass(), "Created table '" + this.scaleAnswerTable + "' successfully");
            }
        }
    }

    public PsychologyReport readPsychologyReport(long sn) {
        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", sn)
                });
        if (result.isEmpty()) {
            return null;
        }

        PsychologyReport report = this.makeReport(result.get(0));
        return report;
    }

    public List<PsychologyReport> readPsychologyReports(long contactId) {
        List<PsychologyReport> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        for (StorageField[] fields : result) {
            PsychologyReport report = this.makeReport(fields);
            // 添加到列表
            list.add(report);
        }

        return list;
    }

    public int countPsychologyReports(long contactId, long starTime, long endTime) {
        List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(*) FROM " + this.reportTable +
                " WHERE contact_id=" + contactId + " AND `timestamp`>=" + starTime +
                " AND `timestamp`<=" + endTime);
        return result.get(0)[0].getInt();
    }

    public List<PsychologyReport> readPsychologyReports(long contactId, long starTime, long endTime, int pageIndex) {
        List<PsychologyReport> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", starTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThanEqual(new StorageField("timestamp", endTime)),
                        Conditional.createLimitOffset(this.limit, pageIndex * this.limit)
                });

        for (StorageField[] fields : result) {
            PsychologyReport report = this.makeReport(fields);
            list.add(report);
        }

        return list;
    }

    public boolean writePsychologyReport(PsychologyReport report) {
//        if (null != report.getBehaviorList()) {
//            for (DescriptionSuggestion bs : report.getBehaviorList()) {
//                this.storage.executeInsert(this.reportBehaviorTable, new StorageField[] {
//                        new StorageField("report_sn", report.sn),
//                        new StorageField("term", bs.term.word),
//                        new StorageField("description", bs.description),
//                        new StorageField("behavior", EmojiFilter.filterEmoji(bs.behavior)),
//                        new StorageField("suggestion", EmojiFilter.filterEmoji(bs.suggestion))
//                });
//            }
//        }

        if (null != report.getReportTextList()) {
            for (ReportSuggestion rs : report.getReportTextList()) {
                this.storage.executeInsert(this.reportTextTable, new StorageField[] {
                        new StorageField("report_sn", report.sn),
                        new StorageField("indicator", rs.indicator.name),
                        new StorageField("title", rs.title),
                        new StorageField("report", EmojiFilter.filterEmoji(rs.report)),
                        new StorageField("suggestion", EmojiFilter.filterEmoji(rs.suggestion))
                });
            }
        }

        this.storage.executeDelete(this.reportTable, new Conditional[] {
                Conditional.createEqualTo("sn", report.sn)
        });

        String dataString = report.getEvaluationReport().toJSON().toString();
        dataString = JSONUtils.escape(dataString);

        return this.storage.executeInsert(this.reportTable, new StorageField[] {
                new StorageField("sn", report.sn),
                new StorageField("contact_id", report.contactId),
                new StorageField("timestamp", report.timestamp),
                new StorageField("name", report.getName()),
                new StorageField("gender", report.getAttribute().gender),
                new StorageField("age", report.getAttribute().age),
                new StorageField("strict", report.getAttribute().strict ? 1 : 0),
                new StorageField("file_code", report.getFileCode()),
                new StorageField("theme", report.getTheme().code),
                new StorageField("finished_timestamp", report.getFinishedTimestamp()),
                new StorageField("summary", report.getSummary()),
                new StorageField("evaluation_data", dataString)
        });
    }

    public Painting readPainting(long sn) {
        List<StorageField[]> result = this.storage.executeQuery(this.paintingTable, this.paintingFields,
                new Conditional[] {
                        Conditional.createEqualTo("report_sn", sn)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> fields = StorageFields.get(result.get(0));
        try {
            return new Painting(new JSONObject(fields.get("data").getString()));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readPainting", e);
            return null;
        }
    }

    public boolean writePainting(long sn, String fileCode, Painting painting) {
        List<StorageField[]> result = this.storage.executeQuery(this.paintingTable, this.paintingFields,
                new Conditional[] {
                        Conditional.createEqualTo("report_sn", sn)
                });

        if (result.isEmpty()) {
            // 插入
            return this.storage.executeInsert(this.paintingTable, new StorageField[] {
                    new StorageField("report_sn", sn),
                    new StorageField("file_code", fileCode),
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("data", painting.toJSON().toString())
            });
        }
        else {
            // 更新
            return this.storage.executeUpdate(this.paintingTable, new StorageField[] {
                    new StorageField("file_code", fileCode),
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("data", painting.toJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("report_sn", sn)
            });
        }
    }

    public Scale readScale(long sn) {
        List<StorageField[]> result = this.storage.executeQuery(this.scaleTable, this.scaleFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", sn)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> fields = StorageFields.get(result.get(0));
        try {
            String dataStr = JSONUtils.filter(fields.get("data").getString());
            return new Scale(new JSONObject(dataStr));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readScale", e);
            return null;
        }
    }

    public synchronized boolean writeScale(Scale scale) {
        List<StorageField[]> result = this.storage.executeQuery(this.scaleTable, new StorageField[] {
                    new StorageField("id", LiteralBase.LONG)
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", scale.getSN())
            });

        if (result.isEmpty()) {
            return this.storage.executeInsert(this.scaleTable, new StorageField[] {
                    new StorageField("sn", scale.getSN()),
                    new StorageField("name", scale.name),
                    new StorageField("timestamp", scale.getTimestamp()),
                    new StorageField("gender", scale.getAttribute().gender),
                    new StorageField("age", scale.getAttribute().age),
                    new StorageField("complete", scale.isComplete() ? 1 : 0),
                    new StorageField("data", scale.toJSON().toString())
            });
        }
        else {
            return this.storage.executeUpdate(this.scaleTable, new StorageField[] {
                    new StorageField("name", scale.name),
                    new StorageField("timestamp", scale.getTimestamp()),
                    new StorageField("gender", scale.getAttribute().gender),
                    new StorageField("age", scale.getAttribute().age),
                    new StorageField("complete", scale.isComplete() ? 1 : 0),
                    new StorageField("data", scale.toJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", scale.getSN())
            });
        }
    }

    public AnswerSheet readAnswerSheet(long scaleSn) {
        List<StorageField[]> result = this.storage.executeQuery(this.scaleAnswerTable, this.scaleAnswerFields,
                new Conditional[] {
                        Conditional.createEqualTo("scale_sn", scaleSn)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> fields = StorageFields.get(result.get(0));
        try {
            String dataStr = JSONUtils.filter(fields.get("sheet").getString());
            return new AnswerSheet(new JSONObject(dataStr));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readAnswerSheet", e);
            return null;
        }
    }

    public synchronized boolean writeAnswerSheet(AnswerSheet answerSheet) {
        List<StorageField[]> result = this.storage.executeQuery(this.scaleAnswerTable, this.scaleAnswerFields,
                new Conditional[] {
                        Conditional.createEqualTo("scale_sn", answerSheet.scaleSn)
                });

        if (result.isEmpty()) {
            return this.storage.executeInsert(this.scaleAnswerTable, new StorageField[] {
                    new StorageField("scale_sn", answerSheet.scaleSn),
                    new StorageField("sheet", answerSheet.toJSON().toString()),
                    new StorageField("num_answers", answerSheet.answers.size())
            });
        }
        else {
            return this.storage.executeUpdate(this.scaleAnswerTable, new StorageField[] {
                    new StorageField("sheet", answerSheet.toJSON().toString()),
                    new StorageField("num_answers", answerSheet.answers.size())
            }, new Conditional[] {
                    Conditional.createEqualTo("scale_sn", answerSheet.scaleSn)
            });
        }
    }

    private PsychologyReport makeReport(StorageField[] storageFields) {
        Map<String, StorageField> data = StorageFields.get(storageFields);

        PsychologyReport report = new PsychologyReport(data.get("sn").getLong(), data.get("contact_id").getLong(),
                data.get("timestamp").getLong(), data.get("name").getString(),
                new Attribute(data.get("gender").getString(), data.get("age").getInt(),
                        data.get("strict").getInt() == 1),
                data.get("file_code").getString(), Theme.parse(data.get("theme").getString()),
                data.get("finished_timestamp").getLong());

        if (!data.get("summary").isNullValue()) {
            report.setSummary(data.get("summary").getString());
        }

        if (!data.get("evaluation_data").isNullValue()) {
            JSONObject dataJson = null;
            try {
                dataJson = new JSONObject(data.get("evaluation_data").getString().trim());
            } catch (Exception e) {
                try {
                    dataJson = new JSONObject(JSONUtils.filter(data.get("evaluation_data").getString().trim()));
                } catch (Exception se) {
                    Logger.e(this.getClass(), "#makeReport", se);
                }
            }

            if (null == dataJson) {
                Logger.w(this.getClass(), "#makeReport - `evaluation_data` data error: " + report.sn);
                return null;
            }

            EvaluationReport evaluationReport = new EvaluationReport(dataJson);
            report.setEvaluationReport(evaluationReport);
        }

        List<StorageField[]> fields = null;

//        List<StorageField[]> fields = this.storage.executeQuery(this.reportBehaviorTable,
//                this.reportBehaviorFields, new Conditional[] {
//                        Conditional.createEqualTo("report_sn", report.sn)
//                });
//        List<DescriptionSuggestion> behaviorList = new ArrayList<>();
//        for (StorageField[] behaviorFields : fields) {
//            Map<String, StorageField> bf = StorageFields.get(behaviorFields);
//            DescriptionSuggestion bs = new DescriptionSuggestion(
//                    Term.parse(bf.get("term").getString()),
//                    bf.get("description").getString(),
//                    bf.get("behavior").getString(),
//                    bf.get("suggestion").getString());
//            behaviorList.add(bs);
//        }
//        report.setBehaviorList(behaviorList);

        fields = this.storage.executeQuery(this.reportTextTable,
                this.reportTextFields, new Conditional[] {
                        Conditional.createEqualTo("report_sn", report.sn)
                });
        List<ReportSuggestion> textList = new ArrayList<>();
        for (StorageField[] textFields : fields) {
            Map<String, StorageField> tf = StorageFields.get(textFields);
            ReportSuggestion rs = new ReportSuggestion(
                    Indicator.parse(tf.get("indicator").getString()),
                    tf.get("title").getString(),
                    tf.get("report").getString(),
                    tf.get("suggestion").getString()
            );
            textList.add(rs);
        }
        report.setReportTextList(textList);

        // 生成 Markdown
        report.makeMarkdown(false);

        return report;
    }
}
