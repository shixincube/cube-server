// 采集抖音指定账号数据

if (!launchApp('抖音')) {
    console.log('没有安装抖音');
    exit();
}

// 等待应用打开
$.descContains('拍摄').waitFor();

var btn = $.desc('搜索，按钮').findOne(5000);
if (null == btn) {
    exit();
}

require('StopApp')('抖音');

sleep(1000);

launchApp('Roboengine');
