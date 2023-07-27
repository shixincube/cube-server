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

package cube.service.aigc.module;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.aigc.Sentiment;
import cube.common.Storagable;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.TextUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 舆情模组。
 */
public class PublicOpinion implements Module {

    private final String name = "PublicOpinion";

    private final static String PositiveQueryFormat = "已知信息：%s\n\n" +
            "根据上述已知信息，请回答：关于%s的正面描述内容有哪些？";

    private final static String NegativeQueryFormat = "已知信息：%s\n\n" +
            "根据上述已知信息，请回答：关于%s的负面描述内容有哪些？";

    private int maxArticleLength = 800;

    private List<String> matchingWords;

    private PublicOpinionStorage storage;

    public PublicOpinion() {
        this.matchingWords = new ArrayList<>();
        this.matchingWords.add("舆情");
        this.matchingWords.add("舆情监测");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void start() {
        this.storage = new PublicOpinionStorage();
        this.storage.open();
        this.storage.execSelfChecking(null);
    }

    @Override
    public void stop() {
        if (null != this.storage) {
            this.storage.close();
        }
    }

    @Override
    public List<String> getMatchingWords() {
        return this.matchingWords;
    }

    public void addArticle(String category, String sentiment, Article article) {
        // TODO
    }

    public List<String> makeEvaluatingArticleQueries(String category, String sentiment, int year, int month,
                                                   int startDate, int endDate) {
        List<String> result = new ArrayList<>();
        List<Article> articleList = this.storage.readArticles(category, sentiment, year, month, startDate, endDate);
        for (Article article : articleList) {
            // 控制内容长度，防止溢出
            List<String> contentList = this.composeArticleContent(article);
            for (String content : contentList) {
                String query = sentiment.equals(Sentiment.Positive) ?
                        String.format(PositiveQueryFormat, article.content, category) :
                        String.format(NegativeQueryFormat, article.content, category);
                result.add(query);
            }
        }
        return result;
    }

    private List<String> composeArticleContent(Article article) {
        List<String> result = new ArrayList<>();

        StringBuilder content = new StringBuilder();
        List<String> sentences = TextUtils.splitSentence(article.content);
        for (String sentence : sentences) {
            content.append(sentence);
            if (content.length() >= this.maxArticleLength) {
                result.add(content.toString());
                content.delete(0, content.length());
            }
        }

        if (content.length() > 0) {
            result.add(content.toString());
        }

        return result;
    }


    protected class PublicOpinionStorage implements Storagable {

        private final String articleTable = "po_article";

        private final StorageField[] articleFields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, new Constraint[] {
                        Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                }),
                // 匹配类别
                new StorageField("category", LiteralBase.STRING, new Constraint[] {
                        Constraint.NOT_NULL
                }),
                // 情感分类：positive/negative/neutral
                new StorageField("sentiment", LiteralBase.STRING, new Constraint[] {
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
        };

        private Storage storage;

        public PublicOpinionStorage() {
        }

        @Override
        public void open() {
            JSONObject config = ConfigUtils.readStorageFile("config/storage_public_opinion.json");
            if (null != config) {
                this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL,
                        "PublicOpinionStorage", config);
                this.storage.open();
            }
        }

        @Override
        public void close() {
            if (null != this.storage) {
                this.storage.close();
            }
        }

        @Override
        public void execSelfChecking(List<String> domainNameList) {
            if (null == this.storage) {
                return;
            }

            if (!this.storage.exist(this.articleTable)) {
                // 不存在，建新表
                if (this.storage.executeCreate(this.articleTable, this.articleFields)) {
                    Logger.i(this.getClass(), "Created table '" + this.articleTable + "' successfully");
                }
            }
        }

        public List<Article> readArticles(String category, String sentiment, int year, int month,
                                          int startDate, int endDate) {
            List<Article> list = new ArrayList<>();
            List<StorageField[]> result = this.storage.executeQuery(this.articleTable, this.articleFields, new Conditional[] {
                    Conditional.createLike("category", category),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("sentiment", sentiment),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("year", year),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("month", month),
                            Conditional.createAnd(),
                            Conditional.createGreaterThanEqual(new StorageField("date", startDate)),
                            Conditional.createAnd(),
                            Conditional.createLessThanEqual(new StorageField("date", endDate))
                    })
            });

            for (StorageField[] fields : result) {
                Map<String, StorageField> data = StorageFields.get(fields);
                Article article = new Article(data.get("title").getString(), data.get("content").getString(),
                        data.get("author").getString(),
                        data.get("year").getInt(),
                        data.get("month").getInt(),
                        data.get("date").getInt());
                list.add(article);
            }

            return list;
        }
    }
}
