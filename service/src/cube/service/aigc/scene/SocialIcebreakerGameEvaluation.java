/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Person;
import cube.aigc.psychology.material.Thing;
import cube.aigc.psychology.material.Tree;
import cube.service.tokenizer.Tokenizer;
import cube.vision.Point;

import java.util.ArrayList;
import java.util.List;

public class SocialIcebreakerGameEvaluation extends Evaluation {

    private static final String sParaphrase = "火、树、人三元素的象征意义是：\n\n* 人：代表现实中的自己、社会角色、当下的状态。\n* 树：代表潜意识中的自我、生命力、成长史、内在的能量和稳定性。\n* 火：代表能量、动力、激情，但也象征毁灭、危险、焦虑或转化的渴望。";

    private PaintingFeatureSet paintingFeatureSet;

    private Tokenizer tokenizer;

    private Person person;
    private Tree tree;
    private Thing fire;

    public SocialIcebreakerGameEvaluation(long contactId, Painting painting, Tokenizer tokenizer) {
        super(contactId, painting);
        this.tokenizer = tokenizer;
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();

        this.person = this.painting.getPerson();
        this.tree = this.painting.getTree();
        this.fire = this.parseFire();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalSize(spaceLayout));
        results.add(this.evalPosition(spaceLayout));

        EvaluationReport report = new EvaluationReport(this.contactId, Theme.SocialIcebreakerGame,
                this.painting.getAttribute(), Reference.Normal, new PaintingConfidence(this.painting), results);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    private EvaluationFeature evalSize(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        double paintingArea = (double) spaceLayout.getPaintingArea();
        if (null != this.person) {
            double ratio = ((double) this.person.area) / paintingArea;
            Logger.d(this.getClass(), "#evalSize - person size ratio: " + ratio);
            String title = "";
            if (ratio > 0.15) {
                // 较大
                title = "人树火绘画人被画得较大";
            }
            else if (ratio < 0.05) {
                // 较小
                title = "人树火绘画人被画得较小";
            }
            else {
                // 适中
                title = "人树火绘画人被画得大小适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalSize - NO title: " + title);
            }
        }

        if (null != this.tree) {
            double ratio = ((double) this.tree.area) / paintingArea;
            Logger.d(this.getClass(), "#evalSize - tree size ratio: " + ratio);
            String title = "";
            if (ratio > 0.10) {
                // 较大
                title = "人树火绘画树木被画得较大";
            }
            else if (ratio < 0.085) {
                // 较小
                title = "人树火绘画树木被画得较小";
            }
            else {
                // 适中
                title = "人树火绘画树木被画得大小适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalSize - NO title: " + title);
            }
        }

        if (null != this.fire) {
            double ratio = ((double) this.fire.area) / paintingArea;
            Logger.d(this.getClass(), "#evalSize - fire size ratio: " + ratio);
            String title = "";
            if (ratio > 0.15) {
                // 较大
                title = "人树火绘画火焰被画得较大";
            }
            else if (ratio < 0.08) {
                // 较小
                title = "人树火绘画火焰被画得较小";
            }
            else {
                // 适中
                title = "人树火绘画火焰被画得大小适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalSize - NO title: " + title);
            }
        }

        return result;
    }

    private EvaluationFeature evalPosition(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        double p2t = 0;
        double p2tThreshold = 0;
        double p2f = 0;
        double p2fThreshold = 0;
        double t2f = 0;
        double t2fThreshold = 0;

        if (null != this.person && null != this.tree) {
            Point cP = this.person.boundingBox.getCenterPoint();
            Point cT = this.tree.boundingBox.getCenterPoint();

            p2t = cP.distance(cT);

            if (Math.abs(cP.x - cT.x) > Math.abs(cP.y - cT.y)) {
                p2tThreshold = this.person.boundingBox.width * 0.5;
            }
            else {
                p2tThreshold = this.person.boundingBox.height * 0.5;
            }
        }
        if (null != this.person && null != this.fire) {
            p2f = this.person.boundingBox.getCenterPoint().distance(this.fire.boundingBox.getCenterPoint());
        }
        if (null != this.tree && null != this.fire) {
            t2f = this.tree.boundingBox.getCenterPoint().distance(this.fire.boundingBox.getCenterPoint());
        }

        Logger.d(this.getClass(), "#evalPosition - distance: p2t/p2f/t2f - " + p2t + "/" + p2f + "/" + t2f);

        if (p2t > 0 && p2f > 0) {

        }

        return result;
    }

    private Thing parseFire() {
        Thing result = this.painting.getDrawingSet().get(Label.Bonfire);
        if (null == result) {
            result = this.painting.getDrawingSet().get(Label.Fire);
            if (null == result) {
                List<Thing> list = this.painting.getDrawingSet().getAll();
                if (!list.isEmpty()) {
                    result = list.get(0);
                }
            }
        }
        return result;
    }
}
