
function scoring(answers) {
    answers.forEach(function(el) {
        print(el.code)
    });
    
    var score = new ScaleScore();

    return {
        result: "",
        score: score
    }
}
