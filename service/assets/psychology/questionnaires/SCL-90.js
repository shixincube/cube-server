// Author: Ambrose Xu
// Date: 2024-06-14

var SCORE_MAP = {
    "A" : 0,
    "B" : 1,
    "C" : 2,
    "D" : 3,
    "E" : 4
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
        return '没有';
    } else if (score <= 1.5) {
        return '略有';
    } else if (score <= 2.5) {
        return '轻度';
    } else if (score <= 3.5) {
        return '中度';
    } else {
        return '重度';
    }
}


function scoring(answers) {
    var index = 0;
    var sum = 0;


    for (var i = 0; i < SomatizationItems.length; ++i) {
        index = SomatizationItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Somatization = sum / SomatizationItems.length;


    sum = 0;
    for (var i = 0; i < ObsessionItems.length; ++i) {
        index = ObsessionItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Obsession = sum / ObsessionItems.length;


    sum = 0;
    for (var i = 0; i < InterpersonalRelationItems.length; ++i) {
        index = InterpersonalRelationItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_InterpersonalRelation = sum / InterpersonalRelationItems.length;


    sum = 0;
    for (var i = 0; i < DepressionItems.length; ++i) {
        index = DepressionItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Depression = sum / DepressionItems.length;


    sum = 0;
    for (var i = 0; i < AnxietyItems.length; ++i) {
        index = AnxietyItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Anxiety = sum / AnxietyItems.length;


    sum = 0;
    for (var i = 0; i < HostileItems.length; ++i) {
        index = HostileItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Hostile = sum / HostileItems.length;


    sum = 0;
    for (var i = 0; i < HorrorItems.length; ++i) {
        index = HorrorItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Horror = sum / HorrorItems.length;


    sum = 0;
    for (var i = 0; i < ParanoidItems.length; ++i) {
        index = ParanoidItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Paranoid = sum / ParanoidItems.length;


    sum = 0;
    for (var i = 0; i < PsychosisItems.length; ++i) {
        index = PsychosisItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_Psychosis = sum / PsychosisItems.length;


    sum = 0;
    for (var i = 0; i < SleepAndDietItems.length; ++i) {
        index = SleepAndDietItems[i] - 1;
        var answer = answers[index];
        var score = SCORE_MAP[answer.code];
        sum += score;
    }
    var T_SleepAndDiet = sum / SleepAndDietItems.length;


    var score = new ScaleScore();
    score.addItem('T_Somatization', '躯体化症状', T_Somatization);
    score.addItem('T_Obsession', '强迫症状', T_Obsession);
    score.addItem('T_InterpersonalRelation', '人际关系问题', T_InterpersonalRelation);
    score.addItem('T_Depression', '抑郁症状', T_Depression);
    score.addItem('T_Anxiety', '焦虑症状', T_Anxiety);
    score.addItem('T_Hostile', '敌对性症状', T_Hostile);
    score.addItem('T_Horror', '恐怖症状', T_Horror);
    score.addItem('T_Paranoid', '偏执症状', T_Paranoid);
    score.addItem('T_Psychosis', '精神病性症状', T_Psychosis);
    score.addItem('T_SleepAndDiet', '睡眠及饮食问题', T_SleepAndDiet);

    // 计算常模
    var desc = [
        '* ' + explain(T_Somatization) + '躯体化症状\n\n',
        '* ' + explain(T_Obsession) + '强迫症状\n\n',
        '* ' + explain(T_InterpersonalRelation) + '人际关系问题\n\n',
        '* ' + explain(T_Depression) + '抑郁症状\n\n',
        '* ' + explain(T_Anxiety) + '焦虑症状\n\n',
        '* ' + explain(T_Hostile) + '敌对性症状\n\n',
        '* ' + explain(T_Horror) + '恐怖症状\n\n',
        '* ' + explain(T_Paranoid) + '偏执症状\n\n',
        '* ' + explain(T_Psychosis) + '精神病性症状\n\n',
        '* ' + explain(T_SleepAndDiet) + '睡眠及饮食问题\n\n'
    ];

    return {
        content: desc.join(''),
        score: score
    }
}
