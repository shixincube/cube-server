
var X12_QN = [1, 4, 8, 11, 15, 18, 22, 25, 30];
var X34_QN = [2, 5, 9, 12, 16, 19, 23, 27, 29, 31];
var X56_QN = [3, 6, 10, 13, 17, 20, 24, 26];
var X78_QN = [7, 14, 21, 28];

var Y12_QN = [35, 42, 52, 57];
var Y34_QN = [33, 38, 40, 43, 45, 49, 51, 53, 55, 60, 62];
var Y56_QN = [36, 41, 47, 58];
var Y78_QN = [32, 34, 37, 39, 44, 46, 48, 50, 54, 56, 59, 61];

var Z12_QN = [63, 74, 76, 79, 82, 85, 88, 91, 93];
var Z34_QN = [64, 66, 70, 81, 87];
var Z56_QN = [68, 72, 75, 77, 80, 83, 86, 89, 92];
var Z78_QN = [65, 67, 69, 71, 73, 78, 84, 90];

function scoring(answers) {
    var X1, X2, X3, X4, X5, X6, X7, X8;
    var Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8;
    var Z1, Z2, Z3, Z4, Z5, Z6, Z7, Z8;
    X1 = X2 = X3 = X4 = X5 = X6 = X7 = X8 = 0;
    Y1 = Y2 = Y3 = Y4 = Y5 = Y6 = Y7 = Y8 = 0;
    Z1 = Z2 = Z3 = Z4 = Z5 = Z6 = Z7 = Z8 = 0;
    var index = 0;

    var num = 0;
    for (var i = 0; i < X12_QN.length; ++i) {
        index = X12_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            X1 += 1;
        } else {
            X2 += 1;
        }
        num++;
    }

    for (var i = 0; i < X34_QN.length; ++i) {
        index = X34_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            X3 += 1;
        } else {
            X4 += 1;
        }
        num++;
    }

    for (var i = 0; i < X56_QN.length; ++i) {
        index = X56_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            X5 += 1;
        } else {
            X6 += 1;
        }
        num++;
    }

    for (var i = 0; i < X78_QN.length; ++i) {
        index = X78_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            X7 += 1;
        } else {
            X8 += 1;
        }
        num++;
    }

    for (var i = 0; i < Y12_QN.length; ++i) {
        index = Y12_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Y1 += 1;
        } else {
            Y2 += 1;
        }
        num++;
    }

    for (var i = 0; i < Y34_QN.length; ++i) {
        index = Y34_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Y3 += 1;
        } else {
            Y4 += 1;
        }
        num++;
    }

    for (var i = 0; i < Y56_QN.length; ++i) {
        index = Y56_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Y5 += 1;
        } else {
            Y6 += 1;
        }
        num++;
    }

    for (var i = 0; i < Y78_QN.length; ++i) {
        index = Y78_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Y7 += 1;
        } else {
            Y8 += 1;
        }
        num++;
    }

    for (var i = 0; i < Z12_QN.length; ++i) {
        index = Z12_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Z1 += 1;
        } else {
            Z2 += 1;
        }
        num++;
    }

    for (var i = 0; i < Z34_QN.length; ++i) {
        index = Z34_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Z3 += 1;
        } else {
            Z4 += 1;
        }
        num++;
    }

    for (var i = 0; i < Z56_QN.length; ++i) {
        index = Z56_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Z5 += 1;
        } else {
            Z6 += 1;
        }
        num++;
    }

    for (var i = 0; i < Z78_QN.length; ++i) {
        index = Z78_QN[i] - 1;
        var answer = answers[index];
        if (answer.code === 'A') {
            Z7 += 1;
        } else {
            Z8 += 1;
        }
        num++;
    }

    // 各维度的“总分”
    var J = X1 + Y1 + Z1;
    var P = X2 + Y2 + Z2;
    var S = X3 + Y3 + Z3;
    var N = X4 + Y4 + Z4;
    var E = X5 + Y5 + Z5;
    var I = X6 + Y6 + Z6;
    var F = X7 + Y7 + Z7;
    var T = X8 + Y8 + Z8;

    // 外倾/内倾
    var W1 = (I - E) / 21 * 10;
    // 感觉/直觉
    var W2 = (S - N) / 26 * 10;
    // 思考/情感
    var W3 = (T - F) / 24 * 10;
    // 知觉/判断
    var W4 = (P - J) / 22 * 10;

    print("W: " + W1 + "," + W2 + "," + W3 + "," + W4);

    var features = [
        W1 >= 0 ? 'I' : 'E',
        W2 >= 0 ? 'S' : 'N',
        W3 >= 0 ? 'T' : 'F',
        W4 >= 0 ? 'P' : 'J',
    ];
    
    var score = new ScaleScore();
    score.addItem('I', I);
    score.addItem('E', E);
    score.addItem('S', S);
    score.addItem('N', N);
    score.addItem('T', T);
    score.addItem('F', F);
    score.addItem('P', P);
    score.addItem('J', J);

    return {
        result: features.join(''),
        score: score
    }
}
