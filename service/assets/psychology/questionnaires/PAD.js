// Author: Ambrose Xu
// Date: 2024-10-10

var SCORE_MAP = {
    "A" : -4,
    "B" : -3,
    "C" : -2,
    "D" : -1,
    "E" : 0,
    "F" : 1,
    "G" : 2,
    "H" : 3,
    "I" : 4
};

function scoring(answers) {
    var scoreList = [];

    answers.forEach(function(el) {
        var s = SCORE_MAP[el.code];
        scoreList.push(s);
    });

    var P = (scoreList[0] - scoreList[3] + scoreList[6] - scoreList[9]) / 16;
    var A = (-scoreList[1] + scoreList[4] - scoreList[7] + scoreList[10]) / 16;
    var D = (scoreList[2] - scoreList[5] + scoreList[8] - scoreList[11]) / 16;

    var score = new ScaleScore();
    score.addItem('P', 'P值', P, P > 0 ? FactorLevel.Severe : FactorLevel.Mild);
    score.addItem('A', 'A值', A, A > 0 ? FactorLevel.Severe : FactorLevel.Mild);
    score.addItem('D', 'D值', D, D > 0 ? FactorLevel.Severe : FactorLevel.Mild);

    var desc = [];
    if (P > 0 && A > 0 && D > 0) {
        desc.push('高兴的');
    } else if (P <= 0 && A <= 0 && D <= 0) {
        desc.push('无聊的');
    } else if (P > 0 && A > 0 && D <= 0) {
        desc.push('依赖的');
    } else if (P <= 0 && A <= 0 && D > 0) {
        desc.push('蔑视的');
    } else if (P > 0 && A <= 0 && D > 0) {
        desc.push('放松的');
    } else if (P <= 0 && A > 0 && D <= 0) {
        desc.push('焦虑的');
    } else if (P > 0 && A <= 0 && D <= 0) {
        desc.push('温顺的');
    } else if (P <= 0 && A > 0 && D > 0) {
        desc.push('敌意的');
    } else {
        desc.push('一般的');
    }

    return {
        content: desc.join(''),
        score: score
    }
}
