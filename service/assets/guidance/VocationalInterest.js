function main(args) {
    for (var i = 0; i < args.length; ++i) {
        var question = args[i];
        var answer = question.getAnswer();
        Logger.d('VocationalInterest.js', 'question: ' + question.question + ' -> ' + answer.code);
    }

    var evaluationResult = new EvaluationResult('职业兴趣评测测试');
    return evaluationResult;
}
