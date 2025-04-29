/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.aigc.Flowable;
import cube.aigc.ModelConfig;
import cube.aigc.Module;
import cube.aigc.Sentiment;
import cube.aigc.opinion.Article;
import cube.auth.AuthToken;
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
 * @deprecated
 */
public class PublicOpinion implements Module {

    private final String name = "PublicOpinion";

    private final static String PositiveQueryFormat = "已知信息：%s\n\n" +
            "根据上述已知信息，请回答：关于%s的正面描述内容有哪些？如果没有正面描述则回答没有正面描述。";

    private final static String NegativeQueryFormat = "已知信息：%s\n\n" +
            "根据上述已知信息，请回答：关于%s的负面描述内容有哪些？如果没有负面描述则回答没有负面描述。";

    private final static String OtherQueryFormat = "已知信息：%s\n\n" +
            "根据上述已知信息，请回答：关于%s的描述内容有哪些？";

    private final static String ArticleSentimentClassificationFormat = "已知内容：%s\n\n" +
            "根据上述已知内容，请分别说明关于%s的正面描述内容和%s的负面描述内容，如果没有正面描述或者负面描述则回答无相关描述。";

    private final static String ArticleQueryOutputFormat = "在《%s》这篇文章里，%s";

    private int maxArticleLength = ModelConfig.BAIZE_CONTEXT_LIMIT;

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
    public Flowable match(AuthToken token, String content) {
        return null;
    }

    @Override
    public List<String> getMatchingWords() {
        return this.matchingWords;
    }

    /**
     *
     * @param category
     * @param sentiment
     * @param article
     * @return 返回文章的 ID 。
     */
    public long addArticle(String category, String sentiment, Article article) {
        if (null == this.storage) {
            return 0;
        }

        return this.storage.writeArticle(category, sentiment, article);
    }

    /**
     *
     * @param category
     * @param title
     * @return 返回删除的记录数量。
     */
    public int removeArticle(String category, String title) {
        if (null == this.storage) {
            return 0;
        }

        return this.storage.deleteArticle(category, title);
    }

    public List<Article> getArticleList(String category) {
        if (null == this.storage) {
            return null;
        }

        return this.storage.readArticles(category);
    }

    /**
     * 生成评估文章的 Query 。
     *
     * @param category
     * @param title
     * @param sentiment
     * @return
     */
    public ArticleQuery makeEvaluatingArticleQuery(String category, String title, Sentiment sentiment) {
        if (null == this.storage) {
            return null;
        }

        Sentiment articleSentiment = sentiment;

        Article article = this.storage.readArticle(category, title);

        if (null == articleSentiment) {
            articleSentiment = article.sentiment;
        }

        List<String> contentList = this.composeArticleContent(article);
        String content = contentList.get(0);
        String query = null;
        if (articleSentiment == Sentiment.Positive) {
            query = String.format(PositiveQueryFormat, content, category);
        }
        else if (articleSentiment == Sentiment.Negative) {
            query = String.format(NegativeQueryFormat, content, category);
        }
        else {
            query = String.format(OtherQueryFormat, content, category);
        }

        ArticleQuery articleQuery = new ArticleQuery(article, articleSentiment, query);
        return articleQuery;
    }

    /**
     * 生成评估文章的 Query 列表。
     *
     * @param category
     * @param sentiment
     * @param year
     * @param month
     * @param startDate
     * @param endDate
     * @return
     */
    public List<ArticleQuery> makeEvaluatingArticleQueries(String category, Sentiment sentiment,
                                                           int year, int month, int startDate, int endDate) {
        List<ArticleQuery> result = new ArrayList<>();
        if (null == this.storage) {
            return result;
        }

        List<Article> articleList = this.storage.readArticles(category, sentiment.code, year, month, startDate, endDate);
        for (Article article : articleList) {
            // 控制内容长度，防止溢出
            List<String> contentList = this.composeArticleContent(article);
            // 目前只处理第一部分内容
            String content = contentList.get(0);
            String query = (sentiment == Sentiment.Positive) ?
                    String.format(PositiveQueryFormat, content, category) :
                    String.format(NegativeQueryFormat, content, category);

            ArticleQuery articleQuery = new ArticleQuery(article, sentiment, query);
            result.add(articleQuery);
        }

        return result;
    }

    public ArticleQuery makeArticleClassificationQuery(String category, String title) {
        if (null == this.storage) {
            return null;
        }

        Article article = this.storage.readArticle(category, title);
        if (null == article.sentiment) {
            Logger.w(this.getClass(),
                    "#makeArticleClassificationQuery - Article do not have sentiment value : " + title);
            return null;
        }

        List<String> contentList = this.composeArticleContent(article);
        String content = contentList.get(0);
        String query = String.format(ArticleSentimentClassificationFormat, content, category, category);
        return new ArticleQuery(article, article.sentiment, query);
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

    public class ArticleQuery {

        public Article article;

        public Sentiment sentiment;

        public String query;

        public String answer;

        protected ArticleQuery(Article article, Sentiment sentiment, String query) {
            this.article = article;
            this.sentiment = sentiment;
            this.query = query;
        }

        public String output() {
            if (null == this.answer) {
                return null;
            }

            return String.format(ArticleQueryOutputFormat, this.article.title, this.answer);
        }
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
            JSONObject config = ConfigUtils.readJsonFile("storage_public_opinion.json");
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

        public Article readArticle(String category, String title) {
            List<StorageField[]> result = this.storage.executeQuery(this.articleTable, this.articleFields,
                    new Conditional[] {
                            Conditional.createEqualTo("category", category),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("title", title),
                    });

            if (result.isEmpty()) {
                return null;
            }

            StorageField[] fields = result.get(result.size() - 1);
            Map<String, StorageField> data = StorageFields.get(fields);
            Article article = new Article(data.get("title").getString(), data.get("content").getString(),
                    data.get("author").getString(),
                    data.get("year").getInt(),
                    data.get("month").getInt(),
                    data.get("date").getInt());
            article.id = data.get("id").getLong();
            article.sentiment = Sentiment.parse(data.get("sentiment").getString());
            return article;
        }

        public List<Article> readArticles(String category) {
            List<Article> list = new ArrayList<>();
            List<StorageField[]> result = this.storage.executeQuery(this.articleTable, this.articleFields, new Conditional[]{
                    Conditional.createEqualTo("category", category)
            });

            for (StorageField[] fields : result) {
                Map<String, StorageField> data = StorageFields.get(fields);
                Article article = new Article(data.get("title").getString(), data.get("content").getString(),
                        data.get("author").getString(),
                        data.get("year").getInt(),
                        data.get("month").getInt(),
                        data.get("date").getInt());
                article.id = data.get("id").getLong();
                article.sentiment = Sentiment.parse(data.get("sentiment").getString());
                list.add(article);
            }

            return list;
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
                article.sentiment = Sentiment.parse(data.get("sentiment").getString());
                list.add(article);
            }

            return list;
        }

        public synchronized long writeArticle(String category, String sentiment, Article article) {
            if (this.storage.executeInsert(this.articleTable, new StorageField[] {
                    new StorageField("category", category),
                    new StorageField("sentiment", sentiment),
                    new StorageField("title", article.title),
                    new StorageField("content", article.content),
                    new StorageField("author", article.author),
                    new StorageField("year", article.year),
                    new StorageField("month", article.month),
                    new StorageField("date", article.date),
            })) {
                List<StorageField[]> result = this.storage.executeQuery(
                        String.format("SELECT MAX(id) FROM `%s`", this.articleTable));
                if (result.isEmpty()) {
                    return -1;
                }

                return result.get(0)[0].getLong();
            }
            else {
                return -1;
            }
        }

        public synchronized int deleteArticle(String category, String title) {
            List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(id) FROM `"
                    + this.articleTable + "` WHERE `category`='" + category + "' AND `title`="
                    + "'" + title + "'");
            if (result.isEmpty()) {
                return 0;
            }

            int num = result.get(0)[0].getInt();

            this.storage.executeDelete(this.articleTable, new Conditional[] {
                    Conditional.createEqualTo("category", category),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("title", title)
            });

            return num;
        }
    }
}
