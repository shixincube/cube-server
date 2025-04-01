// Author: Ambrose Xu
// Date: 2025-03-31

function explain(score) {
    if (score >= 29) {
        return {
            "level": FactorLevel.Severe,
            "desc": '可能为严重焦虑'
        };
    } else if (score >= 21) {
        return {
            "level": FactorLevel.Moderate,
            "desc": '有明显焦虑'
        };
    } else if (score >= 14) {
        return {
            "level":  FactorLevel.Mild,
            "desc": '肯定有焦虑'
        };
    } else if (score >= 7) {
        return {
            "level": FactorLevel.Slight,
            "desc": '可能有焦虑'
        };
    } else {
        return {
            "level": FactorLevel.None,
            "desc": '没有焦虑症状'
        };
    }
}

function scoring(answers) {
    var total = 0;
    answers.forEach(function(el) {
        var s = parseInt(el.code);
        total += s;
    });

    var result = explain(total);

    var score = new ScaleScore();
    score.addItem('total', '总分', total, result.level);

    return {
        content: result.desc,
        score: score
    }
}
