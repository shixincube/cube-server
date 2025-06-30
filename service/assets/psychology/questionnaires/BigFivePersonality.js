// Author: Ambrose Xu
// Date: 2024-09-28

var SCORE_MAP = {
    "1" : 1.0,
    "2" : 3.0,
    "3" : 5.5,
    "4" : 8.0,
    "5" : 10.0
};
var SCORE_REVERSE_MAP = {
    "1" : 10.0,
    "2" : 8.0,
    "3" : 5.5,
    "4" : 3.0,
    "5" : 1.0
}

var ObligingnessArray = [4, 29, 34, 49];
var ObligingnessReverseArray = [9, 14, 19, 24, 39, 44, 54, 59];

var ConscientiousnessArray = [5, 10, 20, 25, 35, 40, 50, 60];
var ConscientiousnessReverseArray = [15, 30, 45, 55];

var ExtraversionArray = [2, 7, 17, 22, 32, 37, 47, 52];
var ExtraversionReverseArray = [12, 27, 42, 57];

var AchievementArray = [3, 8, 13, 38, 43, 53, 58];
var AchievementReverseArray = [18, 23, 28, 33, 48];

var NeuroticismArray = [6, 11, 21, 26, 36, 41, 51, 56];
var NeuroticismReverseArray = [1, 16, 31, 46];

var HighScore = 7.0;
var LowScore = 4.0;

function explain(score) {
    if (score <= 3.5) {
        return FactorLevel.Mild;
    } else if (score < 7.5) {
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

        var data = {
            "score": score,
            "name": name,
            "factor": factor
        };

        if (score >= HighScore) {
            data["description"] = '高分' + name + '表现';
        }
        else if (score <= LowScore) {
            data["description"] = '低分' + name + '表现';
        }
        else {
            data["description"] = name + '一般的表现';
        }

        prompt.addPrompt(data);
    });

    return prompt;
}

function scoring(answers) {
    var desc = [];
    var score = new ScaleScore();
    var promptList = [];

    // 宜人性
    var obligingness = 0;
    ObligingnessArray.forEach(function(value) {
        var answer = answers[value - 1];
        obligingness += SCORE_MAP[answer.code];
    });
    ObligingnessReverseArray.forEach(function(value) {
        var answer = answers[value - 1];
        obligingness += SCORE_REVERSE_MAP[answer.code];
    });
    obligingness = obligingness / (ObligingnessArray.length + ObligingnessReverseArray.length);
    score.addItem("Obligingness", "宜人性", obligingness, explain(obligingness));
    promptList.push({
        "score": obligingness,
        "name": "宜人性",
        "factor": "Obligingness"
    });

    // 尽责性
    var conscientiousness = 0;
    ConscientiousnessArray.forEach(function(value) {
        var answer = answers[value - 1];
        conscientiousness += SCORE_MAP[answer.code];
    });
    ConscientiousnessReverseArray.forEach(function(value) {
        var answer = answers[value - 1];
        conscientiousness += SCORE_REVERSE_MAP[answer.code];
    });
    conscientiousness = conscientiousness / (ConscientiousnessArray.length + ConscientiousnessReverseArray.length);
    score.addItem("Conscientiousness", "尽责性", conscientiousness, explain(conscientiousness));
    promptList.push({
        "score": conscientiousness,
        "name": "尽责性",
        "factor": "Conscientiousness"
    });

    // 外向性
    var extraversion = 0;
    ExtraversionArray.forEach(function(value) {
        var answer = answers[value - 1];
        extraversion += SCORE_MAP[answer.code];
    });
    ExtraversionReverseArray.forEach(function(value) {
        var answer = answers[value - 1];
        extraversion += SCORE_REVERSE_MAP[answer.code];
    });
    extraversion = extraversion / (ExtraversionArray.length + ExtraversionReverseArray.length);
    score.addItem("Extraversion", "外向性", extraversion, explain(extraversion));
    promptList.push({
        "score": extraversion,
        "name": "外向性",
        "factor": "Extraversion"
    });

    // 进取性
    var achievement = 0;
    AchievementArray.forEach(function(value) {
        var answer = answers[value - 1];
        achievement += SCORE_MAP[answer.code];
    });
    AchievementReverseArray.forEach(function(value) {
        var answer = answers[value - 1];
        achievement += SCORE_REVERSE_MAP[answer.code];
    });
    achievement = achievement / (AchievementArray.length + AchievementReverseArray.length);
    score.addItem("Achievement", "进取性", achievement, explain(achievement));
    promptList.push({
        "score": achievement,
        "name": "进取性",
        "factor": "Achievement"
    });

    // 情绪性
    var neuroticism = 0;
    NeuroticismArray.forEach(function(value) {
        var answer = answers[value - 1];
        neuroticism += SCORE_MAP[answer.code];
    });
    NeuroticismReverseArray.forEach(function(value) {
        var answer = answers[value - 1];
        neuroticism += SCORE_REVERSE_MAP[answer.code];
    });
    neuroticism = neuroticism / (NeuroticismArray.length + NeuroticismReverseArray.length);
    score.addItem("Neuroticism", "情绪性", neuroticism, explain(neuroticism));
    promptList.push({
        "score": neuroticism,
        "name": "情绪性",
        "factor": "Neuroticism"
    });

    return {
        content: desc.join(''),
        score: score,
        prompt: makePrompt(promptList)
    }
}
