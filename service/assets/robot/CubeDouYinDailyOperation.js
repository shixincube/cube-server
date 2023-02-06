// 抖音账号日常操作

const parameter = require('CubeDouYinDailyOperationParameter');

// 持续时长，单位：秒
const duration = (undefined !== parameter.duration) ? parameter.duration : random(3 * 60, 17 * 60);

if (!launchApp('抖音')) {
    console.log('没有安装抖音');
    report.submit('CubeDouYinDailyOperation', 'Error', 'launchApp', {
        "desc": "没有安装抖音"
    });
    exit();
}

// 等待应用打开
$.descContains('拍摄').waitFor();

// 计算滑动位置
var cx = Math.floor(device.width * 0.5);
var cy = Math.floor(device.height * 0.5);
var y1 = cy + 400;
var y2 = cy - 400;

// 查找锚点计数
var anchorCount = 0;

var start = Date.now();
while ((Date.now() - start) < duration * 1000) {
    // 模拟观看视频停留时长
    var staying = random(1 * 1000, 11 * 1000);
    console.log('Video stay on ' + staying);
    sleep(staying);

    if (null != $.textContains('青少年模式').findOnce()) {
        // 青少年模式
        var button = $.textContains('我知道了').findOnce();
        if (null != button) {
            var location = button.bounds();
            click(location.centerX(), location.centerY());
            sleep(1000);
        }
    }
    else if (null != $.text('发现抖音朋友').findOnce()) {
        // 发现抖音朋友
        var button = $.text('拒绝').findOnce();
        if (null != button) {
            var location = button.bounds();
            click(location.centerX(), location.centerY());
            sleep(1000);
        }
    }
    else if (null != $.text('一键登录').findOnce()) {
        // 请求登录对话框
        var button = $.text('一键登录').findOnce();
        if (null != button) {
            var location = button.bounds();
            click(location.centerX(), location.centerY());
            sleep(1000);
        }
    }

    var anchor = $.id('user_avatar').findOnce();
    if (null == anchor) {
        ++anchorCount;
        if (anchorCount > 3) {
            console.log('程序不在前台');
            break;
        }
    }
    else {
        anchorCount = 0;
    }

    swipe(cx + random(-5, 5), y1, cx + random(-5, 5), y2, random(650, 700));
}

require('StopApp')('抖音');

sleep(1000);

back();
