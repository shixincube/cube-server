// Author: Ambrose Xu
// Date: 2024-05-21

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

function explain(score) {
    if (score <= 10) {
        return FactorLevel.Mild;
    } else {
        return FactorLevel.Severe;
    }
}

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

    var feature = features.join('');
    var name = '';
    var description = '';
    if (feature === 'ISTJ') {
        name = '物流师';
        description = '安静、严肃，通过全面性和可靠性获得成功。实际，有责任感。决定有逻辑性，并一步步地朝着目标前进，不易分心。喜欢将工作、家庭和生活都安排得井井有条。重视传统和忠诚。';
    } else if (feature === 'ISFJ') {
        name = '守卫者';
        description = '安静、友好、有责任感和良知。坚定地致力于完成他们的义务。全面、勤勉、精确，忠诚、体贴，留心和记得他们重视的人的小细节，关心他们的感受。努力把工作和家庭环境营造得有序而温馨。';
    } else if (feature === 'INFJ') {
        name = '提倡者';
        description = '寻求思想、关系、物质等之间的意义和联系。希望了解什么能够激励人，对人有很强的洞察力。有责任心，坚持自己的价值观。对于怎样更好的服务大众有清晰的远景。在对于目标的实现过程中有计划而且果断坚定。';
    } else if (feature === 'INTJ') {
        name = '建筑师';
        description = '在实现自己的想法和达成自己的目标时有创新的想法和非凡的动力。能很快洞察到外界事物间的规律并形成长期的远景计划。一旦决定做一件事就会开始规划并直到完成为止。多疑、独立，对于自己和他人能力和表现的要求都非常高。';
    } else if (feature === 'ISTP') {
        name = '鉴赏家';
        description = '灵活、忍耐力强，是个安静的观察者直到有问题发生，就会马上行动，找到实用的解决方法。分析事物运作的原理，能从大量的信息中很快的找到关键的症结所在。对于原因和结果感兴趣，用逻辑的方式处理问题，重视效率。';
    } else if (feature === 'ISFP') {
        name = '探险家';
        description = '安静、友好、敏感、和善。享受当前。喜欢有自己的空间，喜欢能按照自己的时间表工作。对于自己的价值观和自己觉得重要的人非常忠诚，有责任心。不喜欢争论和冲突。不会将自己的观念和价值观强加到别人身上。';
    } else if (feature === 'INFP') {
        name = '调停者';
        description = '理想主义，对于自己的价值观和自己觉得重要的人非常忠诚。希望外部的生活和自己内心的价值观是统一的。好奇心重，很快能看到事情的可能性，能成为实现想法的催化剂。寻求理解别人和帮助他们实现潜能。适应力强，灵活，善于接受，除非是有悖于自己的价值观的。';
    } else if (feature === 'INTP') {
        name = '逻辑学家';
        description = '对于自己感兴趣的任何事物都寻求找到合理的解释。喜欢理论性的和抽象的事物，热衷于思考而非社交活动。安静、内向、灵活、适应力强。对于自己感兴趣的领域有超凡的集中精力深度解决问题的能力。多疑，有时会有点挑剔，喜欢分析。';
    } else if (feature === 'ESTP') {
        name = '企业家';
        description = '灵活、忍耐力强，实际，注重结果。觉得理论和抽象的解释非常无趣。喜欢积极地采取行动解决问题。注重当前，自然不做作，享受和他人在一起的时刻。喜欢物质享受和时尚。学习新事物最有效的方式是通过亲身感受和练习。';
    } else if (feature === 'ESFP') {
        name = '表演者';
        description = '外向、友好、接受力强。热爱生活、人类和物质上的享受。喜欢和别人一起将事情做成功。在工作中讲究常识和实用性，并使工作显得有趣。灵活、自然不做作，对于新的任何事物都能很快地适应。学习新事物最有效的方式是和他人一起尝试。';
    } else if (feature === 'ENFP') {
        name = '竞选者';
        description = '热情洋溢、富有想象力。认为人生有很多的可能性。能很快地将事情和信息联系起来，然后很自信地根据自己的判断解决问题。总是需要得到别人的认可，也总是准备着给与他人赏识和帮助。灵活、自然不做作，有很强的即兴发挥的能力，言语流畅。';
    } else if (feature === 'ENTP') {
        name = '辩论家';
        description = '反应快、睿智，有激励别人的能力，警觉性强、直言不讳。在解决新的、具有挑战性的问题时机智而有策略。善于找出理论上的可能性，然后再用战略的眼光分析。善于理解别人。不喜欢例行公事，很少会用相同的方法做相同的事情，倾向于一个接一个的发展新的爱好。';
    } else if (feature === 'ESTJ') {
        name = '总经理';
        description = '实际、现实主义。果断，一旦下决心就会马上行动。善于将项目和人组织起来将事情完成，并尽可能用最有效率的方法得到结果。注重日常的细节。有一套非常清晰的逻辑标准，有系统性地遵循，并希望他人也同样遵循。在实施计划时强而有力。';
    } else if (feature === 'ESFJ') {
        name = '执政官';
        description = '热心肠、有责任心、合作。希望周边的环境温馨而和谐，并为此果断地执行。喜欢和他人一起精确并及时地完成任务。事无巨细都会保持忠诚。能体察到他人在日常生活中的所需并竭尽全力帮助。希望自己和自己的所为能受到他人的认可和赏识。';
    } else if (feature === 'ENFJ') {
        name = '主人公';
        description = '热情、为他人着想、易感应、有责任心。非常注重他人的感情、需求和动机。善于发现他人的潜能，并希望能帮助他们实现。能成为个人或群体成长和进步的催化剂。忠诚，对于赞扬和批评都会积极地回应。友善、好社交。在团体中能很好地帮助他人，并有鼓舞他人的领导能力。';
    } else if (feature === 'ENTJ') {
        name = '指挥官';
        description = '坦诚、果断，有天生的领导能力。能很快看到公司/组织程序和政策中的不合理性和低效能性，发展并实施有效和全面的系统来解决问题。善于做长期的计划和目标的设定。通常见多识广，博览群书，喜欢拓广自己的知识面并将此分享给他人。在陈述自己的想法时非常强而有力。';
    }

    var content = ['**性格类型** ：', name, '（', feature, '）', '\n\n',
        '**性格描述** ：', description, '\n\n' ];

    var score = new ScaleScore();
    score.addItem('I', '内倾', I, explain(I));
    score.addItem('E', '外倾', E, explain(E));
    score.addItem('S', '感觉', S, explain(S));
    score.addItem('N', '直觉', N, explain(N));
    score.addItem('T', '思考', T, explain(T));
    score.addItem('F', '情感', F, explain(F));
    score.addItem('P', '知觉', P, explain(P));
    score.addItem('J', '判断', J, explain(J));

    return {
        content: content.join(''),
        score: score
    }
}
