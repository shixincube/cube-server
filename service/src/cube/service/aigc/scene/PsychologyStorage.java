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
import cube.aigc.psychology.composition.*;
import cube.common.Storagable;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.tokenizer.Tokenizer;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.EmojiFilter;
import cube.util.JSONUtils;
import org.json.JSONArray;
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

    private final String scaleTable = "psychology_scale";

    private final String scaleAnswerTable = "psychology_scale_answer";

    private final String scaleReportTable = "psychology_scale_report";

    private final String paintingLabelTable = "psychology_painting_label";

    private final String paintingReportManagementTable = "psychology_painting_report_mgmt";

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
            }),
            new StorageField("mandala_flower", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] reportFieldsForQuery = new StorageField[] {
            new StorageField(reportTable, "sn", LiteralBase.LONG),
            new StorageField(reportTable, "contact_id", LiteralBase.LONG),
            new StorageField(reportTable, "timestamp", LiteralBase.LONG),
            new StorageField(reportTable, "name", LiteralBase.STRING),
            new StorageField(reportTable, "gender", LiteralBase.STRING),
            new StorageField(reportTable, "age", LiteralBase.INT),
            new StorageField(reportTable, "strict", LiteralBase.INT),
            new StorageField(reportTable, "file_code", LiteralBase.STRING),
            new StorageField(reportTable, "theme", LiteralBase.STRING),
            new StorageField(reportTable, "finished_timestamp", LiteralBase.LONG),
            new StorageField(reportTable, "summary", LiteralBase.STRING),
            new StorageField(reportTable, "evaluation_data", LiteralBase.STRING),
            new StorageField(reportTable, "mandala_flower", LiteralBase.STRING)
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

    private final StorageField[] scaleReportFields = new StorageField[] {
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
            new StorageField("factor_data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] paintingLabelFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("description", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("evaluation_scores", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("representations", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] paintingReportManagementFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("report_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private Storage storage;

    private Tokenizer tokenizer;

    public final int limit = 5;

    public PsychologyStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "PsychologyStorage", config);
    }

    public void open(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.open();
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
                // evaluation_data 字段修改为 MEDIUMTEXT
                /*
                ALTER TABLE `psychology_report` CHANGE COLUMN `evaluation_data` `evaluation_data` MEDIUMTEXT NULL DEFAULT NULL ;
                */
                this.storage.execute("ALTER TABLE `" + this.reportTable +
                        "` CHANGE COLUMN `evaluation_data` `evaluation_data` MEDIUMTEXT NULL DEFAULT NULL");
                Logger.i(this.getClass(), "Created table '" + this.reportTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.paintingTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.paintingTable, this.paintingFields)) {
                Logger.i(this.getClass(), "Created table '" + this.paintingTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.reportTextTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.reportTextTable, this.reportTextFields)) {
                Logger.i(this.getClass(), "Created table '" + this.reportTextTable + "' successfully");
            }
        }

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

        if (!this.storage.exist(this.scaleReportTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.scaleReportTable, this.scaleReportFields)) {
                Logger.i(this.getClass(), "Created table '" + this.scaleReportTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.paintingLabelTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.paintingLabelTable, this.paintingLabelFields)) {
                Logger.i(this.getClass(), "Created table '" + this.paintingLabelTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.paintingReportManagementTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.paintingReportManagementTable, this.paintingReportManagementFields)) {
                Logger.i(this.getClass(), "Created table '" + this.paintingReportManagementTable + "' successfully");
            }
        }

        // 检查管理表里是否有遗漏数据
//        this.recheckReportManagementTable();
    }

    private void recheckReportManagementTable() {
        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        });
        for (StorageField[] fields : result) {
            long sn = fields[0].getLong();
            List<StorageField[]> mgmtResult = this.storage.executeQuery(this.paintingReportManagementTable,
                    new StorageField[] { new StorageField("state", LiteralBase.INT) },
                    new Conditional[] { Conditional.createEqualTo("report_sn", sn) });
            if (mgmtResult.isEmpty()) {
                Logger.w(this.getClass(), "#recheckReportManagementTable - Insert state to report: " + sn);
                this.storage.executeInsert(this.paintingReportManagementTable, new StorageField[] {
                        new StorageField("report_sn", sn),
                        new StorageField("timestamp", System.currentTimeMillis()),
                        new StorageField("state", 0)
                });
            }
        }
    }

    public PaintingReport readPsychologyReport(long sn) {
        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", sn)
                });
        if (result.isEmpty()) {
            return null;
        }

        PaintingReport report = this.makeReport(result.get(0));
        return report;
    }

    public int countPsychologyReports() {
        List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(*) FROM " + this.reportTable +
                " WHERE finished_timestamp<>0");
        return result.get(0)[0].getInt();
    }

    public int countPsychologyReports(long contactId, long starTime, long endTime) {
        List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(*) FROM " + this.reportTable +
                " WHERE contact_id=" + contactId + " AND `timestamp`>=" + starTime +
                " AND `timestamp`<=" + endTime);
        return result.get(0)[0].getInt();
    }

    public int countPsychologyReports(int state) {
        List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(sn) FROM " +
                this.reportTable + "," + this.paintingReportManagementTable +
                " WHERE " +
                this.reportTable + ".sn=" + this.paintingReportManagementTable + ".report_sn" +
                " AND " +
                this.paintingReportManagementTable + ".state=" + state +
                " AND " +
                this.reportTable + ".finished_timestamp<>0");
        return result.get(0)[0].getInt();
    }

    public List<PaintingReport> readPsychologyReports(int pageIndex, int pageSize, boolean descending) {
        List<PaintingReport> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createUnequalTo("finished_timestamp", (long) 0),
                        Conditional.createOrderBy("timestamp", descending),
                        Conditional.createLimitOffset(pageSize, pageIndex * pageSize)
                });

        for (StorageField[] fields : result) {
            PaintingReport report = this.makeReport(fields);
            list.add(report);
        }

        return list;
    }

    public List<PaintingReport> readPsychologyReports(int pageIndex, int pageSize, boolean descending, int state) {
        List<PaintingReport> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(new String[] {
                        this.reportTable,
                        this.paintingReportManagementTable
                }, this.reportFieldsForQuery,
                new Conditional[] {
                        Conditional.createEqualTo(
                                new StorageField(this.paintingReportManagementTable, "report_sn", LiteralBase.LONG),
                                new StorageField(this.reportTable, "sn", LiteralBase.LONG)),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField(this.paintingReportManagementTable, "state",
                                LiteralBase.INT, state)),
                        Conditional.createAnd(),
                        Conditional.createUnequalTo(new StorageField(this.reportTable, "finished_timestamp",
                                LiteralBase.LONG, (long) 0)),
                        Conditional.createOrderBy(this.reportTable, "timestamp", descending),
                        Conditional.createLimitOffset(pageSize, pageIndex * pageSize)
                });

        for (StorageField[] fields : result) {
            PaintingReport report = this.makeReport(fields);
            list.add(report);
        }

        return list;
    }

    public boolean writePsychologyReport(PaintingReport report) {
        // 插入状态
        if (!this.storage.executeInsert(this.paintingReportManagementTable, new StorageField[] {
                new StorageField("report_sn", report.sn),
                new StorageField("timestamp", System.currentTimeMillis()),
                new StorageField("state", 0)
        }));

        if (null != report.getReportTextList()) {
            for (ReportSection rs : report.getReportTextList()) {
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
                new StorageField("evaluation_data", dataString),
                new StorageField("mandala_flower", (null != report.getMandalaFlower() ?
                        report.getMandalaFlower().toJSON().toString() : null))
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
            String dataStr = fields.get("data").getString();
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

        String dataString = scale.toJSON().toString();
        dataString = JSONUtils.escape(dataString);

        if (result.isEmpty()) {
            return this.storage.executeInsert(this.scaleTable, new StorageField[] {
                    new StorageField("sn", scale.getSN()),
                    new StorageField("name", scale.name),
                    new StorageField("timestamp", scale.getTimestamp()),
                    new StorageField("gender", scale.getAttribute().gender),
                    new StorageField("age", scale.getAttribute().age),
                    new StorageField("complete", scale.isComplete() ? 1 : 0),
                    new StorageField("data", dataString)
            });
        }
        else {
            return this.storage.executeUpdate(this.scaleTable, new StorageField[] {
                    new StorageField("name", scale.name),
                    new StorageField("timestamp", scale.getTimestamp()),
                    new StorageField("gender", scale.getAttribute().gender),
                    new StorageField("age", scale.getAttribute().age),
                    new StorageField("complete", scale.isComplete() ? 1 : 0),
                    new StorageField("data", dataString)
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

    public ScaleReport readScaleReport(long sn) {
        List<StorageField[]> result = this.storage.executeQuery(this.scaleReportTable, this.scaleReportFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", sn)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));

        Attribute attribute = new Attribute(data.get("gender").getString(), data.get("age").getInt(),
                data.get("strict").getInt() != 0);

        ScaleReport report = null;
        try {
            report = new ScaleReport(sn, data.get("contact_id").getLong(), data.get("timestamp").getLong(),
                    attribute, new JSONArray(data.get("factor_data").getString()));
        } catch (Exception e) {
            Logger.w(this.getClass(), "#readScaleReport", e);
        }
        return report;
    }

    public boolean writeScaleReport(ScaleReport scaleReport) {
        String dataString = scaleReport.getFactorsAsJSONArray().toString();
        dataString = JSONUtils.escape(dataString);

        List<StorageField[]> result = this.storage.executeQuery(this.scaleReportTable, this.scaleReportFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", scaleReport.sn)
                });
        if (result.isEmpty()) {
            return this.storage.executeInsert(this.scaleReportTable, new StorageField[] {
                    new StorageField("sn", scaleReport.sn),
                    new StorageField("contact_id", scaleReport.contactId),
                    new StorageField("timestamp", scaleReport.timestamp),
                    new StorageField("name", scaleReport.getName()),
                    new StorageField("gender", scaleReport.getAttribute().gender),
                    new StorageField("age", scaleReport.getAttribute().age),
                    new StorageField("strict", scaleReport.getAttribute().strict ? 1 : 0),
                    new StorageField("factor_data", dataString)
            });
        }
        else {
            return this.storage.executeUpdate(this.scaleReportTable, new StorageField[] {
                    new StorageField("contact_id", scaleReport.contactId),
                    new StorageField("timestamp", scaleReport.timestamp),
                    new StorageField("name", scaleReport.getName()),
                    new StorageField("gender", scaleReport.getAttribute().gender),
                    new StorageField("age", scaleReport.getAttribute().age),
                    new StorageField("strict", scaleReport.getAttribute().strict ? 1 : 0),
                    new StorageField("factor_data", dataString)
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", scaleReport.sn)
            });
        }
    }

    public List<PaintingLabel> readPaintingLabels(long sn) {
        List<PaintingLabel> list = new ArrayList<>();
        List<StorageField[]> result = this.storage.executeQuery(this.paintingLabelTable, this.paintingLabelFields,
                new Conditional[] {
                        Conditional.createEqualTo("sn", sn)
                });

        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            PaintingLabel label = new PaintingLabel(data.get("sn").getLong(), data.get("timestamp").getLong(),
                    data.get("description").getString(), new JSONArray(data.get("evaluation_scores").getString()));

            if (!data.get("representations").isNullValue()) {
                label.setRepresentations(new JSONArray(data.get("representations").getString()));
            }

            list.add(label);
        }

        return list;
    }

    public boolean deletePaintingLabel(long sn) {
        return this.storage.executeDelete(this.paintingLabelTable, new Conditional[] {
                Conditional.createEqualTo("sn", sn)
        });
    }

    public boolean writePaintingLabels(List<PaintingLabel> labels) {
        if (labels.isEmpty()) {
            return false;
        }

        for (PaintingLabel label : labels) {
            this.storage.executeInsert(this.paintingLabelTable, new StorageField[] {
                    new StorageField("sn", label.getSn()),
                    new StorageField("timestamp", label.getTimestamp()),
                    new StorageField("description", label.getDescription()),
                    new StorageField("evaluation_scores", label.getEvaluationScoresAsJSONArray().toString()),
                    new StorageField("representations",
                            (null != label.getRepresentations()) ?
                                    label.getRepresentationsAsJSONArray().toString() : null)
            });
        }

        return true;
    }

    public int readPaintingManagementState(long reportSn) {
        List<StorageField[]> result = this.storage.executeQuery(this.paintingReportManagementTable,
                this.paintingReportManagementFields, new Conditional[] {
                        Conditional.createEqualTo("report_sn", reportSn)
                });

        if (result.isEmpty()) {
            return PaintingLabel.STATE_NORMAL;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        return data.get("state").getInt();
    }

    public boolean writePaintingManagementState(long reportSn, int state) {
        return this.storage.executeUpdate(this.paintingReportManagementTable, new StorageField[] {
                new StorageField("timestamp", System.currentTimeMillis()),
                new StorageField("state", state)
        }, new Conditional[] {
                Conditional.createEqualTo("report_sn", reportSn)
        });
    }

    private PaintingReport makeReport(StorageField[] storageFields) {
        Map<String, StorageField> data = StorageFields.get(storageFields);

        PaintingReport report = new PaintingReport(data.get("sn").getLong(), data.get("contact_id").getLong(),
                data.get("timestamp").getLong(), data.get("name").getString(),
                new Attribute(data.get("gender").getString(), data.get("age").getInt(),
                        data.get("strict").getInt() == 1),
                data.get("file_code").getString(), Theme.parse(data.get("theme").getString()),
                data.get("finished_timestamp").getLong());

        if (!data.get("summary").isNullValue()) {
            report.setSummary(data.get("summary").getString());
        }

        if (!data.get("mandala_flower").isNullValue()) {
            report.setMandalaFlower(new MandalaFlower(new JSONObject(data.get("mandala_flower").getString())));
        }
        else {
            report.setMandalaFlower(new MandalaFlower("AS_001"));
        }

        EvaluationReport evaluationReport = null;

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

            evaluationReport = new EvaluationReport(dataJson);
            report.setEvaluationReport(evaluationReport);
        }

        List<StorageField[]> fields = null;

        fields = this.storage.executeQuery(this.reportTextTable,
                this.reportTextFields, new Conditional[] {
                        Conditional.createEqualTo("report_sn", report.sn)
                });
        List<ReportSection> textList = new ArrayList<>();
        for (StorageField[] textFields : fields) {
            Map<String, StorageField> tf = StorageFields.get(textFields);
            ReportSection rs = new ReportSection(
                    Indicator.parse(tf.get("indicator").getString()),
                    tf.get("title").getString(),
                    tf.get("report").getString(),
                    tf.get("suggestion").getString()
            );
            textList.add(rs);
        }
        report.setReportTextList(textList);

        // 六维
        /*HexagonDimensionScore dimensionScore = Resource.getInstance()
                .getHexDimProjection().calc(evaluationReport.getEvaluationScores());

        List<EvaluationScore> scoreList = Resource.getInstance().getBenchmark().getEvaluationScores(report.getAttribute().age);
        scoreList = this.filter(evaluationReport.getEvaluationScores(), scoreList);
        HexagonDimensionScore normDimensionScore = Resource.getInstance().getHexDimProjection().calc(scoreList);

        // 校准视觉效果
        for (HexagonDimension dim : HexagonDimension.values()) {
            int score = dimensionScore.getDimensionScore(dim);
            if (score < 10) {
                dimensionScore.record(dim, (int) Math.round(score * 2.5));
            }
            else if (score < 20) {
                dimensionScore.record(dim, (int) Math.round(score * 2));
            }

            int norm = normDimensionScore.getDimensionScore(dim);
            if (norm < 10) {
                normDimensionScore.record(dim, norm * 3);
                dimensionScore.record(dim, (int) Math.round(score * 1.8));
            } else if (norm < 20) {
                normDimensionScore.record(dim, norm * 2);
                dimensionScore.record(dim, (int) Math.round(score * 1.4));
            }
        }
        dimensionScore.normalization();
        normDimensionScore.normalization();
        */

        HexagonDimensionScore dimensionScore = new HexagonDimensionScore(evaluationReport.getFullEvaluationScores(),
                evaluationReport.getPaintingConfidence(), evaluationReport.getFactorSet());
        HexagonDimensionScore normDimensionScore = new HexagonDimensionScore(80, 80, 80, 80, 80, 80);

        // 描述
        PsychologyHelper.fillDimensionScoreDescription(this.tokenizer, dimensionScore);

        report.setDimensionalScore(dimensionScore, normDimensionScore);

        // 生成 Markdown
        report.makeMarkdown();

        return report;
    }

    private List<EvaluationScore> filter(List<EvaluationScore> indicatorList, List<EvaluationScore> sources) {
        List<EvaluationScore> result = new ArrayList<>();
        for (EvaluationScore es : indicatorList) {
            Indicator indicator = es.indicator;
            for (EvaluationScore score : sources) {
                if (score.indicator == indicator) {
                    result.add(score);
                }
            }
        }
        return result;
    }
}
