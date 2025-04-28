/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cell.util.log.Logger;
import cube.aigc.Flowable;
import cube.aigc.Module;
import cube.auth.AuthToken;
import cube.common.entity.QuestionAnswer;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.service.aigc.module.task.QueryUser;

import java.util.ArrayList;
import java.util.List;

public class AppManager implements Module {

    private AIGCService service;

    public AppManager(AIGCService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "AppManager";
    }

    @Override
    public List<String> getMatchingWords() {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Flowable match(AuthToken token, String query) {
        final List<QuestionAnswer> questionAnswerList = new ArrayList<>();
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                questionAnswerList.addAll(questionAnswers);
                synchronized (questionAnswerList) {
                    questionAnswerList.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (questionAnswerList) {
                    questionAnswerList.notify();
                }
            }
        });

        if (!success) {
            Logger.w(this.getClass(), "#match - search flow task failed");
            return null;
        }

        synchronized (questionAnswerList) {
            try {
                questionAnswerList.wait(60 * 1000);
            } catch (InterruptedException e) {
                Logger.w(this.getClass(), "#match - search flow task failed", e);
            }
        }

        for (QuestionAnswer qa : questionAnswerList) {
            if (qa.getScore() < 0.85) {
                continue;
            }

            if (qa.getAnswers().get(0).equalsIgnoreCase(QueryUser.TASK_NAME)) {
                return new QueryUser(this.service, token, query);
            }
        }

        return null;
    }
}
