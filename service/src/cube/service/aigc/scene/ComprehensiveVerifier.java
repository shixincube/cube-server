/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.Comprehensive;
import cube.aigc.psychology.composition.Question;
import cube.aigc.psychology.composition.Scale;
import cube.common.entity.AIGCChannel;
import cube.service.aigc.scene.evaluation.SubconsciousRelationshipBetweenCoupleEvaluation;

import java.util.List;

public class ComprehensiveVerifier {

    private AIGCChannel channel;

    private ComprehensiveReport report;

    public ComprehensiveVerifier(AIGCChannel channel, ComprehensiveReport report) {
        this.channel = channel;
        this.report = report;
    }

    public boolean verifyParameters() {
        if (null == this.report.theme) {
            return false;
        }

        List<Comprehensive> comprehensiveList = this.report.comprehensives;
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenCouple:
                if (comprehensiveList.size() == 2) {
                    // 将选择输入量表
                    for (Comprehensive comprehensive : comprehensiveList) {
                        // 判断文件
                        if (!comprehensive.hasFileLabels()) {
                            Logger.e(this.getClass(), "#verifyParameters - No file: " + comprehensive.getName());
                            return false;
                        }

                        if (!comprehensive.hasScales()) {
                            Scale scale = Resource.getInstance().loadScaleByName("SRBC",
                                    this.channel.getAuthToken().getContactId());
                            if (null == scale) {
                                Logger.e(this.getClass(), "#verifyParameters - Can NOT find scale: SRBC");
                                return false;
                            }

                            for (String word : comprehensive.getChoices()) {
                                SubconsciousRelationshipBetweenCoupleEvaluation.SRBCWord srbcWord =
                                        SubconsciousRelationshipBetweenCoupleEvaluation.SRBCWord.parse(word);
                                scale.chooseAnswer(1, Integer.toString(srbcWord.sn));
                            }
                            comprehensive.addScale(scale);
                        }
                    }

                    Comprehensive comprehensive1 = comprehensiveList.get(0);
                    Comprehensive comprehensive2 = comprehensiveList.get(1);

                    boolean genderOk = false;
                    Attribute attribute1 = comprehensive1.getAttribute();
                    Attribute attribute2 = comprehensive2.getAttribute();
                    // 必须一男一女
                    if ((attribute1.isMale() && attribute2.isFemale()) ||
                            (attribute1.isFemale() && attribute2.isMale())) {
                        genderOk = true;
                    }

                    // 必须选择3个词
                    boolean scaleOk = false;
                    Scale scale1 = comprehensive1.getScale();
                    Question question1 = scale1.getQuestions().get(0);
                    Scale scale2 = comprehensive2.getScale();
                    Question question2 = scale2.getQuestions().get(0);
                    if (question1.getChosenAnswers().size() == 3 &&
                            question2.getChosenAnswers().size() == 3) {
                        scaleOk = true;
                    }

                    Logger.d(this.getClass(), "#verifyParameters - gender: " + genderOk + " , scale: " + scaleOk);

                    return genderOk && scaleOk;
                }
                else {
                    return false;
                }
            case KineticFamilyDrawing:
                if (comprehensiveList.size() >= 2) {
                    boolean kidOk = false;
                    boolean parentOk = false;

                }
                else {
                    return false;
                }
            default:
                return false;
        }
    }
}
