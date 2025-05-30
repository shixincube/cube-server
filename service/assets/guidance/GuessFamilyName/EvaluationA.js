// Author: Ambrose Xu

var cardScoreMap = {
    "A1": 1,
    "A2": 2,
    "A3": 4,
    "A4": 8,
    "A5": 16,
    "A6": 32,
    "A7": 64,
    "B1": 1,
    "B2": 2,
    "B3": 4,
    "B4": 8,
    "B5": 16,
    "B6": 32,
    "B7": 64
};

var nameAScoreMap = {
    "0": "",
    "1": "赵",
    "2": "司马",
    "3": "钱",
    "4": "孔",
    "5": "孙",
    "6": "罗",
    "7": "李",
    "8": "上官",
    "9": "周",
    "10": "卢",
    "11": "吴",
    "12": "高",
    "13": "郑",
    "14": "顾",
    "15": "王",
    "16": "丁",
    "17": "潘",
    "18": "鲁",
    "19": "余",
    "20": "雷",
    "21": "刘",
    "22": "董",
    "23": "徐",
    "24": "梁",
    "25": "朱",
    "26": "叶",
    "27": "金",
    "28": "任",
    "29": "彭",
    "30": "唐",
    "31": "魏",
    "32": "欧阳",
    "33": "马",
    "34": "胡",
    "35": "林",
    "36": "孟",
    "37": "袁",
    "38": "钟",
    "39": "戴",
    "40": "龚",
    "41": "肖",
    "42": "黄",
    "43": "夏",
    "44": "苏",
    "45": "许",
    "46": "方",
    "47": "何",
    "48": "苗",
    "49": "张",
    "50": "万",
    "51": "曹",
    "52": "毛",
    "53": "严",
    "54": "付",
    "55": "范",
    "56": "田",
    "57": "向",
    "58": "杜",
    "59": "汤",
    "60": "段",
    "61": "姚",
    "62": "宋",
    "63": "蔡",
    "64": "涂",
    "65": "阮",
    "66": "伍",
    "67": "代",
    "68": "覃",
    "69": "成",
    "70": "邹",
    "71": "邓",
    "72": "吕",
    "73": "程",
    "74": "汪",
    "75": "柳",
    "76": "石",
    "77": "卜",
    "78": "郝",
    "79": "易",
    "80": "陶",
    "81": "童",
    "82": "曾",
    "83": "欧",
    "84": "喻",
    "85": "郭",
    "86": "施",
    "87": "史",
    "88": "贾",
    "89": "关",
    "90": "侯",
    "91": "薛",
    "92": "熊",
    "93": "江",
    "94": "盛",
    "95": "白",
    "96": "赖",
    "97": "贺",
    "98": "舒",
    "99": "谢",
    "100": "姜",
    "101": "申",
    "102": "华",
    "103": "尹",
    "104": "邱",
    "105": "殷",
    "106": "常",
    "107": "柯",
    "108": "牛",
    "109": "莫",
    "110": "祁",
    "111": "聂",
    "112": "康",
    "113": "倪",
    "114": "梅",
    "115": "水",
    "116": "艾",
    "117": "岳",
    "118": "廖",
    "119": "翟",
    "120": "邬",
    "121": "季",
    "122": "花",
    "123": "左",
    "124": "桂",
    "125": "龙",
    "126": "陆",
};

function main(args) {
    var evaluationResult = new EvaluationResult('我猜您的姓氏是：');

    var hitSnList = [];

    for (var i = 0; i < args.length; ++i) {
        var question = args[i];
        var answer = question.getAnswer();
        if (null != answer) {
            if (answer.code === 'true') {
                hitSnList.push(question.sn);
            }
        }
    }

    var total = 0;
    for (var i = 0; i < hitSnList.length; ++i) {
        var score = cardScoreMap[hitSnList[i]];
        total += score;
    }

    if (0 === total) {
        // 没有得分
        return evaluationResult;
    }
    else if (127 === total) {
        // 得分越界
        evaluationResult.setTerminated(true);
        return evaluationResult;
    }

    var name = nameAScoreMap[total.toString()];
    if (name.length > 0) {
        evaluationResult.setTerminated(true);
        evaluationResult.setResult(name);
    }
    return evaluationResult;
}
