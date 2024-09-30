
function scoring(answers) {
    answers.forEach(function(el) {
        print(el.code)
    });

    var score = new ScaleScore();
    score.addItem('Name', 'DisplayName', 5.5, FactorLevel.None);

    return {
        result: "",
        score: score
    }
}
