
function main(args) {
    const evaluationResult = new EvaluationResult();

    for (var i = 0; i < args.length; ++i) {
        var question = args[i];
        Logger.d('MINI_evaluation.js', 'question: ' + question.sn);
    }

    return evaluationResult;
}
