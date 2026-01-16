/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Person;
import cube.aigc.psychology.material.Thing;
import cube.aigc.psychology.material.Tree;

import java.util.ArrayList;
import java.util.List;

public class SocialIcebreakerGameEvaluation extends Evaluation {

    private PaintingFeatureSet paintingFeatureSet;

    private Person person;
    private Tree tree;
    private Thing fire;

    public SocialIcebreakerGameEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalPosition(spaceLayout));
        results.add(this.evalSize(spaceLayout));

        EvaluationReport report = null;
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    private EvaluationFeature evalPosition(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        this.person = this.painting.getPerson();
        this.tree = this.painting.getTree();
        this.fire = this.painting.getOther().get(Label.Bonfire);

        return result;
    }

    private EvaluationFeature evalSize(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        return result;
    }
}
