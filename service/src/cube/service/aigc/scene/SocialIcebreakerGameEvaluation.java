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
                title = "人树火绘画人物被画得较大";
            }
            else if (ratio < 0.05) {
                // 较小
                title = "人树火绘画人物被画得较小";
            }
            else {
                // 适中
                title = "人树火绘画人物被画得大小适中";
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
        double p2tMinThreshold = 0;
        double p2tMaxThreshold = 0;
        double p2f = 0;
        double p2fMinThreshold = 0;
        double p2fMaxThreshold = 0;
        double t2f = 0;
        double t2fMinThreshold = 0;
        double t2fMaxThreshold = 0;

        if (null != this.person && null != this.tree) {
            Point cP = this.person.boundingBox.getCenterPoint();
            Point cT = this.tree.boundingBox.getCenterPoint();

            p2t = cP.distance(cT);

            if (Math.abs(cP.x - cT.x) > Math.abs(cP.y - cT.y)) {
                p2tMinThreshold = this.person.boundingBox.width * 0.5;
                p2tMaxThreshold = p2tMinThreshold + this.tree.boundingBox.width * 0.5;
            }
            else {
                p2tMinThreshold = this.person.boundingBox.height * 0.5;
                p2tMaxThreshold = p2tMinThreshold + this.tree.boundingBox.height * 0.5;
            }
        }

        if (null != this.person && null != this.fire) {
            Point cP = this.person.boundingBox.getCenterPoint();
            Point cF = this.fire.boundingBox.getCenterPoint();

            p2f = cP.distance(cF);

            if (Math.abs(cP.x - cF.x) > Math.abs(cP.y - cF.y)) {
                p2fMinThreshold = this.person.boundingBox.width * 0.5;
                p2fMaxThreshold = p2fMinThreshold + this.fire.boundingBox.width * 0.5;
            }
            else {
                p2fMinThreshold = this.person.boundingBox.height * 0.5;
                p2fMaxThreshold = p2fMinThreshold + this.fire.boundingBox.height * 0.5;
            }
        }

        if (null != this.tree && null != this.fire) {
            Point cT = this.tree.boundingBox.getCenterPoint();
            Point cF = this.fire.boundingBox.getCenterPoint();

            t2f = cT.distance(cF);

            if (Math.abs(cT.x - cF.x) > Math.abs(cT.y - cF.y)) {
                t2fMinThreshold = this.tree.boundingBox.width * 0.5;
                t2fMaxThreshold = t2fMinThreshold + this.fire.boundingBox.width * 0.5;
            }
            else {
                t2fMinThreshold = this.tree.boundingBox.height * 0.5;
                t2fMaxThreshold = t2fMinThreshold + this.fire.boundingBox.height * 0.5;
            }
        }

        Logger.d(this.getClass(), "#evalPosition - distance: p2t/p2f/t2f - " + p2t + "/" + p2f + "/" + t2f);

        if (p2t != 0) {
            String title = "";
            if (p2t > p2tMaxThreshold) {
                // 人树火绘画人物与树木距离较远
                title = "人树火绘画人物与树木距离较远";
            }
            else if (p2t < p2tMinThreshold) {
                // 人树火绘画人物与树木距离较近
                title = "人树火绘画人物与树木距离较近";
            }
            else {
                // 人树火绘画人物与树木距离适中
                title = "人树火绘画人物与树木距离适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalPosition - NO title: " + title);
            }
        }

        if (p2f != 0) {
            String title = "";
            if (p2f > p2fMaxThreshold) {
                // 人树火绘画人物与火焰距离较远
                title = "人树火绘画人物与火焰距离较远";
            }
            else if (p2f < p2fMinThreshold) {
                // 人树火绘画人物与火焰距离较近
                title = "人树火绘画人物与火焰距离较近";
            }
            else {
                // 人树火绘画人物与火焰距离适中
                title = "人树火绘画人物与火焰距离适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalPosition - NO title: " + title);
            }
        }

        if (t2f != 0) {
            String title = "";
            if (t2f > t2fMaxThreshold) {
                // 人树火绘画树木与火焰距离较远
                title = "人树火绘画树木与火焰距离较远";
            }
            else if (t2f < t2fMinThreshold) {
                // 人树火绘画树木与火焰距离较近
                title = "人树火绘画树木与火焰距离较近";
            }
            else {
                // 人树火绘画树木与火焰距离适中
                title = "人树火绘画树木与火焰距离适中";
            }

            String content = ContentTools.extract(title, this.tokenizer);
            if (null != content) {
                KeyFeature feature = new KeyFeature(title, content);
                result.addKeyFeature(feature);
            }
            else {
                Logger.w(this.getClass(), "#evalPosition - NO title: " + title);
            }
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
