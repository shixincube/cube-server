// Author: Ambrose Xu

function main(args) {
    var evaluationResult = new EvaluationResult('抑郁发作');

    var trueCount = 0;
    var truePastTwoWeeksCount = 0;
    var trueInThePastCount = 0;

    var A4TwoWeeks = false;
    var A4Past = false;
    var A5 = false;

    for (var i = 0; i < args.length; ++i) {
        var question = args[i];

        if (question.sn.startsWith('A1') || question.sn.startsWith('A2') || question.sn.startsWith('A3')) {
            if (question.isAnswerGroup()) {
                var answer = question.getGroupAnswer('A');
                if (null != answer) {
                    if (answer.code === 'true') {
                        truePastTwoWeeksCount++;
                    }
                    Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ', A: ' + answer.code);
                }

                answer = question.getGroupAnswer('B');
                if (null != answer) {
                    if (answer.code === 'true') {
                        trueInThePastCount++;
                    }
                    Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ', B: ' + answer.code);
                }
            }
            else {
                var answer = question.getAnswer();
                if (null != answer) {
                    if (answer.code === 'true') {
                        trueCount++;
                    }
                    Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ' - ' + answer.code);
                }
            }
        }
        else if (question.sn.startsWith('A4')) {
            var answer = question.getGroupAnswer('A');
            if (null != answer) {
                if (answer.code === 'true') {
                    A4TwoWeeks = true;
                }
                Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ', A: ' + answer.code);
            }

            answer = question.getGroupAnswer('B');
            if (null != answer) {
                if (answer.code === 'true') {
                    A4Past = true;
                }
                Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ', B: ' + answer.code);
            }
        }
        else if (question.sn.startsWith('A4')) {
            var answer = question.getAnswer();
            if (null != answer) {
                if (answer.code === 'true') {
                    A5 = true;
                }
                Logger.d('MINI_A_Evaluation.js', 'question: ' + question.sn + ' - ' + answer.code);
            }
        }
    }

    if (((trueCount + truePastTwoWeeksCount) >= 5 || (trueCount + trueInThePastCount) >= 5)
        && (A4TwoWeeks || A4Past)) {
        evaluationResult.setResult(true);
    }
    else {
        evaluationResult.setResult(false);
    }

    if ((trueCount + truePastTwoWeeksCount) >= 5) {
        evaluationResult.addItem('当前', true);
    }
    else {
        evaluationResult.addItem('当前', false);
    }

    if ((trueCount + trueInThePastCount) >= 5) {
        evaluationResult.addItem('既往', true);
    }
    else {
        evaluationResult.addItem('既往', false);
    }

    evaluationResult.addItem('复发', A5);

    return evaluationResult;
}
