
function scoring(answers) {
    answers.forEach(function(el) {
        print(el.code)
    });
    
    var score = new ScaleScore();
    score.addItem("item", 123);

    return {
        result: "INFP",
        score: score
    }
}
