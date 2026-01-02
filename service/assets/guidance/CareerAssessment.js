// Author: Ambrose Xu
const R = {
    "2": 1,
    "13": 1,
    "22": 1,
    "36": 1,
    "43": 1,
    "14": -1,
    "23": -1,
    "44": -1,
    "47": -1,
    "48": -1
};

const I = {};


const CareerComparisonTable = {};

function parseCareerComparisonTable(type) {
    var desc = '';

    for (const key in CareerComparisonTable) {
        var count = 0;
        for (var i = 0; i < type.length; ++i) {
            var T = type[i];
            if (key.indexOf(T) >= 0) {
                ++count;
            }
        }

        if (count === 3) {
            desc = CareerComparisonTable[key];
            return desc;
        }
    }
    return desc;
}

function main(args) {
    const R_score = {
        "name": 'R',
        "yes": 0,
        "no": 0
    };
    const I_score = {
        "name": 'I',
        "yes": 0,
        "no": 0
    };
    const A_score = {
        "name": 'A',
        "yes": 0,
        "no": 0
    };
    const S_score = {
        "name": 'S',
        "yes": 0,
        "no": 0
    };
    const E_score = {
        "name": 'E',
        "yes": 0,
        "no": 0
    };
    const C_score = {
        "name": 'C',
        "yes": 0,
        "no": 0
    };

    const description = [];
    for (var i = 0; i < args.length; ++i) {
        var question = args[i];
        var answer = question.getAnswer();
        Logger.d('CareerAssessment.js', 'question: ' + question.question + ' -> ' + answer.code);

        if (R.hasOwnProperty(question.sn)) {
            var score = R[question.sn];
            if (score > 0) {
                R_score["yes"] += 1;
            } else {
                R_score["no"] += 1;
            }
        } else if (I.hasOwnProperty(question.sn)) {
            var score = I[question.sn];
            if (score > 0) {
                I_score["yes"] += 1;
            } else {
                I_score["no"] += 1;
            }
        }
    }

    var yesList = [ R_score, I_score, A_score, S_score, E_score, C_score ];
    yesList.sort(function(a, b) {
        return b["yes"] - a["yes"];
    });

    var yesType = [yesList[0]["name"], yesList[1]["name"], yesList[2]["name"]];

    var yesDesc = parseCareerComparisonTable(yesType);

    const evaluationResult = new EvaluationResult('职业兴趣评测');
    evaluationResult.setDescription(description.join(''));
    return evaluationResult;
}
