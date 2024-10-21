// Author: Ambrose Xu
// Date: 2024-10-20

var SCORE_MAP = {
    "A" : 1,
    "B" : 2,
    "C" : 3,
    "D" : 4,
    "E" : 5
};

var PositiveAffectArray = [1, 3, 5, 9, 10, 12, 14, 16, 17, 19];
var NegativeAffectArray = [2, 4, 6, 7, 8, 11, 13, 15, 18, 20];

/* (10)  15  [25  30  35]  45  (50) */
var mid = 30;

function explain(score) {
    if (score < 25) {
        return FactorLevel.Mild;
    } else if (score >= 25 && score <= 35) {
        return FactorLevel.Moderate;
    } else {
        return FactorLevel.Severe;
    }
}

function scoring(answers) {
    var positive = 0;
    var negative = 0;

    PositiveAffectArray.forEach(function(value) {
        var answer = answers[value - 1];
        positive += SCORE_MAP[answer.code];
    });

    NegativeAffectArray.forEach(function(value) {
        var answer = answers[value - 1];
        negative += SCORE_MAP[answer.code];
    });

    var desc = [];
    if (positive > negative) {
        desc.push('积极情绪高于消极情绪。\n\n');
    } else if (positive < negative) {
        desc.push('积极情绪低于消极情绪。\n\n');
    } else {
        desc.push('积极情绪与消极情绪得分相同。\n\n');
    }

    if (positive > mid) {
        desc.push('* 个体精力旺盛，能全神贯注和快乐的情绪状况。\n');
    } else {
        desc.push('* 个体情绪状况淡漠。\n');
    }

    if (negative > mid) {
        desc.push('* 个体主观感觉困惑，痛苦的情绪状态。\n');
    } else {
        desc.push('* 个体情绪状况镇定。\n');
    }

    var score = new ScaleScore();
    score.addItem('Positive', '正性情绪分', positive, explain(positive));
    score.addItem('Negative', '负性情绪分', negative, explain(negative));

    return {
        content: desc.join(''),
        score: score
    }
}
