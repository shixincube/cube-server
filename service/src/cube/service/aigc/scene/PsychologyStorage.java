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
import cube.common.Storagable;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
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

    private final String reportParagraphTable = "psychology_report_paragraph";

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
            new StorageField("fileCode", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("theme", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("evaluation_data", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

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

    private Storage storage;

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

        if (!this.storage.exist(this.reportParagraphTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.reportParagraphTable, this.reportParagraphFields)) {
                Logger.i(this.getClass(), "Created table '" + this.reportParagraphTable + "' successfully");
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

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        PsychologyReport report = new PsychologyReport(data.get("sn").getLong(), data.get("contact_id").getLong(),
                data.get("timestamp").getLong(), data.get("name").getString(),
                new Attribute(data.get("gender").getString(), data.get("age").getInt()),
                data.get("fileCode").getString(), Theme.parse(data.get("theme").getString()));

        if (!data.get("evaluation_data").isNullValue()) {
            EvaluationReport evaluationReport = new EvaluationReport(new JSONObject(data.get("evaluation_data").getString()));
            report.setEvaluationReport(evaluationReport);
        }

        List<StorageField[]> paragraphResult = this.storage.executeQuery(this.reportParagraphTable,
                this.reportParagraphFields, new Conditional[] {
                        Conditional.createEqualTo("report_sn", report.sn)
                });
        for (StorageField[] paragraphFields : paragraphResult) {
            Map<String, StorageField> pd = StorageFields.get(paragraphFields);
            ReportParagraph paragraph = new ReportParagraph(pd.get("title").getString(),
                    pd.get("score").getInt(), pd.get("description").getString(), pd.get("opinion").getString());

            String arrayString = pd.get("features").getString();
            JSONArray array = new JSONArray(arrayString);
            paragraph.addFeatures(JSONUtils.toStringList(array));

            arrayString = pd.get("suggestions").getString();
            array = new JSONArray(arrayString);
            paragraph.addSuggestions(JSONUtils.toStringList(array));

            report.addParagraph(paragraph);
        }

        return report;
    }

    public List<PsychologyReport> readPsychologyReports(long contactId) {
        List<PsychologyReport> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.reportTable, this.reportFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> data = StorageFields.get(fields);
            PsychologyReport report = new PsychologyReport(data.get("sn").getLong(), data.get("contact_id").getLong(),
                    data.get("timestamp").getLong(), data.get("name").getString(),
                    new Attribute(data.get("gender").getString(), data.get("age").getInt()),
                    data.get("fileCode").getString(), Theme.parse(data.get("theme").getString()));

            List<StorageField[]> paragraphResult = this.storage.executeQuery(this.reportParagraphTable,
                    this.reportParagraphFields, new Conditional[] {
                            Conditional.createEqualTo("report_sn", report.sn)
                    });
            for (StorageField[] paragraphFields : paragraphResult) {
                Map<String, StorageField> pd = StorageFields.get(paragraphFields);
                ReportParagraph paragraph = new ReportParagraph(pd.get("title").getString(),
                        pd.get("score").getInt(), pd.get("description").getString(), pd.get("opinion").getString());

                String arrayString = pd.get("features").getString();
                JSONArray array = new JSONArray(arrayString);
                paragraph.addFeatures(JSONUtils.toStringList(array));

                arrayString = pd.get("suggestions").getString();
                array = new JSONArray(arrayString);
                paragraph.addSuggestions(JSONUtils.toStringList(array));

                report.addParagraph(paragraph);
            }

            // 添加到列表
            list.add(report);
        }

        return list;
    }

    public boolean writePsychologyReport(PsychologyReport report) {
        for (ReportParagraph paragraph : report.getParagraphs()) {
            this.storage.executeInsert(this.reportParagraphTable, new StorageField[] {
                    new StorageField("report_sn", report.sn),
                    new StorageField("title", paragraph.title),
                    new StorageField("score", paragraph.score),
                    new StorageField("description", paragraph.description),
                    new StorageField("opinion", paragraph.opinion),
                    new StorageField("features", JSONUtils.toStringArray(paragraph.getFeatures()).toString()),
                    new StorageField("suggestions", JSONUtils.toStringArray(paragraph.getSuggestions()).toString())
            });
        }

        return this.storage.executeInsert(this.reportTable, new StorageField[] {
                new StorageField("sn", report.sn),
                new StorageField("contact_id", report.contactId),
                new StorageField("timestamp", report.timestamp),
                new StorageField("name", report.getName()),
                new StorageField("gender", report.getAttribute().gender),
                new StorageField("age", report.getAttribute().age),
                new StorageField("fileCode", report.getFileCode()),
                new StorageField("theme", report.getTheme().code)
        });
    }
}
