// Author: Ambrose Xu
// Date: 2024-06-14

var SCORE_MAP = {
    "A" : 1,
    "B" : 2,
    "C" : 3,
    "D" : 4,
    "E" : 5
}

var SomatizationItems = [1, 4, 12, 27, 40, 42, 48, 49, 52, 53, 56, 58];
var ObsessionItems = [3, 9, 10, 28, 38, 45, 46, 51, 55, 65];
var InterpersonalRelationItems = [6, 21, 34, 36, 37, 41, 61, 69, 73];
var DepressionItems = [5, 14, 15, 20, 22, 26, 29, 30, 31, 32, 54, 71, 79];
var AnxietyItems = [2, 17, 23, 33, 39, 57, 72, 78, 80, 86];
var HostileItems = [11, 24, 63, 67, 74, 81];
var HorrorItems = [13, 25, 47, 50, 70, 75, 82];
var ParanoidItems = [8, 18, 43, 68, 76, 83];
var PsychosisItems = [7, 16, 35, 62, 77, 84, 85, 87, 88, 90];
var SleepAndDietItems = [13, 25, 47, 50, 70, 75, 82];


function explain(score) {
    if (score <= 0.5) {
        return FactorLevel.None;
    } else if (score <= 1.5) {
        return FactorLevel.Slight;
    } else if (score <= 2.5) {
        return FactorLevel.Mild;
    } else if (score <= 3.5) {
        return FactorLevel.Moderate;
    } else {
        return FactorLevel.Severe;
    }
}

function makePrompt(list) {
    var prompt = new ScalePrompt();

    list.forEach(function(element) {
        var score = element["score"];
        var name = element["name"];
        var factor = element["factor"];

        score = 0.0001 + score;
        var data = {
            "score": score,
            "name": name,
            "factor": factor
        };

        if (score <= 2.0) {
            if (factor === 'InterpersonalRelation' || factor === 'SleepAndDiet') {
                data["description"] = '无明显' + name + '的描述和表现';
                data["suggestion"] = '无明显' + name + '的建议';
            } else {
                data["description"] = '无明显' + name + '症状的描述和表现';
                data["suggestion"] = '无明显' + name + '症状的建议';
            }
        } else if (score <= 2.9) {
            if (factor === 'InterpersonalRelation' || factor === 'SleepAndDiet') {
                data["description"] = '有较轻' + name + '的描述和表现';
                data["suggestion"] = '有较轻' + name + '的建议';
            } else {
                data["description"] = '有较轻' + name + '症状的描述和表现';
                data["suggestion"] = '有较轻' + name + '症状的建议';
            }
        } else if (score <= 3.8) {
            if (factor === 'InterpersonalRelation' || factor === 'SleepAndDiet') {
                data["description"] = '有' + name + '的描述和表现';
                data["suggestion"] = '有' + name + '的建议';
            } else {
                data["description"] = '有' + name + '倾向的描述和表现';
                data["suggestion"] = '有' + name + '倾向的建议';
            }
        } else {
            if (factor === 'InterpersonalRelation' || factor === 'SleepAndDiet') {
                data["description"] = '有明显' + name + '的描述和表现';
                data["suggestion"] = '有明显' + name + '的建议';
            } else {
                data["description"] = '有明显' + name + '倾向的描述和表现';
                data["suggestion"] = '有明显' + name + '倾向的建议';
            }
        }

        prompt.addPrompt(data);
    });

    return prompt;
}


function scoring(answers) {
    var index = 0;
    var sum = 0.0;


    for (var i = 0; i < SomatizationItems.length; ++i) {
        index = SomatizationItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Somatization = sum / SomatizationItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < ObsessionItems.length; ++i) {
        index = ObsessionItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Obsession = sum / ObsessionItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < InterpersonalRelationItems.length; ++i) {
        index = InterpersonalRelationItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_InterpersonalRelation = sum / InterpersonalRelationItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < DepressionItems.length; ++i) {
        index = DepressionItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Depression = sum / DepressionItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < AnxietyItems.length; ++i) {
        index = AnxietyItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Anxiety = sum / AnxietyItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < HostileItems.length; ++i) {
        index = HostileItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Hostile = sum / HostileItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < HorrorItems.length; ++i) {
        index = HorrorItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Horror = sum / HorrorItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < ParanoidItems.length; ++i) {
        index = ParanoidItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Paranoid = sum / ParanoidItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < PsychosisItems.length; ++i) {
        index = PsychosisItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Psychosis = sum / PsychosisItems.length + 0.0;


    sum = 0;
    for (var i = 0; i < SleepAndDietItems.length; ++i) {
        index = SleepAndDietItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_SleepAndDiet = sum / SleepAndDietItems.length + 0.0;


    var score = new ScaleScore();
    score.addItem('Somatization', '躯体化症状', T_Somatization, explain(T_Somatization));
    score.addItem('Obsession', '强迫症状', T_Obsession, explain(T_Obsession));
    score.addItem('InterpersonalRelation', '人际关系敏感', T_InterpersonalRelation, explain(T_InterpersonalRelation));
    score.addItem('Depression', '抑郁症状', T_Depression, explain(T_Depression));
    score.addItem('Anxiety', '焦虑症状', T_Anxiety, explain(T_Anxiety));
    score.addItem('Hostile', '敌对性症状', T_Hostile, explain(T_Hostile));
    score.addItem('Horror', '恐怖症状', T_Horror, explain(T_Horror));
    score.addItem('Paranoid', '偏执症状', T_Paranoid, explain(T_Paranoid));
    score.addItem('Psychosis', '精神病性症状', T_Psychosis, explain(T_Psychosis));
    score.addItem('SleepAndDiet', '睡眠及饮食问题', T_SleepAndDiet, explain(T_SleepAndDiet));

    // 计算常模
    var desc = [
        '* ' + explain(T_Somatization).prefix + '躯体化症状\n\n',
        '* ' + explain(T_Obsession).prefix + '强迫症状\n\n',
        '* ' + explain(T_InterpersonalRelation).prefix + '人际关系敏感\n\n',
        '* ' + explain(T_Depression).prefix + '抑郁症状\n\n',
        '* ' + explain(T_Anxiety).prefix + '焦虑症状\n\n',
        '* ' + explain(T_Hostile).prefix + '敌对性症状\n\n',
        '* ' + explain(T_Horror).prefix + '恐怖症状\n\n',
        '* ' + explain(T_Paranoid).prefix + '偏执症状\n\n',
        '* ' + explain(T_Psychosis).prefix + '精神病性症状\n\n',
        '* ' + explain(T_SleepAndDiet).prefix + '睡眠及饮食问题\n\n'
    ];

    var prompt = makePrompt([{
        "score": T_Somatization,
        "factor": 'Somatization',
        "name": '躯体化',
    }, {
        "score": T_Obsession,
        "factor": 'Obsession',
        "name": '强迫',
    }, {
        "score": T_InterpersonalRelation,
        "factor": 'InterpersonalRelation',
        "name": '人际关系敏感',
    }, {
        "score": T_Depression,
        "factor": 'Depression',
        "name": '抑郁',
    }, {
        "score": T_Anxiety,
        "factor": 'Anxiety',
        "name": '焦虑',
    }, {
        "score": T_Hostile,
        "factor": 'Hostile',
        "name": '敌对',
    }, {
        "score": T_Horror,
        "factor": 'Horror',
        "name": '恐怖',
    }, {
        "score": T_Paranoid,
        "factor": 'Paranoid',
        "name": '偏执',
    }, {
        "score": T_Psychosis,
        "factor": 'Psychosis',
        "name": '精神病性',
    }, {
        "score": T_SleepAndDiet,
        "factor": 'SleepAndDiet',
        "name": '睡眠及饮食问题',
    }]);

    return {
        content: desc.join(''),
        score: score,
        prompt: prompt
    }
}
