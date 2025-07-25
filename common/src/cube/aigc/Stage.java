/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cell.util.Utils;
import cube.auth.AuthToken;
import cube.common.entity.AttachmentResource;
import cube.common.entity.ChartResource;
import cube.common.entity.Entity;
import cube.common.entity.GeneratingRecord;
import org.json.JSONObject;

import java.util.List;

/**
 * 互动舞台。
 */
public class Stage extends Entity {

    public AuthToken authToken;

    public Flowable flowable;

    public Stage(AuthToken authToken) {
        super(Utils.generateSerialNumber());
        this.authToken = authToken;
    }

    public GeneratingRecord perform() {
        return null;
    }

    /*public void perform(AIGCService service, AIGCChannel channel, StageListener listener) {
        Module mod = ModuleManager.getInstance().matchModule(this.words);
        if (null == mod) {
            return;
        }

        if (mod instanceof PublicOpinion) {
            PublicOpinion publicOpinion = (PublicOpinion) mod;

            if (!this.chartResources.isEmpty()) {
                ChartResource chartResource = this.chartResources.get(0);
                // 获取时间线
                Chart.Timeline timeline = chartResource.chart.timeline;
                Chart.TimePoint starting = timeline.first();
                Chart.TimePoint ending = timeline.last();

                AtomicInteger total = new AtomicInteger(0);
                AtomicInteger callCount = new AtomicInteger(0);
                // 结果数组
                List<PublicOpinion.ArticleQuery> result = new ArrayList<>();

                List<PublicOpinion.ArticleQuery> negativeQueries = publicOpinion.makeEvaluatingArticleQueries(
                        chartResource.chart.label, Sentiment.Negative,
                        starting.year, starting.month, starting.date, ending.date);
                if (!negativeQueries.isEmpty()) {
                    total.addAndGet(negativeQueries.size());

                    for (PublicOpinion.ArticleQuery articleQuery : negativeQueries) {
                        service.generateText(channel,
                                service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational),
                                articleQuery.query, articleQuery.query, new GenerativeOption(),
                                null, 0, null, null, false, false,
                                new GenerateTextListener() {
                                    @Override
                                    public void onGenerated(AIGCChannel channel, GenerativeRecord record) {
                                        PublicOpinion.ArticleQuery current = findArticleQuery(negativeQueries,
                                                record.query);
                                        if (null != current) {
                                            current.answer = record.answer.replaceAll(",", "，");
                                            result.add(current);
                                        }
                                        callback(total.get(), callCount, publicOpinion, result, listener);
                                    }

                                    @Override
                                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                                        callback(total.get(), callCount, publicOpinion, result, listener);
                                    }
                                });
                    }
                }

                List<PublicOpinion.ArticleQuery> positiveQueries = publicOpinion.makeEvaluatingArticleQueries(
                        chartResource.chart.label, Sentiment.Positive,
                        starting.year, starting.month, starting.date, ending.date);
                if (!positiveQueries.isEmpty()) {
                    total.addAndGet(positiveQueries.size());

                    for (PublicOpinion.ArticleQuery articleQuery : positiveQueries) {
                        service.generateText(channel,
                                service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational),
                                articleQuery.query, articleQuery.query, new GenerativeOption(),
                                null, 0, null, null, false, false,
                                new GenerateTextListener() {
                                    @Override
                                    public void onGenerated(AIGCChannel channel, GenerativeRecord record) {
                                        PublicOpinion.ArticleQuery current = findArticleQuery(positiveQueries,
                                                record.query);
                                        if (null != current) {
                                            current.answer = record.answer.replaceAll(",", "，");
                                            result.add(current);
                                        }
                                        callback(total.get(), callCount, publicOpinion, result, listener);
                                    }

                                    @Override
                                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                                        callback(total.get(), callCount, publicOpinion, result, listener);
                                    }
                                });
                    }
                }

                if (total.get() == 0) {
                    callback(0, callCount, publicOpinion, result, listener);
                }
            }
        }
    }*/

    /*private PublicOpinion.ArticleQuery findArticleQuery(List<PublicOpinion.ArticleQuery> queryList, String query) {
        for (PublicOpinion.ArticleQuery articleQuery : queryList) {
            if (articleQuery.query.equals(query)) {
                return articleQuery;
            }
        }
        return null;
    }

    private void callback(int targetTotal, AtomicInteger callCount, Module module,
                          List<PublicOpinion.ArticleQuery> articleQueryList, StageListener listener) {
        // 更新计数
        int count = callCount.incrementAndGet();
        if (targetTotal <= count) {
            // 结束
            ArrayList<String> list = new ArrayList<>();
            for (PublicOpinion.ArticleQuery articleQuery : articleQueryList) {
                list.add(articleQuery.output());
            }

            listener.onPerform(this, module, list);
        }
    }*/

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
