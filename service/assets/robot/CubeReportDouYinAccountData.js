// 采集抖音指定账号数据

const parameter = require('CubeReportDouYinAccountDataParameter');
const videoInfo = require('DouYinVideoInfo');

const word = parameter.word;

if (undefined === word) {
    console.log('参数错误，没有设置 word 参数');
    report.submit('CubeReportDouYinAccountData', 'Error', 'Parameter', {
        "desc": "参数错误，没有设置 word 参数"
    });
    exit();
}

if (!launchApp('抖音')) {
    console.log('没有安装抖音');
    report.submit('CubeReportDouYinAccountData', 'Error', word, {
        "desc": "没有安装抖音"
    });
    exit();
}

// 等待应用打开
$.descContains('拍摄').waitFor();

var btn = $.desc('搜索，按钮').findOne(5000);
if (null == btn) {
    report.submit('CubeReportDouYinAccountData', 'Error', word, {
        "desc": "没有找到搜索按钮"
    });
    exit();
}

// 进入搜索界面
var location = btn.bounds();
click(location.centerX(), location.centerY());

var searchEdit = $.id('et_search_kw').untilFindOne();
// 设置搜索内容
searchEdit.setText(word);

// 清空搜索框按钮 desc:清空 id:btn_clear

var searchBtn = $.desc('搜索').findOnce();
location = searchBtn.bounds();
click(location.centerX(), location.centerY());

sleep(1000);

// 等待搜索结果
var count = 10;
var resultReady = false;
var userTabClicked = false;
var tabX = 0;
var tabY = 0;
while (count > 0) {
    var userTab = $.text('用户').findOnce();
    var followBtn = $.desc('关注按钮').findOnce();

    if (null != userTab && null != followBtn) {
        if (!userTabClicked) {
            userTabClicked = true;
            location = userTab.bounds();
            click(location.centerX(), location.centerY());

            tabX = location.centerX();
            tabY = location.centerY();

            sleep(1000);
        }

        // 搜索结果显示
        resultReady = true;
        break;
    }
    else if (null != userTab) {
        if (!userTabClicked) {
            userTabClicked = true;
            location = userTab.bounds();
            click(location.centerX(), location.centerY());

            tabX = location.centerX();
            tabY = location.centerY();
        }

        --count;
        sleep(1000);
    }
    else {
        --count;
        sleep(1000);
    }
}

if (!resultReady) {
    console.log('没有搜索结果');
    report.submit('CubeReportDouYinAccountData', 'Error', word, {
        "desc": "没有搜索结果或搜索超时"
    });
    exit();
}

// 等待列表页加载数据
var el = $.descContains(word).findOne(3000);
if (null != el) {
    location = el.bounds();
    var x = location.centerX();
    var y = location.centerY();
    if (x < 0 || y < 0) {
        x = tabX + 120;
        y = tabY + 160;
    }

    // 进入账号详情界面
    click(x, y);

    var data = {
        "word": word,
        "name": "",
        "verification": "",
        "likeCount": "",
        "followCount": "",
        "fansCount": "",
        "intro": "",
        "ipLocation": "",
        "numWorks": 0,  // 作品数
        "works": []
    };

    // 等待详情界面
    sleep(2000);
    el = $.descContains(word).findOne(5000);
    if (null != el) {
        // 按照层级关系定位
        el = $.className('android.view.ViewGroup').findOnce();  // 11
        //console.log('' + el.childCount());//11

        var nameLayout = el.child(3);
        data.name = nameLayout.child(0).text();
        if (nameLayout.childCount() > 1) {
            data.verification = nameLayout.child(1).child(1).text();
        }

        // 描述信息
        var descLayout = el.child(6);   // LinearLayout
        var descContentLayout = descLayout.child(0);    // RelativeLayout
        // 获赞
        var likeCountLayout = descContentLayout.child(0);   // RelativeLayout
        data.likeCount = likeCountLayout.child(0).text();   // TextView
        // 关注
        var followCountLayout = descContentLayout.child(1); // RelativeLayout
        data.followCount = followCountLayout.child(0).text();   // TextView
        // 粉丝
        var fansCountLayout = descContentLayout.child(2);   // RelativeLayout
        data.fansCount = fansCountLayout.child(0).text();   // TextView

        // 简介
        var introLayout = el.child(7);    // LinearLayout - 4
        data.intro = introLayout.child(2).child(0).text();
        if (introLayout.childCount() > 4) {
            // 获取 IP 属地
            data.ipLocation = introLayout.child(4).child(0).child(0).text();
        }


    }

    report.submit('CubeReportDouYinAccountData', 'Result', word, data);
}

require('StopApp')('抖音');

sleep(1000);

back();
//launchApp('Roboengine');
