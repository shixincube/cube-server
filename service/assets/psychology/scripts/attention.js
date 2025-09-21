
function calc(attribute, scores, factorSet, reference) {
    var nScore = 0;
    var fDelta = 0;
    var bDepression = false;
    var fDepressionScore = 0;
    var bSenseOfSecurity = false;
    var bStress = false;
    var bAnxiety = false;
    var bObsession = false;
    var bOptimism = false;
    var bPessimism = false;

    for (var i = 0; i < scores.length; ++i) {
        var score = scores[i];

        switch (score.indicator.code) {
            case Indicator.Psychosis.code:
                if (score.positiveScore > 0.9) {
                    nScore += 4;
                    Logger.d('attention.js', 'Attention: Psychosis +4');
                } else if (score.positiveScore > 0.6) {
                    nScore += 3;
                    Logger.d('attention.js', 'Attention: Psychosis +3');
                } else if (score.positiveScore > 0.3) {
                    nScore += 2;
                    Logger.d('attention.js', 'Attention: Psychosis +2');
                }
                break;
            case Indicator.SocialAdaptability.code:
                fDelta = score.negativeScore - score.positiveScore;
                if (fDelta > 0) {
                    if (fDelta > 0.9) {
                        nScore += 2;
                        Logger.d('attention.js', 'Attention: SocialAdaptability +2');
                    } else if (fDelta >= 0.5) {
                        nScore += 1;
                        Logger.d('attention.js', 'Attention: SocialAdaptability +1');
                    }
                }
                break;
            case Indicator.Depression.code:
                fDepressionScore = score.positiveScore - score.negativeScore;
                if (fDepressionScore >= 1.2) {
                    bDepression = true;
                    nScore += 3;
                    Logger.d('attention.js', 'Attention: Depression +3');
                } else if (fDepressionScore > 0.8) {
                    bDepression = true;
                    nScore += 2;
                    Logger.d('attention.js', 'Attention: Depression +2');
                } else if (fDepressionScore > 0.4) {
                    bDepression = true;
                    nScore += 1;
                    Logger.d('attention.js', 'Attention: Depression +1');
                } else if (fDepressionScore > 0) {
                    bDepression = true;
                    Logger.d('attention.js', 'Attention: Depression 0');
                } else if (fDepressionScore < 0) {
                    nScore -= 1;
                    Logger.d('attention.js', 'Attention: Depression -1');
                }
                break;
            case Indicator.SenseOfSecurity.code:
                if (score.negativeScore - score.positiveScore > 0.7) {
                    bSenseOfSecurity = true;
                    nScore += 1;
                    Logger.d('attention.js', 'Attention: SenseOfSecurity +1');
                }
                break;
            case Indicator.Stress.code:
                if (score.positiveScore - score.negativeScore > 0.5) {
                    bStress = true;
                }
                break;
            case Indicator.Anxiety.code:
                fDelta = score.positiveScore - score.negativeScore;
                if (fDelta > 1.5) {
                    bAnxiety = true;
                    nScore += 2;
                    Logger.d('attention.js', 'Attention: Anxiety +2');
                } else if (fDelta > 0.8) {
                    bAnxiety = true;
                    nScore += 1;
                    Logger.d('attention.js', 'Attention: Anxiety +1');
                } else if (fDelta > 0) {
                    bAnxiety = true;
                    Logger.d('attention.js', 'Attention: Anxiety 0');
                } else if (fDelta < 0) {
                    nScore -= 1;
                    Logger.d('attention.js', 'Attention: Anxiety -1');
                }
                break;
            case Indicator.Obsession.code:
                if (score.positiveScore - score.negativeScore > 0.8) {
                    bObsession = true;
                    nScore += 1;
                    Logger.d('attention.js', 'Attention: Obsession +1');
                }
                break;
            case Indicator.Optimism.code:
                if (score.positiveScore - score.negativeScore > 1.0) {
                    bOptimism = true;
                    nScore -= 1;
                    Logger.d('attention.js', 'Attention: Optimism -1');
                } else if (score.positiveScore - score.negativeScore > 0.5) {
                    bOptimism = true;
                }
                break;
            case Indicator.Pessimism.code:
                if (score.positiveScore - score.negativeScore > 0.1) {
                    bPessimism = true;
                }
                break;
            case Indicator.Unknown.code:
                // 绘图未被识别
                nScore = 4;
                Logger.d('attention.js', 'Attention: Unknown =4');
                break;
            default:
                break;
        }
    }

    var rawScore = nScore;
    Logger.d('attention.js', "Raw Score: " + nScore);

    if (bDepression && bSenseOfSecurity) {
        nScore += 1;
        Logger.d('attention.js', "(depression && senseOfSecurity)");
    }
    else if (bDepression && bStress) {
        nScore += 1;
        Logger.d('attention.js', "(depression && stress)");
    }
    else if (bDepression && bAnxiety) {
        nScore += 1;
        Logger.d('attention.js', "(depression && anxiety)");
    }

    if (!bDepression && !bAnxiety) {
        nScore -= 1;
        Logger.d('attention.js', "(!depression && !anxiety)");
    }

    if (nScore > rawScore && rawScore == 4) {
        Logger.d('attention.js', "Fix score " + nScore + " -> " + rawScore);
        nScore = 4;
    }

    Logger.d('attention.js', "Calc score: " + nScore + " - " +
        "depression:" + bDepression + " | " +
        "senseOfSecurity:" + bSenseOfSecurity + " | " +
        "stress:" + bStress + " | " +
        "anxiety:" + bAnxiety + " | " +
        "obsession:" + bObsession + " | " +
        "optimism:" + bOptimism + " | " +
        "pessimism:" + bPessimism);

    // 根据 strict 修正
    if (attribute.strict) {
        if (bOptimism) {
            if (fDepressionScore >= 0.4) {
                nScore -= 1;
                Logger.d('attention.js', "Attention: strict -> Optimism -1");
            }
        }
    }

    // Projection
    var projectionScore = nScore;
    // 回滚条件
    var rollbackState = fDepressionScore < 1.0 && bOptimism;
    Logger.d('attention.js', "Attention: Rollback state: " + rollbackState + " - " + fDepressionScore);

    // 通过 FactorSet 修正
    if (null != factorSet) {
        Logger.d('attention.js', "Attention: Fix with the factor set data, total: " + factorSet.symptomFactor.total);
        if (factorSet.symptomFactor.total > 160) {
            Logger.d('attention.js', "Attention: FactorSet symptom total: " + factorSet.symptomFactor.total);
            nScore += 1;
        }

        if (factorSet.symptomFactor.obsession > 3) {
            Logger.d('attention.js', "Attention: FactorSet symptom (obsession > 3)");
            nScore += 1;
        }
        else if (factorSet.symptomFactor.interpersonal > 3) {
            Logger.d('attention.js', "Attention: FactorSet symptom (interpersonal > 3)");
            nScore += 1;
        }

        if (factorSet.symptomFactor.depression > 2 && factorSet.symptomFactor.anxiety > 2) {
            Logger.d('attention.js', "Attention: FactorSet symptom (depression > 2 && anxiety > 2)");
            nScore += 1;
        }
        else if (factorSet.symptomFactor.obsession > 2 && factorSet.symptomFactor.interpersonal > 2 &&
            factorSet.symptomFactor.anxiety > 2) {
            Logger.d('attention.js', "Attention: FactorSet symptom (obsession > 2 && interpersonal > 2 && anxiety > 2)");
            nScore += 1;
        }
    }

    // 根据年龄就行修正
    if (attribute.age <= 16) {
        if (rollbackState) {
            nScore = projectionScore;
            nScore -= 2;
            Logger.d('attention.js', "Attention: (age <= 16) roll back -=2");

            if (null != factorSet) {
                // 回滚
                if (factorSet.symptomFactor.total > 160) {
                    nScore -= 1;
                }

                if (factorSet.symptomFactor.obsession > 3 || factorSet.symptomFactor.interpersonal > 3) {
                    nScore -= 1;
                }
            }
        }
    }
    else if (attribute.age > 20 && attribute.age < 35) {
        if (nScore >= 5) {
            nScore -= 1;
            Logger.d('attention.js', "Attention: (age > 20) -=1");
        }
    }
    else if (attribute.age >= 35 && attribute.age <= 50) {
        if (nScore >= 5) {
            nScore -= 2;
            Logger.d('attention.js', "Attention: (age >= 35) -=2");
        }
    }
    else if (attribute.age > 50) {
        if (nScore >= 5) {
            nScore = 3;
        }
        else if (nScore >= 4) {
            nScore = 2;
        }
    }

    var attention = Attention.NoAttention;
    var fixReference = reference;
    var additionScale = "";

    Logger.d('attention.js', 'Score: ' + nScore);

    if (nScore > 0) {
        if (nScore >= 5) {
            attention = Attention.SpecialAttention;
            fixReference = Reference.Abnormal;
            Logger.d('attention.js', "Attention: Fix reference to Abnormal (score >= 5)");
        }
        else if (nScore >= 4) {
            attention = Attention.FocusedAttention;
        }
        else {
            if (nScore == 1 && fixReference.name == Reference.Normal.name) {
                Logger.d('attention.js', "Attention: Reference is normal and score is 1");
                attention = Attention.NoAttention;
            }
            else {
                attention = Attention.GeneralAttention;
            }
        }
    }

    if (nScore >= 4 || fixReference.name == Reference.Abnormal.name) {
        additionScale = "SCL-90";
    }

    if (fixReference.name == Reference.Abnormal.name) {
        if (attribute.age <= 11) {
            // 降级
            if (attention.level == Attention.SpecialAttention.level) {
                // 调整为重点关注
                attention = Attention.FocusedAttention;
                Logger.d('attention.js', "Attention: Focused attention");
            }
            else if (attention.level == Attention.FocusedAttention.level) {
                // 调整为一般关注
                attention = Attention.GeneralAttention;
            }
        }
        else {
            if (attention.level == Attention.NoAttention.level && nScore > 0) {
                // 如果非模态，将非关注标注为一般关注
                attention = Attention.GeneralAttention;
                Logger.d('attention.js', "Attention: General attention");
            }

            if (attribute.age < 20) {
                if (null != factorSet && factorSet.symptomFactor.total > 180 && !rollbackState) {
                    if (attention.level == Attention.GeneralAttention.level) {
                        attention = Attention.FocusedAttention;
                        Logger.d('attention.js', "Attention (age<20 && factor>190): Focused attention");
                    }
                }
            }
            else {
                // 细化年龄分段
                if (null != factorSet && factorSet.symptomFactor.total > 160 && !rollbackState && nScore > 3) {
                    if (attention.level == Attention.GeneralAttention.level) {
                        attention = Attention.FocusedAttention;
                        Logger.d('attention.js', "Attention (age>20 && factor>160): Focused attention");
                    }
                }
            }
        }
    }

    Logger.i('attention.js', "Attention: " + attention.level + " - " + attention.description);

    return {
        "attention": attention,
        "reference": fixReference,
        "additionScale": additionScale
    };
}
