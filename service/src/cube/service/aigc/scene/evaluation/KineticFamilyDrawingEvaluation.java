/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene.evaluation;

import cell.util.log.Logger;
import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.Person;

import java.util.ArrayList;
import java.util.List;

public class KineticFamilyDrawingEvaluation extends Evaluation {

    public enum FamilyMemberRole {
        Child("child"),

        Father("father"),

        Mother("mother"),

        Brother("brother"),

        YoungerBrother("younger_brother"),

        Sister("sister"),

        YoungerSister("younger_sister"),

        Grandpa("grandpa"),

        Grandma("grandma"),

        MaternalGrandpa("maternal_grandpa"),

        MaternalGrandma("maternal_grandma"),

        None("none"),

        ;

        public final String code;

        FamilyMemberRole(String code) {
            this.code = code;
        }

        public boolean isKid() {
            switch (this) {
                case Child:
                case Brother:
                case YoungerBrother:
                case Sister:
                case YoungerSister:
                    return true;
                default:
                    return false;
            }
        }

        public final static FamilyMemberRole parse(String nameOrCode) {
            for (FamilyMemberRole role : FamilyMemberRole.values()) {
                if (role.code.equalsIgnoreCase(nameOrCode) ||
                    role.name().equalsIgnoreCase(nameOrCode)) {
                    return role;
                }
            }

            return FamilyMemberRole.None;
        }
    }

    public KineticFamilyDrawingEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalPerson(spaceLayout));

        return null;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return null;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        List<Person> personList = this.painting.getPersons();
        if (null == personList) {
            Logger.w(this.getClass(), "#evalPerson - No person in the painting: " + this.contactId);
            return null;
        }



        return result;
    }
}
