// 采集抖音指定账号数据

const parameter = require('CubeDouYinAccountDataParameter');
const videoInfo = require('DouYinVideoInfo');

const word = parameter.word;
const maxNumVideo = (undefined === parameter.maxNumVideo) ? 10 : parameter.maxNumVideo;

if (undefined === word) {
    console.log('参数错误，没有设置 word 参数');
    report.submit('CubeDouYinAccountData', 'Error', 'Parameter', {
        "desc": "参数错误，没有设置 word 参数"
    });
    exit();
}

if (!launchApp('抖音')) {
    console.log('没有安装抖音');
    report.submit('CubeDouYinAccountData', 'Error', word, {
        "desc": "没有安装抖音"
    });
    exit();
}

// 等待应用打开
$.descContains('拍摄').waitFor();

var btn = $.desc('搜索，按钮').findOne(5000);
if (null == btn) {
    report.submit('CubeDouYinAccountData', 'Error', word, {
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
    report.submit('CubeDouYinAccountData', 'Error', word, {
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
    // 是否显示
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

        // 作品数
        var numWorksLayout = el.child(9);   // HorizontalScrollView
        var text = numWorksLayout.child(0).child(0).child(0).child(0).child(0).text();
        data.numWorks = parseInt(text.split(' ')[1]);

        // 计算滑动位置
        var cx = Math.floor(device.width * 0.5);
        var cy = Math.floor(device.height * 0.5);
        var y1 = cy + 400;
        var y2 = cy - 400;
        // 界面向上滑动以便完整显示第一个视频的视图
        swipe(cx, cy, cx, y2, random(600, 800));
        sleep(1000);

        // 浏览作品
        var firstVideo = $.id('container').findOnce();
        if (null != firstVideo) {
            location = firstVideo.bounds();
            var x = location.centerX();
            var y = location.centerY();
            if (x > 0 && y > 0) {
                // 打开视频
                click(x, y);

                sleep(1000);

                // 浏览视频数量
                var watchVideoCount = Math.min(maxNumVideo, data.numWorks);
                while (watchVideoCount > 0) {
                    sleep(4000);

                    var videoView = $.id('viewpager').findOne(3000);
                    if (null == videoView) {
                        break;
                    }

                    var video = videoInfo.getInfo();
                    var snapshot = report.submitScreenSnapshot('CubeDouYinAccountData', word);
                    if (null != snapshot) {
                        video.snapshot = snapshot;
                    }
                    data.works.push(video);

                    --watchVideoCount;
                    if (watchVideoCount <= 0) {
                        break;
                    }

                    swipe(cx + random(-5, 5), y1, cx + random(-5, 5), y2, random(600, 700));
                    sleep(1000);
                }
            }
        }
    }

    report.submit('CubeDouYinAccountData', 'Result', word, data);
}

require('StopApp')('抖音');

sleep(1000);

back();
//launchApp('Roboengine');
